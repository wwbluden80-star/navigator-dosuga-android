package ru.navigatordosuga.app

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import ru.navigatordosuga.app.data.db.AppDatabase
import ru.navigatordosuga.app.data.network.WebDataClient
import ru.navigatordosuga.app.data.repository.*
import ru.navigatordosuga.app.data.seed.SeedImporter
import ru.navigatordosuga.app.data.preferences.AppStateStore
import ru.navigatordosuga.app.game.tower.TowerLeaderboardSync
import ru.navigatordosuga.app.location.LocationController
import ru.navigatordosuga.app.location.TrackManager
import ru.navigatordosuga.app.location.CarMarkerRepository

class AppContainer private constructor(context: Context) {
    val appContext=context.applicationContext
    val json=Json { ignoreUnknownKeys=true; explicitNulls=false; coerceInputValues=true; isLenient=true }
    val db=AppDatabase.get(appContext)
    val prefs:DataStore<Preferences> = PreferenceDataStoreFactory.create(scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)){ appContext.preferencesDataStoreFile("navigator.preferences_pb") }
    val web=WebDataClient()
    val appState=AppStateStore(prefs)
    val content=ContentRepository(db)
    val events=EventRepository(db,json)
    val profiles=ProfileRepository(db.profileDao(),prefs)
    val saved=SavedRepository(db.savedDao())
    val trips=TripRepository(db.tripDao())
    val search=SearchRepository(db,json)
    val guides=GuideRepository(appContext,json)
    val seed=SeedImporter(appContext,db,json)
    val location=LocationController(appContext)
    val tracks=TrackManager(appContext)
    val car=CarMarkerRepository(db.carMarkerDao())
    val towerSync=TowerLeaderboardSync(db,web,json)
    companion object {
        @Volatile private var instance:AppContainer?=null
        fun get(context:Context)=instance ?: synchronized(this){instance ?: AppContainer(context).also{instance=it}}
    }
}
