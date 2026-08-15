package ru.navigatordosuga.app.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.navigatordosuga.app.model.ActivityMode
import ru.navigatordosuga.app.model.MapCameraState

data class PersistedUiState(val mode:ActivityMode=ActivityMode.MUSHROOMS,val camera:MapCameraState=MapCameraState(),val liveGlass:Boolean=false)
class AppStateStore(private val prefs:DataStore<Preferences>){
    private val mode=stringPreferencesKey("ui_mode");private val lat=doublePreferencesKey("map_lat");private val lon=doublePreferencesKey("map_lon");private val zoom=doublePreferencesKey("map_zoom");private val bearing=doublePreferencesKey("map_bearing");private val tilt=doublePreferencesKey("map_tilt");private val liveGlass=booleanPreferencesKey("live_glass")
    val state:Flow<PersistedUiState> = prefs.data.map{p->PersistedUiState(ActivityMode.fromWire(p[mode]),MapCameraState(p[lat]?:55.751244,p[lon]?:37.618423,p[zoom]?:9.5,p[bearing]?:0.0,p[tilt]?:0.0),p[liveGlass]?:false)}
    suspend fun saveMode(v:ActivityMode){prefs.edit{it[mode]=v.wire}}
    suspend fun saveCamera(v:MapCameraState){prefs.edit{it[lat]=v.lat;it[lon]=v.lon;it[zoom]=v.zoom;it[bearing]=v.bearing;it[tilt]=v.tilt}}
    suspend fun saveLiveGlass(v:Boolean){prefs.edit{it[liveGlass]=v}}
}
