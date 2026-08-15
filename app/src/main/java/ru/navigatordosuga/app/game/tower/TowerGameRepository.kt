package ru.navigatordosuga.app.game.tower

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import ru.navigatordosuga.app.data.db.*
import java.time.Instant
import java.util.UUID

class TowerGameRepository(private val db:AppDatabase){
    fun stats(profileId:String):Flow<GameStatsEntity?> = db.gameDao().stats(profileId)
    fun runs(profileId:String)=db.gameDao().runs(profileId)
    suspend fun save(profileId:String,score:ScoreState,startedAt:Instant,durationSec:Int):GameRunEntity{
        val now=Instant.now();val run=GameRunEntity("run_${UUID.randomUUID()}",profileId,"tower_builder",score.floors,score.score,score.perfects,score.bestCombo,durationSec,score.placements,startedAt.toString(),now.toString(),false)
        db.gameDao().insertRun(run)
        val old=db.gameDao().stats(profileId).first()
        db.gameDao().upsertStats(GameStatsEntity(profileId,"tower_builder",maxOf(old?.bestFloors?:0,score.floors),maxOf(old?.bestScore?:0,score.score),maxOf(old?.bestPerfectStreak?:0,score.bestCombo),(old?.totalGames?:0)+1,(old?.totalBlocks?:0)+score.floors,(old?.totalPerfects?:0)+score.perfects,System.currentTimeMillis()))
        db.pendingDao().upsert(PendingActionEntity(run.runId,"tower_score",run.runId,System.currentTimeMillis()))
        return run
    }
}
