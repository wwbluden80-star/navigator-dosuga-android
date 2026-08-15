package ru.navigatordosuga.app.game.tower

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.navigatordosuga.app.data.db.AppDatabase
import ru.navigatordosuga.app.data.db.GameRunEntity
import ru.navigatordosuga.app.data.network.WebDataClient

@Serializable data class TowerScorePayload(val runId:String,val profileId:String,val displayName:String,val floors:Int,val score:Int,val perfects:Int,val bestCombo:Int,val duration:Int,val startedAt:String,val endedAt:String,val gameVersion:String="1.0.0",val physicsVersion:String="opr-rigidbody-android-1.0",val scoringVersion:String="tower-score-1.0",val placements:List<String>)
class TowerLeaderboardSync(private val db:AppDatabase,private val web:WebDataClient,private val json:Json){
    suspend fun syncPending():androidx.work.ListenableWorker.Result{
        if(!web.configured)return androidx.work.ListenableWorker.Result.success()
        var failed=false
        for(run in db.gameDao().pendingRuns()){
            val p=TowerScorePayload(run.runId,run.profileId,"Игрок",run.floors,run.score,run.perfects,run.bestCombo,run.durationSec,run.startedAt,run.endedAt,placements=run.placements.map{it.toString()})
            val r=runCatching{web.postJson("/api/games/tower/score",json.encodeToString(p))}.getOrNull()
            if(r!=null&&r.code in 200..299)db.gameDao().markSynced(run.runId) else failed=true
        }
        return if(failed)androidx.work.ListenableWorker.Result.retry() else androidx.work.ListenableWorker.Result.success()
    }
}
