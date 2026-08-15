package ru.navigatordosuga.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import ru.navigatordosuga.app.data.db.*
import ru.navigatordosuga.app.model.*
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class ContentRepository(private val db:AppDatabase) {
    fun items(mode:ActivityMode): Flow<List<GeoItem>> = when(mode) {
        ActivityMode.MUSHROOMS -> db.contentDao().mushrooms().map { it.map(MushroomEntity::domain) }
        ActivityMode.FISHING -> db.contentDao().fishing().map { it.map(FishingEntity::domain) }
        ActivityMode.BEAUTIFUL -> db.contentDao().beautiful().map { it.map(BeautifulEntity::domain) }
        ActivityMode.CINEMA -> db.contentDao().cinema().map { it.map(CinemaEntity::domain) }
        ActivityMode.HISTORY -> db.contentDao().history().map { it.map(HistoryEntity::domain) }
        ActivityMode.EVENTS -> flowOf(emptyList())
    }
}

class EventRepository(private val db:AppDatabase, private val json:Json) {
    fun events(filter:EventFilter): Flow<List<EventItem>> {
        val from="${filter.dateFrom}T00:00:00+03:00"
        val to="${filter.dateTo}T23:59:59+03:00"
        return db.eventDao().visible(from,to).map { rows ->
            rows.asSequence().map { it.domain(json) }
                .filter { filter.price=="all" || (filter.price=="free"&&it.isFree) || (filter.price=="paid"&&it.priceType=="PAID") }
                .filter { filter.category.isBlank() || it.category==filter.category }
                .filter { filter.query.isBlank() || (it.title+" "+it.venueName+" "+it.participants.joinToString(" ")).contains(filter.query,true) }
                .toList()
        }
    }
    fun defaultFilter():EventFilter { val d=LocalDate.now(ZoneId.of("Europe/Moscow")); return EventFilter(d.toString(),d.toString()) }
}

class ProfileRepository(private val dao:ProfileDao, private val prefs:DataStore<Preferences>) {
    private val activeKey=stringPreferencesKey("active_profile_id")
    val activeId:Flow<String?> = prefs.data.map { it[activeKey] }
    val profiles=dao.all()
    suspend fun create(name:String,avatarUri:String?,interests:Set<String>,transport:String,maxDistance:Int):Profile {
        val p=Profile("p_${UUID.randomUUID()}",name.trim().ifBlank{"Путешественник"},avatarUri=avatarUri,interests=interests,transportPreference=transport,maxTripDistanceKm=maxDistance)
        dao.upsert(p.entity()); prefs.edit{it[activeKey]=p.id}; return p
    }
    suspend fun setActive(id:String){ prefs.edit{it[activeKey]=id} }
    suspend fun active():Profile?=activeId.first()?.let{dao.get(it)?.domain()}
}
private fun Profile.entity()=ProfileEntity(id,displayName,avatarUri,interests.joinToString(","),transportPreference,maxTripDistanceKm,defaultActivity.wire,theme,System.currentTimeMillis())
private fun ProfileEntity.domain()=Profile(id,displayName,avatarUri,interestsCsv.split(',').filter{it.isNotBlank()}.toSet(),transportPreference,maxTripDistanceKm,ActivityMode.fromWire(defaultActivity),theme)

class SavedRepository(private val dao:SavedDao){
    fun all(profileId:String)=dao.all(profileId).map { rows -> rows.map { StoredItem(it.dataset,it.itemId,snapshotTitle(it.snapshotJson),it.savedAt) } }
    suspend fun toggle(profileId:String,dataset:String,itemId:String,snapshotJson:String):Boolean{val ex=dao.exists(profileId,dataset,itemId);if(ex)dao.delete(profileId,dataset,itemId)else dao.upsert(SavedItemEntity(profileId,dataset,itemId,snapshotJson,System.currentTimeMillis()));return !ex}
    suspend fun remove(profileId:String,dataset:String,itemId:String)=dao.delete(profileId,dataset,itemId)
}
class TripRepository(private val dao:TripDao){
    fun all(profileId:String)=dao.all(profileId).map { rows -> rows.map { StoredItem(it.dataset,it.itemId,snapshotTitle(it.snapshotJson),it.addedAt,it.position) } }
    suspend fun add(profileId:String,dataset:String,itemId:String,snapshot:String,position:Int)=dao.upsert(TripItemEntity(profileId,itemId,dataset,snapshot,position,System.currentTimeMillis()))
    suspend fun remove(profileId:String,itemId:String)=dao.delete(profileId,itemId)
    suspend fun clear(profileId:String)=dao.clear(profileId)
}

class SearchRepository(private val db:AppDatabase,private val json:Json){
    suspend fun search(raw:String,limitPerMode:Int=12):List<SearchHit>{
        val q=raw.trim(); if(q.length<2)return emptyList()
        val c=db.contentDao()
        val out=mutableListOf<SearchHit>()
        out += c.searchMushrooms(q,limitPerMode).map{it.domain().hit()}
        out += c.searchFishing(q,limitPerMode).map{it.domain().hit()}
        out += c.searchBeautiful(q,limitPerMode).map{it.domain().hit()}
        out += c.searchCinema(q,limitPerMode).map{it.domain().hit()}
        out += c.searchHistory(q,limitPerMode).map{it.domain().hit()}
        out += db.eventDao().search(q,limitPerMode).map{ e -> val d=e.domain(json); SearchHit(d.id,ActivityMode.EVENTS,d.title,d.venueName,d.lat,d.lon,d.startDateTime.take(10)) }
        return out.sortedWith(compareBy<SearchHit>{it.mode.ordinal}.thenBy{it.title}).take(60)
    }
}

private fun GeoItem.hit()=SearchHit(id,mode,name,summary.ifBlank{subCategory},lat,lon)
private fun snapshotTitle(raw:String):String {
    val key="\"name\":\""
    val start=raw.indexOf(key)
    if(start<0)return "Сохранённый объект"
    val from=start+key.length
    val end=raw.indexOf('"',from)
    return if(end>from) raw.substring(from,end).replace("\\\"","\"").replace("\\\\","\\") else "Сохранённый объект"
}
