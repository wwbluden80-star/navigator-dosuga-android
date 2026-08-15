package ru.navigatordosuga.app.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.max
import kotlin.math.min

data class GlassMotion(val x:Float=0f,val y:Float=0f)

class LiveGlassController(context:Context):SensorEventListener{
    private val manager=context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor=manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) ?: manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val _motion=MutableStateFlow(GlassMotion())
    val motion:StateFlow<GlassMotion> = _motion
    private var running=false
    fun start(){if(running||sensor==null)return;running=manager.registerListener(this,sensor,SensorManager.SENSOR_DELAY_UI)}
    fun stop(){if(!running)return;manager.unregisterListener(this);running=false;_motion.value=GlassMotion()}
    override fun onAccuracyChanged(sensor:Sensor?,accuracy:Int)=Unit
    override fun onSensorChanged(event:SensorEvent){
        val rawX:Float
        val rawY:Float
        if(event.sensor.type==Sensor.TYPE_ROTATION_VECTOR){
            val r=FloatArray(9);SensorManager.getRotationMatrixFromVector(r,event.values)
            val o=FloatArray(3);SensorManager.getOrientation(r,o)
            rawX=(o[2]/0.55f).coerceIn(-1f,1f)
            rawY=(o[1]/0.55f).coerceIn(-1f,1f)
        }else{
            rawX=(event.values.getOrElse(0){0f}/6.5f).coerceIn(-1f,1f)
            rawY=(-event.values.getOrElse(1){0f}/6.5f).coerceIn(-1f,1f)
        }
        val old=_motion.value
        val a=.14f
        _motion.value=GlassMotion(old.x+(rawX-old.x)*a,old.y+(rawY-old.y)*a)
    }
}
