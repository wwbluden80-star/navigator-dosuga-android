package ru.navigatordosuga.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MushroomEntity::class, FishingEntity::class, BeautifulEntity::class, CinemaEntity::class, HistoryEntity::class, EventEntity::class, ProfileEntity::class, SavedItemEntity::class, TripItemEntity::class, SyncStateEntity::class, PendingActionEntity::class, GameStatsEntity::class, GameRunEntity::class, TrackPointEntity::class, CarMarkerEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contentDao(): ContentDao
    abstract fun eventDao(): EventDao
    abstract fun profileDao(): ProfileDao
    abstract fun savedDao(): SavedDao
    abstract fun tripDao(): TripDao
    abstract fun syncDao(): SyncDao
    abstract fun pendingDao(): PendingDao
    abstract fun gameDao(): GameDao
    abstract fun trackDao(): TrackDao
    abstract fun carMarkerDao(): CarMarkerDao
    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "navigator-dosuga.db")
                .fallbackToDestructiveMigrationOnDowngrade()
                .build().also { INSTANCE = it }
        }
    }
}
