package ru.navigatordosuga.app

import android.app.Application
import android.util.Log
import kotlinx.coroutines.*
import ru.navigatordosuga.app.data.sync.SyncScheduler
import org.maplibre.android.MapLibre

class NavigatorApplication:Application(){
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    override fun onCreate(){
        super.onCreate()
        MapLibre.getInstance(this)
        val c=AppContainer.get(this)
        scope.launch {
            runCatching { c.seed.ensureSeeded() }
                .onFailure { Log.e("NavigatorApplication","Offline seed import failed",it) }
            SyncScheduler.schedule(this@NavigatorApplication)
        }
    }
}
