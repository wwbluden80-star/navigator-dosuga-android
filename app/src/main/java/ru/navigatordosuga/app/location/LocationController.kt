package ru.navigatordosuga.app.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LocationController(private val context:Context){
    private val client=LocationServices.getFusedLocationProviderClient(context)
    fun hasAnyPermission()=ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED
    suspend fun currentLocation(highAccuracy:Boolean=true):Location?{
        if(!hasAnyPermission())return null
        return suspendCancellableCoroutine { cont ->
            val source=com.google.android.gms.tasks.CancellationTokenSource()
            cont.invokeOnCancellation { source.cancel() }
            client.getCurrentLocation(if(highAccuracy)Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY,source.token)
                .addOnSuccessListener{if(cont.isActive)cont.resume(it)}.addOnFailureListener{if(cont.isActive)cont.resume(null)}
        }
    }
    fun updates(intervalMs:Long=3000):Flow<Location> = callbackFlow {
        if(!hasAnyPermission()){close();return@callbackFlow}
        val req=LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,intervalMs).setMinUpdateIntervalMillis(intervalMs/2).setMinUpdateDistanceMeters(2f).build()
        val cb=object:LocationCallback(){override fun onLocationResult(result:LocationResult){result.locations.forEach{trySend(it)}}}
        client.requestLocationUpdates(req,cb,android.os.Looper.getMainLooper())
        awaitClose{client.removeLocationUpdates(cb)}
    }
}
