package ru.navigatordosuga.app.data.db

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.navigatordosuga.app.model.*

fun MushroomEntity.domain()=GeoItem(id,ActivityMode.MUSHROOMS,name,lat,lon,region,category,subCategory,score,secondaryScore,confidence,summary,iconKey,updatedAt,payloadJson)
fun FishingEntity.domain()=GeoItem(id,ActivityMode.FISHING,name,lat,lon,region,category,subCategory,score,secondaryScore,confidence,summary,iconKey,updatedAt,payloadJson)
fun BeautifulEntity.domain()=GeoItem(id,ActivityMode.BEAUTIFUL,name,lat,lon,region,category,subCategory,score,secondaryScore,confidence,summary,iconKey,updatedAt,payloadJson)
fun CinemaEntity.domain()=GeoItem(id,ActivityMode.CINEMA,name,lat,lon,region,category,subCategory,score,secondaryScore,confidence,summary,iconKey,updatedAt,payloadJson)
fun HistoryEntity.domain()=GeoItem(id,ActivityMode.HISTORY,name,lat,lon,region,category,subCategory,score,secondaryScore,confidence,summary,iconKey,updatedAt,payloadJson)
fun EventEntity.domain(json:Json)=EventItem(id,title,subtitle,description,category,startDateTime,endDateTime,status,venueName,address,lat,lon,priceType,priceMin,priceMax,registrationRequired,ticketUrl,registrationUrl,ageRating,runCatching{json.decodeFromString<List<String>>(participantsJson)}.getOrDefault(emptyList()),sourceConfidence,updatedAt,payloadJson)
