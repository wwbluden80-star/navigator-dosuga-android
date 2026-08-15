package ru.navigatordosuga.app.data.repository

import android.content.Context
import kotlinx.serialization.json.*

data class MushroomGuideItem(
    val id:String,
    val name:String,
    val scientificName:String,
    val group:String,
    val edibleStatus:String,
    val season:String,
    val habitat:String,
    val features:List<String>,
    val lookalikes:List<String>,
    val note:String,
    val imageUrl:String?=null,
    val imageSource:String?=null
)

data class FishGuideItem(
    val id:String,
    val name:String,
    val scientificName:String,
    val legalStatus:String,
    val minLegalSizeCm:Double?,
    val dailyLimit:String?,
    val methods:List<String>,
    val habitat:String,
    val notes:String
)

class GuideRepository(private val context:Context,private val json:Json){
    val mushrooms:List<MushroomGuideItem> by lazy { loadMushrooms() }
    val fish:List<FishGuideItem> by lazy { loadFish() }

    private fun loadMushrooms():List<MushroomGuideItem>{
        val root=read("seed/mushroom_guide.json")
        return root["species"]?.jsonArray.orEmpty().mapNotNull { el ->
            val o=el.jsonObject
            val id=o.s("id")?:return@mapNotNull null
            val looks=o["dangerousLookalikes"]?.jsonArray.orEmpty().mapNotNull { x -> x.jsonObject.s("label") }
            val image=o["images"]?.jsonArray?.firstOrNull()?.jsonObject
            MushroomGuideItem(
                id=id,
                name=o.s("russianName")?:id,
                scientificName=o.s("scientificName")?:"",
                group=o.s("guideGroup")?:o.s("category")?:"",
                edibleStatus=o.s("edibleStatus")?:"Статус не указан",
                season=o.s("season")?:"",
                habitat=o.s("habitat")?:"",
                features=o.strings("identificationFeatures"),
                lookalikes=looks,
                note=o.s("note")?:o.s("confusionNotes")?:"",
                imageUrl=image?.s("src"),
                imageSource=image?.s("sourceUrl")
            )
        }
    }

    private fun loadFish():List<FishGuideItem>{
        val root=read("seed/fishing_guide.json")
        return root["guide"]?.jsonArray.orEmpty().mapNotNull { el ->
            val o=el.jsonObject
            val id=o.s("id")?:return@mapNotNull null
            FishGuideItem(
                id=id,
                name=o.s("nameRu")?:id,
                scientificName=o.s("scientificName")?:"",
                legalStatus=o.s("legalStatusMoscowMO")?:"VERIFY_CURRENT_RULES",
                minLegalSizeCm=o.d("minLegalSizeCm"),
                dailyLimit=o.s("dailyCountLimit"),
                methods=o.strings("methodsBaseline"),
                habitat=o.s("habitatBaseline")?:"",
                notes=o.s("notes")?:""
            )
        }
    }

    private fun read(path:String):JsonObject = context.assets.open(path).bufferedReader().use { json.parseToJsonElement(it.readText()).jsonObject }
}
private fun JsonObject.s(k:String)=this[k]?.jsonPrimitive?.contentOrNull
private fun JsonObject.d(k:String)=this[k]?.jsonPrimitive?.doubleOrNull
private fun JsonObject.strings(k:String)=this[k]?.jsonArray?.mapNotNull{it.jsonPrimitive.contentOrNull}.orEmpty()
