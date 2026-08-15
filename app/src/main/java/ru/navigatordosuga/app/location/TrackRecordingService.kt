package ru.navigatordosuga.app.location

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import ru.navigatordosuga.app.AppContainer
import ru.navigatordosuga.app.R
import ru.navigatordosuga.app.data.db.TrackPointEntity
import java.util.UUID

class TrackRecordingService:Service(){
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private lateinit var client:FusedLocationProviderClient
    private var callback:LocationCallback?=null
    private var profileId:String="guest"
    private var trackId:String=""
    private var seq=0
    override fun onCreate(){super.onCreate();client=LocationServices.getFusedLocationProviderClient(this);createChannel()}
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int{
        if(intent?.action==ACTION_STOP){stopTracking();stopSelf();return START_NOT_STICKY}
        profileId=intent?.getStringExtra(EXTRA_PROFILE)?:"guest";trackId=intent?.getStringExtra(EXTRA_TRACK)?:"track_${UUID.randomUUID()}"
        startForeground(NOTIFICATION_ID,notification())
        startTracking();return START_STICKY
    }
    private fun startTracking(){
        if(callback!=null)return
        val granted=ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED||ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED
        if(!granted){stopSelf();return}
        val req=LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,3000).setMinUpdateDistanceMeters(2f).setMinUpdateIntervalMillis(1500).build()
        callback=object:LocationCallback(){override fun onLocationResult(result:LocationResult){for(loc in result.locations){val n=++seq;scope.launch{AppContainer.get(this@TrackRecordingService).db.trackDao().insert(TrackPointEntity(trackId,n,profileId,loc.latitude,loc.longitude,if(loc.hasAltitude())loc.altitude else null,loc.accuracy,loc.time))}}}}
        try {
            client.requestLocationUpdates(req,callback!!,mainLooper)
        } catch (_:SecurityException) {
            callback=null;stopSelf()
        }
    }
    private fun stopTracking(){callback?.let{client.removeLocationUpdates(it)};callback=null}
    private fun createChannel(){if(android.os.Build.VERSION.SDK_INT>=26){val nm=getSystemService(NotificationManager::class.java);nm.createNotificationChannel(NotificationChannel(CHANNEL,"Запись маршрута",NotificationManager.IMPORTANCE_LOW))}}
    private fun notification():Notification{val stop=PendingIntent.getService(this,1,Intent(this,TrackRecordingService::class.java).setAction(ACTION_STOP),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT);return NotificationCompat.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_menu_mylocation).setContentTitle("Навигатор досуга").setContentText("Записываем ваш путь").setOngoing(true).addAction(0,"Остановить",stop).build()}
    override fun onDestroy(){stopTracking();scope.cancel();super.onDestroy()}
    override fun onBind(intent:Intent?):IBinder?=null
    companion object{const val ACTION_STOP="ru.navigatordosuga.STOP_TRACK";const val EXTRA_PROFILE="profile";const val EXTRA_TRACK="track";private const val CHANNEL="track_recording";private const val NOTIFICATION_ID=1107}
}
