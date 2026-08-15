package ru.navigatordosuga.app.location

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import ru.navigatordosuga.app.data.db.CarMarkerDao
import ru.navigatordosuga.app.data.db.CarMarkerEntity
import java.util.UUID

class TrackManager(private val context:Context){
    fun start(profileId:String):String{val id="track_${UUID.randomUUID()}";ContextCompat.startForegroundService(context,Intent(context,TrackRecordingService::class.java).putExtra(TrackRecordingService.EXTRA_PROFILE,profileId).putExtra(TrackRecordingService.EXTRA_TRACK,id));return id}
    fun stop(){context.startService(Intent(context,TrackRecordingService::class.java).setAction(TrackRecordingService.ACTION_STOP))}
}
class CarMarkerRepository(private val dao:CarMarkerDao){fun observe(profileId:String)=dao.observe(profileId);suspend fun save(profileId:String,lat:Double,lon:Double)=dao.upsert(CarMarkerEntity(profileId,lat,lon,System.currentTimeMillis()));suspend fun clear(profileId:String)=dao.clear(profileId)}
