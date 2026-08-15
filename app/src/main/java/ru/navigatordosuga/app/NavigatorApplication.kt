package ru.navigatordosuga.app

import android.app.Application
import kotlinx.coroutines.*
import ru.navigatordosuga.app.data.sync.SyncScheduler
import org.maplibre.android.MapLibre

class NavigatorApplication:Application(){
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    override fun onCreate(){
        super.onCreate()
        MapLibre.getInstance(this)
        val c=AppContainer.get(this)
        scope.launch { c.seed.ensureSeeded(); SyncScheduler.schedule(this@NavigatorApplication) }
    }
}
