package ru.navigatordosuga.app.data.sync

import android.content.Context
import androidx.room.withTransaction
import androidx.work.*
import kotlinx.serialization.json.*
import ru.navigatordosuga.app.AppContainer
import ru.navigatordosuga.app.data.db.*
import ru.navigatordosuga.app.data.network.WebNormalizers
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class ContentSyncWorker(ctx:Context, params:WorkerParameters):CoroutineWorker(ctx,params){
    override suspend fun doWork():Result {
        val c=AppContainer.get(applicationContext); if(!c.web.configured)return Result.success()
        val n=WebNormalizers(c.json)
        val specs=listOf(
            Triple("mushrooms","/data/mobile/v1/mushrooms.json","/data/embedded.json"),
            Triple("fishing","/data/mobile/v1/fishing.json","/data/fishing.json"),
            Triple("beautiful","/data/mobile/v1/beautiful.json","/data/beautiful_places.json"),
            Triple("cinema","/data/mobile/v1/cinema.json","/data/cinema.json"),
            Triple("history","/data/mobile/v1/history.json","/data/history_v15_alpha.json")
        )
        var failed=false
        for((dataset,normalized,legacy) in specs){
            val old=c.db.syncDao().get(dataset); c.db.syncDao().upsert((old?:SyncStateEntity(dataset)).copy(lastAttemptAt=System.currentTimeMillis(),status="updating",error=null))
            try{
                var r=c.web.get(normalized,old?.etag)
                val parsed=when {
                    r.code==304 -> null
                    r.code in 200..299 && r.body!=null -> n.normalizedGeo(r.body) ?: n.rawGeo(dataset,r.body)
                    else -> { r=c.web.get(legacy,old?.etag); if(r.code==304)null else if(r.code in 200..299&&r.body!=null)n.rawGeo(dataset,r.body) else error("HTTP ${r.code}") }
                }
                // A syntactically valid but empty backend response must never erase the
                // packaged offline dataset. Treat it as a sync failure so Room remains
                // useful without a network and the next scheduled run can retry.
                if(parsed!=null){
                    if(parsed.items.isEmpty())error("Empty $dataset dataset; keeping local data")
                    c.db.withTransaction { applyDataset(c.db,dataset,parsed.items,c.json) }
                }
                c.db.syncDao().upsert((old?:SyncStateEntity(dataset)).copy(lastAttemptAt=System.currentTimeMillis(),lastSuccessAt=System.currentTimeMillis(),etag=r.etag?:old?.etag,status="ok",error=null))
            }catch(t:Throwable){failed=true;c.db.syncDao().upsert((old?:SyncStateEntity(dataset)).copy(lastAttemptAt=System.currentTimeMillis(),status="error",error=t.message?.take(180)))}
        }
        return if(failed)Result.retry() else Result.success()
    }
}

class EventsSyncWorker(ctx:Context,params:WorkerParameters):CoroutineWorker(ctx,params){
    override suspend fun doWork():Result{
        val c=AppContainer.get(applicationContext);if(!c.web.configured)return Result.success();val zone=ZoneId.of("Europe/Moscow");val from=LocalDate.now(zone);val to=from.plusDays(90);var cursor:String?="0";val rows=mutableListOf<EventEntity>()
        return try{
            do{var path="/api/v1/events?dateFrom=$from&dateTo=$to&limit=500&cursor=${cursor?:"0"}";var r=c.web.get(path);if(r.code==404){path="/api/events?dateFrom=$from&dateTo=$to&limit=500&cursor=${cursor?:"0"}";r=c.web.get(path)};if(r.code !in 200..299||r.body==null)error("HTTP ${r.code}");val root=c.json.parseToJsonElement(r.body).jsonObject;rows+=WebNormalizers(c.json).eventsApi(r.body);cursor=root["nextCursor"]?.jsonPrimitive?.contentOrNull}while(cursor!=null&&rows.size<10000)
            if(rows.isEmpty())error("Empty events dataset; keeping local data")
            c.db.withTransaction { c.db.eventDao().clear();c.db.eventDao().upsert(rows) }
            c.db.syncDao().upsert(SyncStateEntity("events",System.currentTimeMillis(),System.currentTimeMillis(),status="ok",version="live"));Result.success()
        }catch(t:Throwable){val old=c.db.syncDao().get("events")?:SyncStateEntity("events");c.db.syncDao().upsert(old.copy(lastAttemptAt=System.currentTimeMillis(),status="error",error=t.message?.take(180)));Result.retry()}
    }
}

class PendingActionsWorker(ctx:Context,params:WorkerParameters):CoroutineWorker(ctx,params){ override suspend fun doWork():Result{ val c=AppContainer.get(applicationContext); if(!c.web.configured)return Result.success(); return c.towerSync.syncPending() } }

private suspend fun applyDataset(db:AppDatabase,dataset:String,items:List<ru.navigatordosuga.app.data.seed.SeedGeoItem>,json:Json){
    fun p(x:ru.navigatordosuga.app.data.seed.SeedGeoItem)=x.payload.toString()
    val d=db.contentDao();when(dataset){
        "mushrooms"->{d.clearMushrooms();d.upsertMushrooms(items.map{MushroomEntity(it.id,it.name,it.lat,it.lon,it.region,it.category,it.subCategory,it.score,it.secondaryScore,it.confidence,it.summary,it.iconKey,it.updatedAt,p(it))})}
        "fishing"->{d.clearFishing();d.upsertFishing(items.map{FishingEntity(it.id,it.name,it.lat,it.lon,it.region,it.category,it.subCategory,it.score,it.secondaryScore,it.confidence,it.summary,it.iconKey,it.updatedAt,p(it))})}
        "beautiful"->{d.clearBeautiful();d.upsertBeautiful(items.map{BeautifulEntity(it.id,it.name,it.lat,it.lon,it.region,it.category,it.subCategory,it.score,it.secondaryScore,it.confidence,it.summary,it.iconKey,it.updatedAt,p(it))})}
        "cinema"->{d.clearCinema();d.upsertCinema(items.map{CinemaEntity(it.id,it.name,it.lat,it.lon,it.region,it.category,it.subCategory,it.score,it.secondaryScore,it.confidence,it.summary,it.iconKey,it.updatedAt,p(it))})}
        "history"->{d.clearHistory();d.upsertHistory(items.map{HistoryEntity(it.id,it.name,it.lat,it.lon,it.region,it.category,it.subCategory,it.score,it.secondaryScore,it.confidence,it.summary,it.iconKey,it.updatedAt,p(it))})}
    }
}

object SyncScheduler{
    fun schedule(context:Context){
        val net=Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("content-sync",ExistingPeriodicWorkPolicy.UPDATE,PeriodicWorkRequestBuilder<ContentSyncWorker>(12,TimeUnit.HOURS).setConstraints(net).setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS).build())
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("events-sync",ExistingPeriodicWorkPolicy.UPDATE,PeriodicWorkRequestBuilder<EventsSyncWorker>(4,TimeUnit.HOURS).setConstraints(net).setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS).build())
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("pending-sync",ExistingPeriodicWorkPolicy.UPDATE,PeriodicWorkRequestBuilder<PendingActionsWorker>(1,TimeUnit.HOURS).setConstraints(net).build())
    }
    fun refreshNow(context:Context){val net=Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();WorkManager.getInstance(context).enqueueUniqueWork("manual-content-sync",ExistingWorkPolicy.REPLACE,OneTimeWorkRequestBuilder<ContentSyncWorker>().setConstraints(net).build());WorkManager.getInstance(context).enqueueUniqueWork("manual-events-sync",ExistingWorkPolicy.REPLACE,OneTimeWorkRequestBuilder<EventsSyncWorker>().setConstraints(net).build())}
}
