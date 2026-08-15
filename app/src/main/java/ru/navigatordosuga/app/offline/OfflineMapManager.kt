package ru.navigatordosuga.app.offline

import android.content.Context
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.offline.*
import ru.navigatordosuga.app.BuildConfig
import java.nio.charset.StandardCharsets

data class OfflinePack(val id:Long,val name:String,val bytes:Long,val complete:Boolean,val progress:Int)
data class OfflineProgress(val id:Long,val name:String,val progress:Int,val bytes:Long,val complete:Boolean,val error:String?=null)

class OfflineMapManager(context:Context){
    private val app=context.applicationContext
    private val manager=OfflineManager.getInstance(app)
    fun list():Flow<List<OfflinePack>> = callbackFlow {
        manager.listOfflineRegions(object:OfflineManager.ListOfflineRegionsCallback{
            override fun onList(offlineRegions:Array<OfflineRegion>?) {
                val regions=offlineRegions.orEmpty();if(regions.isEmpty()){trySend(emptyList());close();return}
                val result=ArrayList<OfflinePack>();var left=regions.size
                regions.forEach{r->r.getStatus(object:OfflineRegion.OfflineRegionStatusCallback{
                    override fun onStatus(status:OfflineRegionStatus){val p=if(status.requiredResourceCount>0)((status.completedResourceCount*100/status.requiredResourceCount).coerceIn(0,100)).toInt() else if(status.isComplete)100 else 0;result+=OfflinePack(r.id,metadataName(r.metadata),status.completedResourceSize,status.isComplete,p);if(--left==0){trySend(result.sortedBy{it.name});close()}}
                    override fun onError(error:String){result+=OfflinePack(r.id,metadataName(r.metadata),0,false,0);if(--left==0){trySend(result);close()}}
                })}
            }
            override fun onError(error:String){close(IllegalStateException(error))}
        });awaitClose{}
    }
    fun download(name:String,bounds:LatLngBounds,minZoom:Double=6.0,maxZoom:Double=15.0):Flow<OfflineProgress> = callbackFlow {
        val def=OfflineTilePyramidRegionDefinition(BuildConfig.MAP_STYLE_URL,bounds,minZoom,maxZoom,app.resources.displayMetrics.density,false)
        val meta=("{\"name\":\""+name.replace("\"","")+"\"}").toByteArray(StandardCharsets.UTF_8)
        manager.createOfflineRegion(def,meta,object:OfflineManager.CreateOfflineRegionCallback{
            override fun onCreate(region:OfflineRegion){
                region.setObserver(object:OfflineRegion.OfflineRegionObserver{
                    override fun onStatusChanged(status:OfflineRegionStatus){val p=if(status.requiredResourceCount>0)((status.completedResourceCount*100/status.requiredResourceCount).coerceIn(0,100)).toInt() else 0;trySend(OfflineProgress(region.id,name,p,status.completedResourceSize,status.isComplete));if(status.isComplete){region.setDownloadState(OfflineRegion.STATE_INACTIVE);close()}}
                    override fun onError(error:OfflineRegionError){trySend(OfflineProgress(region.id,name,0,0,false,error.message))}
                    override fun mapboxTileCountLimitExceeded(limit:Long){trySend(OfflineProgress(region.id,name,0,0,false,"Достигнут лимит тайлов: $limit"))}
                });region.setDownloadState(OfflineRegion.STATE_ACTIVE)
            }
            override fun onError(error:String){close(IllegalStateException(error))}
        });awaitClose{}
    }
    fun delete(id:Long,onDone:(Boolean)->Unit){manager.getOfflineRegion(id,object:OfflineManager.GetOfflineRegionCallback{override fun onRegion(region:OfflineRegion){region.delete(object:OfflineRegion.OfflineRegionDeleteCallback{override fun onDelete(){onDone(true)};override fun onError(error:String){onDone(false)}})};override fun onRegionNotFound(){onDone(false)};override fun onError(error:String){onDone(false)}})}
    private fun metadataName(b:ByteArray)=runCatching{String(b,StandardCharsets.UTF_8).substringAfter("\"name\":\"").substringBefore('"')}.getOrDefault("Офлайн-регион")
}
