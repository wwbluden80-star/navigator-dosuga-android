package ru.navigatordosuga.app.data.db

import androidx.room.Entity
import androidx.room.Index

@Entity(tableName = "mushroom_places", indices = [Index("lat","lon"), Index("score"), Index("updatedAt")])
data class MushroomEntity(@androidx.room.PrimaryKey val id: String, val name: String, val lat: Double?, val lon: Double?, val region: String, val category: String, val subCategory: String, val score: Double, val secondaryScore: Double, val confidence: String, val summary: String, val iconKey: String, val updatedAt: String, val payloadJson: String)
@Entity(tableName = "fishing_places", indices = [Index("lat","lon"), Index("score"), Index("updatedAt")])
data class FishingEntity(@androidx.room.PrimaryKey val id: String, val name: String, val lat: Double?, val lon: Double?, val region: String, val category: String, val subCategory: String, val score: Double, val secondaryScore: Double, val confidence: String, val summary: String, val iconKey: String, val updatedAt: String, val payloadJson: String)
@Entity(tableName = "beautiful_places", indices = [Index("lat","lon"), Index("score"), Index("updatedAt")])
data class BeautifulEntity(@androidx.room.PrimaryKey val id: String, val name: String, val lat: Double?, val lon: Double?, val region: String, val category: String, val subCategory: String, val score: Double, val secondaryScore: Double, val confidence: String, val summary: String, val iconKey: String, val updatedAt: String, val payloadJson: String)
@Entity(tableName = "cinema_places", indices = [Index("lat","lon"), Index("score"), Index("updatedAt")])
data class CinemaEntity(@androidx.room.PrimaryKey val id: String, val name: String, val lat: Double?, val lon: Double?, val region: String, val category: String, val subCategory: String, val score: Double, val secondaryScore: Double, val confidence: String, val summary: String, val iconKey: String, val updatedAt: String, val payloadJson: String)
@Entity(tableName = "history_places", indices = [Index("lat","lon"), Index("score"), Index("updatedAt")])
data class HistoryEntity(@androidx.room.PrimaryKey val id: String, val name: String, val lat: Double?, val lon: Double?, val region: String, val category: String, val subCategory: String, val score: Double, val secondaryScore: Double, val confidence: String, val summary: String, val iconKey: String, val updatedAt: String, val payloadJson: String)

@Entity(tableName = "events", indices = [Index("startDateTime"), Index("status"), Index("category"), Index("lat","lon"), Index("updatedAt")])
data class EventEntity(
    @androidx.room.PrimaryKey val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val category: String,
    val startDateTime: String,
    val endDateTime: String?,
    val status: String,
    val venueName: String,
    val address: String,
    val lat: Double,
    val lon: Double,
    val priceType: String,
    val priceMin: Double?,
    val priceMax: Double?,
    val registrationRequired: Boolean,
    val ticketUrl: String?,
    val registrationUrl: String?,
    val ageRating: String?,
    val participantsJson: String,
    val sourceConfidence: String,
    val updatedAt: String,
    val payloadJson: String
)

@Entity(tableName = "profiles")
data class ProfileEntity(@androidx.room.PrimaryKey val id: String, val displayName: String, val avatarUri: String?, val interestsCsv: String, val transportPreference: String, val maxTripDistanceKm: Int, val defaultActivity: String, val theme: String, val updatedAt: Long)

@Entity(tableName = "saved_items", primaryKeys = ["profileId","dataset","itemId"], indices = [Index("profileId"), Index("dataset")])
data class SavedItemEntity(val profileId: String, val dataset: String, val itemId: String, val snapshotJson: String, val savedAt: Long)

@Entity(tableName = "trip_items", primaryKeys = ["profileId","itemId"], indices = [Index("profileId"), Index("position")])
data class TripItemEntity(val profileId: String, val itemId: String, val dataset: String, val snapshotJson: String, val position: Int, val addedAt: Long)

@Entity(tableName = "sync_state")
data class SyncStateEntity(@androidx.room.PrimaryKey val dataset: String, val lastSuccessAt: Long = 0, val lastAttemptAt: Long = 0, val etag: String? = null, val checksum: String? = null, val version: String? = null, val status: String = "never", val error: String? = null)

@Entity(tableName = "pending_actions", indices = [Index("type"), Index("createdAt")])
data class PendingActionEntity(@androidx.room.PrimaryKey val id: String, val type: String, val payloadJson: String, val createdAt: Long, val attempts: Int = 0)

@Entity(tableName = "game_stats", primaryKeys = ["profileId","gameId"])
data class GameStatsEntity(val profileId: String, val gameId: String, val bestFloors: Int, val bestScore: Int, val bestPerfectStreak: Int, val totalGames: Int, val totalBlocks: Int, val totalPerfects: Int, val updatedAt: Long)

@Entity(tableName = "game_runs", indices = [Index("profileId"), Index("gameId"), Index("endedAt")])
data class GameRunEntity(@androidx.room.PrimaryKey val runId: String, val profileId: String, val gameId: String, val floors: Int, val score: Int, val perfects: Int, val bestCombo: Int, val durationSec: Int, val placements: String, val startedAt: String, val endedAt: String, val synced: Boolean = false)

@Entity(tableName = "track_points", primaryKeys = ["trackId","seq"], indices = [Index("profileId"), Index("timestamp")])
data class TrackPointEntity(val trackId:String,val seq:Int,val profileId:String,val lat:Double,val lon:Double,val altitude:Double?,val accuracy:Float,val timestamp:Long)

@Entity(tableName = "car_markers")
data class CarMarkerEntity(@androidx.room.PrimaryKey val profileId:String,val lat:Double,val lon:Double,val savedAt:Long)
