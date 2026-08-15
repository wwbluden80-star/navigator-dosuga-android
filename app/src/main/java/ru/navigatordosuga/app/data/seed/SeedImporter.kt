package ru.navigatordosuga.app.data.seed

import android.content.Context
import androidx.room.withTransaction
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import ru.navigatordosuga.app.data.db.*

@Serializable data class SeedGeoDataset(val schemaVersion:Int=1,val dataset:String="",val generatedAt:String?=null,val items:List<SeedGeoItem> = emptyList())
@Serializable data class SeedGeoItem(val id:String,val name:String,val lat:Double?=null,val lon:Double?=null,val region:String="",val category:String="",val subCategory:String="",@Serializable(with=FlexibleDoubleSerializer::class) val score:Double=0.0,@Serializable(with=FlexibleDoubleSerializer::class) val secondaryScore:Double=0.0,val confidence:String="",val summary:String="",val iconKey:String="",val updatedAt:String="",val payload:JsonElement)
@Serializable data class SeedEventDataset(val schemaVersion:Int=1,val dataset:String="events",val generatedAt:String?=null,val items:List<SeedEvent> = emptyList())
@Serializable data class SeedEvent(val id:String,val title:String,val subtitle:String="",val description:String="",val category:String="other",val startDateTime:String,val endDateTime:String?=null,val status:String="UNKNOWN",val venueName:String="",val address:String="",val lat:Double,val lon:Double,val priceType:String="UNKNOWN",val priceMin:Double?=null,val priceMax:Double?=null,val registrationRequired:Boolean=false,val ticketUrl:String?=null,val registrationUrl:String?=null,val ageRating:String?=null,val artists:List<String> = emptyList(),val bloggers:List<String> = emptyList(),val performers:List<String> = emptyList(),val sourceConfidence:String="",val updatedAt:String="",val payload:JsonElement)

/** The web/seed contract contains numeric scores as well as evidence grades such as "B-/C+". */
object FlexibleDoubleSerializer:KSerializer<Double>{
    override val descriptor:SerialDescriptor=PrimitiveSerialDescriptor("FlexibleDouble",PrimitiveKind.DOUBLE)
    override fun deserialize(decoder:Decoder):Double{
        if(decoder !is JsonDecoder)return decoder.decodeDouble()
        val value=decoder.decodeJsonElement()
        return (value as? JsonPrimitive)?.doubleOrNull ?: 0.0
    }
    override fun serialize(encoder:Encoder,value:Double)=encoder.encodeDouble(value)
}

class SeedImporter(private val context: Context, private val db: AppDatabase, private val json: Json) {
    suspend fun ensureSeeded() {
        val c=db.contentDao()
        if (c.mushroomCount()+c.fishingCount()+c.beautifulCount()+c.cinemaCount()+c.historyCount()>0) return
        db.withTransaction {
            importGeo("mushrooms.json") { rows -> c.upsertMushrooms(rows.map { it.mushroom() }) }
            importGeo("fishing.json") { rows -> c.upsertFishing(rows.map { it.fishing() }) }
            importGeo("beautiful.json") { rows -> c.upsertBeautiful(rows.map { it.beautiful() }) }
            importGeo("cinema.json") { rows -> c.upsertCinema(rows.map { it.cinema() }) }
            importGeo("history.json") { rows -> c.upsertHistory(rows.map { it.history() }) }
            val events = readAsset<SeedEventDataset>("events.json")
            db.eventDao().upsert(events.items.map { it.entity(json) })
            val now=System.currentTimeMillis()
            listOf("mushrooms","fishing","beautiful","cinema","history","events").forEach { key ->
                db.syncDao().upsert(SyncStateEntity(key,lastSuccessAt=now,lastAttemptAt=now,version="seed-v1",status="seed"))
            }
        }
    }
    private suspend fun importGeo(file:String, block:suspend (List<SeedGeoItem>)->Unit) = block(readAsset<SeedGeoDataset>(file).items)
    private inline fun <reified T> readAsset(name:String): T = context.assets.open("seed/$name").bufferedReader().use { json.decodeFromString(it.readText()) }
}

private fun SeedGeoItem.payload(j:Json)=payload.toString()
private fun SeedGeoItem.mushroom()=MushroomEntity(id,name,lat,lon,region,category,subCategory,score,secondaryScore,confidence,summary,iconKey,updatedAt,payload(Json.Default))
private fun SeedGeoItem.fishing()=FishingEntity(id,name,lat,lon,region,category,subCategory,score,secondaryScore,confidence,summary,iconKey,updatedAt,payload(Json.Default))
private fun SeedGeoItem.beautiful()=BeautifulEntity(id,name,lat,lon,region,category,subCategory,score,secondaryScore,confidence,summary,iconKey,updatedAt,payload(Json.Default))
private fun SeedGeoItem.cinema()=CinemaEntity(id,name,lat,lon,region,category,subCategory,score,secondaryScore,confidence,summary,iconKey,updatedAt,payload(Json.Default))
private fun SeedGeoItem.history()=HistoryEntity(id,name,lat,lon,region,category,subCategory,score,secondaryScore,confidence,summary,iconKey,updatedAt,payload(Json.Default))
private fun SeedEvent.entity(j:Json)=EventEntity(id,title,subtitle,description,category,startDateTime,endDateTime,status,venueName,address,lat,lon,priceType,priceMin,priceMax,registrationRequired,ticketUrl,registrationUrl,ageRating,kotlinx.serialization.json.JsonArray((artists+bloggers+performers).distinct().map{kotlinx.serialization.json.JsonPrimitive(it)}).toString(),sourceConfidence,updatedAt,payload.toString())
