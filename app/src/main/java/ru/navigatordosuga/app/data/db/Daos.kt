package ru.navigatordosuga.app.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ContentDao {
    @Query("SELECT * FROM mushroom_places ORDER BY score DESC") fun mushrooms(): Flow<List<MushroomEntity>>
    @Query("SELECT * FROM fishing_places ORDER BY score DESC") fun fishing(): Flow<List<FishingEntity>>
    @Query("SELECT * FROM beautiful_places ORDER BY score DESC") fun beautiful(): Flow<List<BeautifulEntity>>
    @Query("SELECT * FROM cinema_places ORDER BY score DESC") fun cinema(): Flow<List<CinemaEntity>>
    @Query("SELECT * FROM history_places ORDER BY score DESC") fun history(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertMushrooms(v: List<MushroomEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertFishing(v: List<FishingEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertBeautiful(v: List<BeautifulEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertCinema(v: List<CinemaEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertHistory(v: List<HistoryEntity>)

    @Query("DELETE FROM mushroom_places") suspend fun clearMushrooms()
    @Query("DELETE FROM fishing_places") suspend fun clearFishing()
    @Query("DELETE FROM beautiful_places") suspend fun clearBeautiful()
    @Query("DELETE FROM cinema_places") suspend fun clearCinema()
    @Query("DELETE FROM history_places") suspend fun clearHistory()

    @Query("SELECT COUNT(*) FROM mushroom_places") suspend fun mushroomCount(): Int
    @Query("SELECT COUNT(*) FROM fishing_places") suspend fun fishingCount(): Int
    @Query("SELECT COUNT(*) FROM beautiful_places") suspend fun beautifulCount(): Int
    @Query("SELECT COUNT(*) FROM cinema_places") suspend fun cinemaCount(): Int
    @Query("SELECT COUNT(*) FROM history_places") suspend fun historyCount(): Int

    @Query("SELECT * FROM mushroom_places WHERE lower(name || ' ' || summary || ' ' || category || ' ' || subCategory) LIKE '%' || lower(:query) || '%' ORDER BY score DESC LIMIT :limit") suspend fun searchMushrooms(query:String,limit:Int=20):List<MushroomEntity>
    @Query("SELECT * FROM fishing_places WHERE lower(name || ' ' || summary || ' ' || category || ' ' || subCategory) LIKE '%' || lower(:query) || '%' ORDER BY score DESC LIMIT :limit") suspend fun searchFishing(query:String,limit:Int=20):List<FishingEntity>
    @Query("SELECT * FROM beautiful_places WHERE lower(name || ' ' || summary || ' ' || category || ' ' || subCategory) LIKE '%' || lower(:query) || '%' ORDER BY score DESC LIMIT :limit") suspend fun searchBeautiful(query:String,limit:Int=20):List<BeautifulEntity>
    @Query("SELECT * FROM cinema_places WHERE lower(name || ' ' || summary || ' ' || category || ' ' || subCategory) LIKE '%' || lower(:query) || '%' ORDER BY score DESC LIMIT :limit") suspend fun searchCinema(query:String,limit:Int=20):List<CinemaEntity>
    @Query("SELECT * FROM history_places WHERE lower(name || ' ' || summary || ' ' || category || ' ' || subCategory) LIKE '%' || lower(:query) || '%' ORDER BY score DESC LIMIT :limit") suspend fun searchHistory(query:String,limit:Int=20):List<HistoryEntity>
}

@Dao
interface EventDao {
    @Query("SELECT * FROM events WHERE status IN ('CONFIRMED','REGISTRATION_OPEN','ON_SALE','SOLD_OUT') AND startDateTime <= :toIso AND COALESCE(endDateTime,startDateTime) >= :fromIso ORDER BY startDateTime ASC")
    fun visible(fromIso: String, toIso: String): Flow<List<EventEntity>>
    @Query("SELECT * FROM events WHERE lower(title || ' ' || venueName || ' ' || category || ' ' || participantsJson) LIKE '%' || lower(:query) || '%' ORDER BY startDateTime ASC LIMIT :limit")
    suspend fun search(query: String, limit: Int = 100): List<EventEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(v: List<EventEntity>)
    @Query("DELETE FROM events WHERE id IN (:ids)") suspend fun deleteIds(ids: List<String>)
    @Query("DELETE FROM events") suspend fun clear()
    @Query("SELECT COUNT(*) FROM events") suspend fun count(): Int
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY updatedAt DESC") fun all(): Flow<List<ProfileEntity>>
    @Query("SELECT * FROM profiles WHERE id=:id LIMIT 1") suspend fun get(id: String): ProfileEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(v: ProfileEntity)
}

@Dao
interface SavedDao {
    @Query("SELECT * FROM saved_items WHERE profileId=:profileId ORDER BY savedAt DESC") fun all(profileId: String): Flow<List<SavedItemEntity>>
    @Query("SELECT EXISTS(SELECT 1 FROM saved_items WHERE profileId=:profileId AND dataset=:dataset AND itemId=:itemId)") suspend fun exists(profileId: String,dataset:String,itemId:String): Boolean
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(v: SavedItemEntity)
    @Query("DELETE FROM saved_items WHERE profileId=:profileId AND dataset=:dataset AND itemId=:itemId") suspend fun delete(profileId:String,dataset:String,itemId:String)
}

@Dao
interface TripDao {
    @Query("SELECT * FROM trip_items WHERE profileId=:profileId ORDER BY position") fun all(profileId: String): Flow<List<TripItemEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(v: TripItemEntity)
    @Query("DELETE FROM trip_items WHERE profileId=:profileId AND itemId=:itemId") suspend fun delete(profileId:String,itemId:String)
    @Query("DELETE FROM trip_items WHERE profileId=:profileId") suspend fun clear(profileId:String)
}

@Dao
interface SyncDao {
    @Query("SELECT * FROM sync_state WHERE dataset=:dataset LIMIT 1") suspend fun get(dataset:String): SyncStateEntity?
    @Query("SELECT * FROM sync_state") fun all(): Flow<List<SyncStateEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(v: SyncStateEntity)
}

@Dao
interface PendingDao {
    @Query("SELECT * FROM pending_actions ORDER BY createdAt LIMIT :limit") suspend fun next(limit:Int=100): List<PendingActionEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(v: PendingActionEntity)
    @Query("DELETE FROM pending_actions WHERE id=:id") suspend fun delete(id:String)
}

@Dao
interface GameDao {
    @Query("SELECT * FROM game_stats WHERE profileId=:profileId AND gameId=:gameId LIMIT 1") fun stats(profileId:String, gameId:String="tower_builder"): Flow<GameStatsEntity?>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertStats(v: GameStatsEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertRun(v: GameRunEntity)
    @Query("SELECT * FROM game_runs WHERE profileId=:profileId AND gameId=:gameId ORDER BY endedAt DESC LIMIT :limit") fun runs(profileId:String,gameId:String="tower_builder",limit:Int=20): Flow<List<GameRunEntity>>
    @Query("SELECT * FROM game_runs WHERE synced=0 ORDER BY endedAt LIMIT :limit") suspend fun pendingRuns(limit:Int=50): List<GameRunEntity>
    @Query("UPDATE game_runs SET synced=1 WHERE runId=:runId") suspend fun markSynced(runId:String)
}


@Dao
interface TrackDao {
    @Query("SELECT * FROM track_points WHERE profileId=:profileId AND trackId=:trackId ORDER BY seq") fun points(profileId:String,trackId:String): Flow<List<TrackPointEntity>>
    @Query("SELECT MAX(seq) FROM track_points WHERE trackId=:trackId") suspend fun lastSeq(trackId:String): Int?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(v:TrackPointEntity)
}

@Dao
interface CarMarkerDao {
    @Query("SELECT * FROM car_markers WHERE profileId=:profileId LIMIT 1") fun observe(profileId:String):Flow<CarMarkerEntity?>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(v:CarMarkerEntity)
    @Query("DELETE FROM car_markers WHERE profileId=:profileId") suspend fun clear(profileId:String)
}
