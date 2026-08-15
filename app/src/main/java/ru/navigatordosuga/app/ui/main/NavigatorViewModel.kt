package ru.navigatordosuga.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.navigatordosuga.app.AppContainer
import ru.navigatordosuga.app.data.sync.SyncScheduler
import ru.navigatordosuga.app.model.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.*

@OptIn(ExperimentalCoroutinesApi::class)
class NavigatorViewModel(private val c:AppContainer):ViewModel(){
    private val _mode=MutableStateFlow(ActivityMode.MUSHROOMS); val mode=_mode.asStateFlow()
    private val _camera=MutableStateFlow(MapCameraState()); val camera=_camera.asStateFlow()
    private val _selectedId=MutableStateFlow<String?>(null); val selectedId=_selectedId.asStateFlow()
    private val _bottom=MutableStateFlow(BottomSection.MAP); val bottom=_bottom.asStateFlow()
    private val _games=MutableStateFlow(false); val games=_games.asStateFlow()
    private val _profileSetup=MutableStateFlow(false); val profileSetup=_profileSetup.asStateFlow()
    private val _profileManager=MutableStateFlow(false); val profileManager=_profileManager.asStateFlow()
    private val _offlineMaps=MutableStateFlow(false); val offlineMaps=_offlineMaps.asStateFlow()
    private val _guides=MutableStateFlow(false); val guides=_guides.asStateFlow()
    private val _eventFilter=MutableStateFlow(c.events.defaultFilter()); val eventFilter=_eventFilter.asStateFlow()
    private val _geoFilter=MutableStateFlow(GeoFilter()); val geoFilter=_geoFilter.asStateFlow()
    private val _userLocation=MutableStateFlow<UserLocationState?>(null); val userLocation=_userLocation.asStateFlow()
    private val _locationFollowing=MutableStateFlow(false); val locationFollowing=_locationFollowing.asStateFlow()
    private var locationJob:Job?=null
    private val _query=MutableStateFlow(""); val query=_query.asStateFlow()
    private val _activeTrackId=MutableStateFlow<String?>(null); val activeTrackId=_activeTrackId.asStateFlow()
    private val _liveGlass=MutableStateFlow(false); val liveGlass=_liveGlass.asStateFlow()
    private val _theme=MutableStateFlow("system"); val theme=_theme.asStateFlow()

    val profiles=c.profiles.profiles.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
    val activeProfileId=c.profiles.activeId.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),null)
    private val baseItems=mode.flatMapLatest { if(it==ActivityMode.EVENTS) flowOf(emptyList()) else c.content.items(it) }
    val items=combine(baseItems,_query,_geoFilter,_camera,_userLocation){rows,q,filter,camera,user->
        val centerLat=user?.lat?:camera.lat;val centerLon=user?.lon?:camera.lon
        rows.distinctCanonical().asSequence()
            .filter{q.isBlank()||(it.name+" "+it.summary+" "+it.category+" "+it.subCategory).contains(q,true)}
            .filter{it.score>=filter.minScore}
            .filter{it.lat==null||it.lon==null||distanceKm(centerLat,centerLon,it.lat,it.lon)<=filter.maxDistanceKm}
            .toList()
    }
        .stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
    val events=_eventFilter.flatMapLatest(c.events::events).stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
    val searchResults=_query.debounce(180).mapLatest { q -> if(q.trim().length<2) emptyList() else c.search.search(q) }.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
    val savedItems=activeProfileId.flatMapLatest { c.saved.all(it?:"guest") }.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
    val tripItems=activeProfileId.flatMapLatest { c.trips.all(it?:"guest") }.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
    val carMarker=activeProfileId.flatMapLatest { c.car.observe(it?:"guest") }.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),null)
    val syncStates=c.db.syncDao().all().stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())

    init {
        viewModelScope.launch { c.appState.state.first().let{_mode.value=it.mode;_camera.value=it.camera;_geoFilter.value=it.geoFilter;_liveGlass.value=it.liveGlass;_theme.value=it.theme}; if(c.profiles.active()==null && c.db.profileDao().all().first().isEmpty()) _profileSetup.value=true }
    }
    fun mode(v:ActivityMode){_mode.value=v;_selectedId.value=null;_bottom.value=BottomSection.MAP;viewModelScope.launch{c.appState.saveMode(v)}}
    fun camera(v:MapCameraState){_camera.value=v;viewModelScope.launch{c.appState.saveCamera(v)}}
    fun select(id:String?){_selectedId.value=id}
    fun bottom(v:BottomSection){_bottom.value=v}
    fun games(show:Boolean){_games.value=show}
    fun profileManager(show:Boolean){_profileManager.value=show}
    fun setActiveProfile(id:String){viewModelScope.launch{c.profiles.setActive(id);_profileManager.value=false}}
    fun newProfile(){_profileManager.value=false;_profileSetup.value=true}
    fun offlineMaps(show:Boolean){_offlineMaps.value=show}
    fun guides(show:Boolean){_guides.value=show}
    fun query(v:String){_query.value=v; if(_mode.value==ActivityMode.EVENTS)_eventFilter.update{it.copy(query=v)} }
    fun eventPrice(v:String){_eventFilter.update{it.copy(price=v)}}
    fun eventCategory(v:String){_eventFilter.update{it.copy(category=v)}}
    fun geoMinScore(v:Float){_geoFilter.update{it.copy(minScore=v.coerceIn(0f,100f))};viewModelScope.launch{c.appState.saveGeoFilter(_geoFilter.value)}}
    fun geoMaxDistance(v:Float){_geoFilter.update{it.copy(maxDistanceKm=v.coerceIn(10f,500f))};viewModelScope.launch{c.appState.saveGeoFilter(_geoFilter.value)}}
    fun eventRange(kind:String){
        val z=ZoneId.of("Europe/Moscow");val today=LocalDate.now(z)
        val range=when(kind){
            "tomorrow"->today.plusDays(1) to today.plusDays(1)
            "weekend"->{val sat=generateSequence(today){it.plusDays(1)}.first{it.dayOfWeek==DayOfWeek.SATURDAY};sat to sat.plusDays(1)}
            "7d"->today to today.plusDays(6)
            else->today to today
        }
        _eventFilter.update{it.copy(dateFrom=range.first.toString(),dateTo=range.second.toString())}
    }
    fun refresh(){SyncScheduler.refreshNow(c.appContext)}
    fun toggleLiveGlass(){_liveGlass.value=!_liveGlass.value;viewModelScope.launch{c.appState.saveLiveGlass(_liveGlass.value)}}
    fun cycleTheme(){_theme.value=when(_theme.value){"system"->"light";"light"->"dark";else->"system"};viewModelScope.launch{c.appState.saveTheme(_theme.value)}}
    fun toggleSaved(dataset:String,itemId:String,snapshot:String){viewModelScope.launch{val pid=c.profiles.active()?.id?:"guest";c.saved.toggle(pid,dataset,itemId,snapshot)}}
    fun addTrip(dataset:String,itemId:String,snapshot:String){viewModelScope.launch{val pid=c.profiles.active()?.id?:"guest";val pos=c.db.tripDao().all(pid).first().size;c.trips.add(pid,dataset,itemId,snapshot,pos)}}
    fun removeSaved(x:StoredItem){viewModelScope.launch{val pid=c.profiles.active()?.id?:"guest";c.saved.remove(pid,x.dataset,x.itemId)}}
    fun removeTrip(x:StoredItem){viewModelScope.launch{val pid=c.profiles.active()?.id?:"guest";c.trips.remove(pid,x.itemId)}}
    fun clearTrip(){viewModelScope.launch{val pid=c.profiles.active()?.id?:"guest";c.trips.clear(pid)}}
    fun rememberCar(){viewModelScope.launch{val pid=c.profiles.active()?.id?:"guest";c.location.currentLocation(true)?.let{c.car.save(pid,it.latitude,it.longitude)}}}
    fun clearCar(){viewModelScope.launch{val pid=c.profiles.active()?.id?:"guest";c.car.clear(pid)}}
    fun startTrack(){viewModelScope.launch{val pid=c.profiles.active()?.id?:"guest";if(_activeTrackId.value==null)_activeTrackId.value=c.tracks.start(pid)}}
    fun stopTrack(){c.tracks.stop();_activeTrackId.value=null}
    fun openSearchHit(hit:SearchHit){
        _mode.value=hit.mode;_selectedId.value=hit.id;_bottom.value=BottomSection.MAP;_query.value=""
        if(hit.mode==ActivityMode.EVENTS && hit.eventDate!=null)_eventFilter.update{it.copy(dateFrom=hit.eventDate,dateTo=hit.eventDate,query="")}
        if(hit.lat!=null&&hit.lon!=null)_camera.value=_camera.value.copy(lat=hit.lat,lon=hit.lon,zoom=maxOf(_camera.value.zoom,13.5))
        viewModelScope.launch{c.appState.saveMode(hit.mode);c.appState.saveCamera(_camera.value)}
    }
    fun createProfile(name:String,avatarUri:String?,interests:Set<String>,transport:String,maxDistance:Int){viewModelScope.launch{c.profiles.create(name,avatarUri,interests,transport,maxDistance);_profileSetup.value=false}}
    fun closeProfileSetup(){_profileSetup.value=false}
    fun enableLocation(){
        if(locationJob!=null){_locationFollowing.value=!_locationFollowing.value;if(_locationFollowing.value)_userLocation.value?.let{_camera.value=_camera.value.copy(lat=it.lat,lon=it.lon,zoom=maxOf(_camera.value.zoom,14.5))};return}
        _locationFollowing.value=true
        locationJob=viewModelScope.launch{
            c.location.currentLocation(true)?.let{loc->updateLocation(loc.latitude,loc.longitude,loc.accuracy,loc.bearing.takeIf{loc.hasBearing()})}
            c.location.updates().collect{loc->updateLocation(loc.latitude,loc.longitude,loc.accuracy,loc.bearing.takeIf{loc.hasBearing()})}
        }
    }
    private fun updateLocation(lat:Double,lon:Double,accuracy:Float,heading:Float?){
        _userLocation.value=UserLocationState(lat,lon,accuracy,heading)
        if(_locationFollowing.value)_camera.value=_camera.value.copy(lat=lat,lon=lon,zoom=maxOf(_camera.value.zoom,14.5))
    }
}

private fun List<GeoItem>.distinctCanonical():List<GeoItem>{
    val ids=HashSet<String>();val spatial=HashSet<String>()
    return sortedByDescending{it.score}.filter{x->
        if(!ids.add(x.id))false else if(x.lat==null||x.lon==null)true else {
            val title=x.name.lowercase().replace(Regex("[^а-яa-z0-9]+"),"").take(28)
            spatial.add("${(x.lat*10000).roundToInt()}:${(x.lon*10000).roundToInt()}:$title")
        }
    }
}

private fun distanceKm(aLat:Double,aLon:Double,bLat:Double,bLon:Double):Double{
    val r=6371.0088;val dLat=Math.toRadians(bLat-aLat);val dLon=Math.toRadians(bLon-aLon)
    val s=sin(dLat/2).pow(2)+cos(Math.toRadians(aLat))*cos(Math.toRadians(bLat))*sin(dLon/2).pow(2)
    return 2*r*asin(sqrt(s.coerceIn(0.0,1.0)))
}

enum class BottomSection{MAP,TOP,TRIP,SAVED}
