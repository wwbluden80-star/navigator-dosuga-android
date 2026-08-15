package ru.navigatordosuga.app.data.network

import kotlinx.serialization.json.*
import ru.navigatordosuga.app.data.db.*
import ru.navigatordosuga.app.data.seed.SeedEventDataset
import ru.navigatordosuga.app.data.seed.SeedGeoDataset
import ru.navigatordosuga.app.data.seed.SeedGeoItem

class WebNormalizers(private val json:Json) {
    fun normalizedGeo(body:String): SeedGeoDataset? = runCatching { json.decodeFromString<SeedGeoDataset>(body) }.getOrNull()
    fun rawGeo(dataset:String,body:String): SeedGeoDataset {
        val root=json.parseToJsonElement(body).jsonObject
        val generated=root.str("generatedAt") ?: root.str("generated_at") ?: root.str("updated_at") ?: root["meta"]?.jsonObject?.str("generatedAt")
        val rows=(root["places"] as? JsonArray).orEmpty().mapNotNull { e ->
            val o=e.jsonObject; val lat=o.d("lat") ?: return@mapNotNull null; val lon=o.d("lon") ?: return@mapNotNull null
            when(dataset){
                "mushrooms" -> SeedGeoItem(o.str("id")?:stable(dataset,o,lat,lon),o.str("name")?:"",lat,lon,o.str("area")?:o.str("region")?:"","mushroom",o.str("dominant_mushroom")?:"",o.d("base_mps")?:0.0,o.d("base_effective")?:0.0,o.str("confidence")?:"",o.str("signal")?:o.str("weather_note")?:"",o.str("dominant_mushroom")?:"Грибы",generated?:"",o)
                "fishing" -> { val species=o["species"]?.jsonArray?.mapNotNull{it.jsonPrimitive.contentOrNull}.orEmpty(); SeedGeoItem(o.str("id")?:stable(dataset,o,lat,lon),o.str("name")?:"",lat,lon,"","fishing",species.firstOrNull()?:o.str("waterbodyType")?:"",o.d("fpsBeta")?:0.0,o.d("confidence")?:0.0,o.str("geoConfidence")?:o.str("confidence")?:"",o.str("bestTime")?:"",species.firstOrNull()?:"рыба",generated?:"",o) }
                "beautiful" -> SeedGeoItem(o.str("id")?:stable(dataset,o,lat,lon),o.str("name")?:"",lat,lon,o.str("region")?:"",o.str("category")?:"beautiful",o.str("subCategory")?:"",o.d("bps")?:0.0,o.d("wowScore")?:0.0,o.str("geoConfidence")?:"",o.str("researchNotes")?:"",o.str("iconType")?:o.str("subCategory")?:"nature",o.str("updatedAt")?:generated?:"",o)
                "cinema" -> SeedGeoItem(o.str("id")?:stable(dataset,o,lat,lon),o.str("name")?:"",lat,lon,o.str("area")?:"","cinema",o.str("locationType")?:"",o.d("cinemaScore")?:0.0,o.d("thenNowScore")?:0.0,o.str("locationConfidence")?:"",o.str("sceneDescription")?:"","cinema",o.str("updatedAt")?:generated?:"",o)
                "history" -> { val cats=o["categories"]?.jsonArray?.mapNotNull{it.jsonPrimitive.contentOrNull}.orEmpty(); SeedGeoItem(o.str("id")?:stable(dataset,o,lat,lon),o.str("name")?:"",lat,lon,o.str("area")?:o.str("region")?:"","history",cats.firstOrNull()?:o.str("era")?:"",o.d("importanceScore")?:0.0,o.d("storyScore")?:0.0,o.str("confidence")?:"",o.str("summary")?:"",cats.firstOrNull()?:o.str("era")?:"history",o.str("updatedAt")?:generated?:"",o) }
                else -> null
            }
        }
        return SeedGeoDataset(1,dataset,generated,rows)
    }
    fun normalizedEvents(body:String): SeedEventDataset? = runCatching { json.decodeFromString<SeedEventDataset>(body) }.getOrNull()
    fun eventsApi(body:String): List<EventEntity> {
        val root=json.parseToJsonElement(body).jsonObject
        val arr=root["events"]?.jsonArray ?: return emptyList()
        return arr.mapNotNull { e ->
            val o=e.jsonObject; val id=o.str("id")?:return@mapNotNull null; val lat=o.d("lat")?:return@mapNotNull null; val lon=o.d("lon")?:return@mapNotNull null
            val people=listOf("artists","bloggers","speakers","performers").flatMap { k -> o[k]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty() }.distinct()
            EventEntity(id,o.str("title")?:"",o.str("subtitle")?:"",o.str("descriptionFull")?:o.str("descriptionShort")?:"",o.str("category")?:"other",o.str("startDateTime")?:return@mapNotNull null,o.str("endDateTime"),o.str("status")?:"UNKNOWN",o.str("venueName")?:"",o.str("address")?:"",lat,lon,o.str("priceType")?:"UNKNOWN",o.d("priceMin"),o.d("priceMax"),o.bool("registrationRequired"),o.str("ticketUrl"),o.str("registrationUrl"),o.str("ageRating"),JsonArray(people.map(::JsonPrimitive)).toString(),o.str("sourceConfidence")?:"",o.str("updatedAt")?:o.str("sourceLastCheckedAt")?:"",o.toString())
        }
    }
    private fun stable(dataset:String,o:JsonObject,lat:Double,lon:Double)="${dataset}_${(o.str("name")?:"item").lowercase().replace(Regex("[^a-zа-я0-9]+"),"_").take(24)}_${kotlin.math.abs((lat*10000).toInt())}_${kotlin.math.abs((lon*10000).toInt())}"
}
private fun JsonObject.str(k:String)=this[k]?.jsonPrimitive?.contentOrNull
private fun JsonObject.d(k:String)=this[k]?.jsonPrimitive?.doubleOrNull
private fun JsonObject.bool(k:String)=this[k]?.jsonPrimitive?.booleanOrNull ?: false
