package ru.navigatordosuga.app.map

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.gson.JsonObject
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.expressions.Expression.has
import org.maplibre.android.style.expressions.Expression.not
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconAnchor
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.iconSize
import org.maplibre.android.style.layers.PropertyFactory.textAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.textColor
import org.maplibre.android.style.layers.PropertyFactory.textField
import org.maplibre.android.style.layers.PropertyFactory.textIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.textSize
import org.maplibre.android.style.layers.Property.ICON_ANCHOR_CENTER
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonOptions
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import ru.navigatordosuga.app.model.EventItem
import ru.navigatordosuga.app.model.GeoItem
import ru.navigatordosuga.app.model.MapCameraState
import kotlin.math.hypot
import kotlin.math.roundToInt

private const val SOURCE="opr-content-source"

@Composable
fun NativeMap(
    items:List<GeoItem>, events:List<EventItem>, camera:MapCameraState, modifier:Modifier=Modifier,
    dark:Boolean=false,
    onCameraChanged:(MapCameraState)->Unit={}, onItemClick:(String)->Unit={}
){
    val context=LocalContext.current; val owner=LocalLifecycleOwner.current
    val mapView=remember{MapView(context).apply{onCreate(Bundle())}}
    var map by remember{mutableStateOf<MapLibreMap?>(null)}
    var cameraTick by remember{mutableIntStateOf(0)}
    var styleLoaded by remember{mutableStateOf(false)}
    var requestedStyle by remember{mutableStateOf<String?>(null)}
    var listenersInstalled by remember{mutableStateOf(false)}
    val latestCameraChanged by rememberUpdatedState(onCameraChanged)
    val latestItemClick by rememberUpdatedState(onItemClick)
    val latestItems by rememberUpdatedState(items)
    val latestEvents by rememberUpdatedState(events)
    val markerEntries=remember(items,events){
        buildList{
            items.forEach{x->val lat=x.lat?:return@forEach;val lon=x.lon?:return@forEach;add(MapMarkerEntry(x.id,lat,lon,glassMarkerBitmap(context,MapMarkerRegistry.resource(x),x.score.roundToInt().toString(),false)))}
            events.forEach{x->add(MapMarkerEntry(x.id,x.lat,x.lon,glassMarkerBitmap(context,MapMarkerRegistry.resource(x),if(x.isFree)"0" else x.priceMin?.roundToInt()?.toString()?:"₽",true)))}
        }.also{Log.i("NativeMap","MARKER_OVERLAY_RENDERED count=${it.size}")}
    }
    fun applyStyle(m:MapLibreMap){
        val key=if(dark)"dark" else "light"
        if(requestedStyle==key)return
        requestedStyle=key;styleLoaded=false
        m.setStyle(Style.Builder().fromJson(baseMapStyle(dark))){style->
            MapMarkerRegistry.install(context,style)
            installLayers(style)
            styleLoaded=true
            updateSource(style,items,events)
            cameraTick++
        }
    }
    DisposableEffect(owner,mapView){
        var destroyed=false;var started=false;var resumed=false
        fun startOnce(){if(!destroyed&&!started){mapView.onStart();started=true}}
        fun resumeOnce(){if(!destroyed&&!resumed){startOnce();mapView.onResume();resumed=true}}
        fun pauseOnce(){if(resumed){mapView.onPause();resumed=false}}
        fun stopOnce(){pauseOnce();if(started){mapView.onStop();started=false}}
        fun destroyOnce(){if(!destroyed){stopOnce();destroyed=true;mapView.onDestroy()}}
        if(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))startOnce()
        if(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))resumeOnce()
        val obs=LifecycleEventObserver{_,e->when(e){Lifecycle.Event.ON_START->startOnce();Lifecycle.Event.ON_RESUME->resumeOnce();Lifecycle.Event.ON_PAUSE->pauseOnce();Lifecycle.Event.ON_STOP->stopOnce();Lifecycle.Event.ON_DESTROY->destroyOnce();else->{}}}
        owner.lifecycle.addObserver(obs)
        onDispose{owner.lifecycle.removeObserver(obs);destroyOnce()}
    }
    LaunchedEffect(items,events,styleLoaded,map){
        if(styleLoaded)map?.let{m->m.style?.let{updateSource(it,items,events)}}
    }
    Box(modifier){
        AndroidView(factory={mapView},modifier=Modifier.fillMaxSize(),update={ view ->
            if(map==null)view.getMapAsync{m->
                map=m
                m.cameraPosition=CameraPosition.Builder().target(LatLng(camera.lat,camera.lon)).zoom(camera.zoom).bearing(camera.bearing).tilt(camera.tilt).build()
                if(!listenersInstalled){
                    listenersInstalled=true
                    m.addOnCameraMoveListener{cameraTick++}
                    m.addOnCameraIdleListener{cameraTick++;val c=m.cameraPosition;latestCameraChanged(MapCameraState(c.target?.latitude?:camera.lat,c.target?.longitude?:camera.lon,c.zoom,c.bearing,c.tilt))}
                    m.addOnMapClickListener{tap->
                        val p=m.projection.toScreenLocation(tap)
                        val candidates=buildList<Pair<String,LatLng>>{
                            latestItems.forEach{x->if(x.lat!=null&&x.lon!=null)add(x.id to LatLng(x.lat,x.lon))}
                            latestEvents.forEach{x->add(x.id to LatLng(x.lat,x.lon))}
                        }
                        val hit=candidates.map{x->x to m.projection.toScreenLocation(x.second)}.minByOrNull{(_,q)->hypot((q.x-p.x).toDouble(),(q.y-p.y).toDouble())}
                        val distance=hit?.second?.let{q->hypot((q.x-p.x).toDouble(),(q.y-p.y).toDouble())}?:Double.MAX_VALUE
                        if(hit!=null&&distance<=64f*context.resources.displayMetrics.density){latestItemClick(hit.first.first);true}else false
                    }
                }
                applyStyle(m)
            } else {
                map?.let(::applyStyle)
                if(styleLoaded)map?.style?.let{updateSource(it,items,events)}
            }
        })
        Canvas(Modifier.fillMaxSize()){
            cameraTick
            val m=map?:return@Canvas
            drawContext.canvas.nativeCanvas.let{canvas->markerEntries.forEach{entry->
                val p=m.projection.toScreenLocation(LatLng(entry.lat,entry.lon));val half=entry.bitmap.width/2f
                if(p.x>=-half&&p.x<=size.width+half&&p.y>=-half&&p.y<=size.height+half)canvas.drawBitmap(entry.bitmap,p.x-half,p.y-half,null)
            }}
        }
    }
}

private fun baseMapStyle(dark:Boolean):String{
    val tiles=if(dark)"https://a.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}@2x.png" else "https://tile.openstreetmap.org/{z}/{x}/{y}.png"
    val background=if(dark)"#101715" else "#e9f0e5"
    val attribution=if(dark)"© OpenStreetMap contributors © CARTO" else "© OpenStreetMap contributors"
    return """{
        "version":8,
        "name":"Navigator Dosuga ${if(dark)"Dark" else "Light"}",
        "sources":{"opr-basemap":{"type":"raster","tiles":["$tiles"],"tileSize":256,"maxzoom":19,"attribution":"$attribution"}},
        "layers":[
          {"id":"opr-background","type":"background","paint":{"background-color":"$background"}},
          {"id":"opr-basemap-layer","type":"raster","source":"opr-basemap","paint":{"raster-opacity":1.0,"raster-fade-duration":180}}
        ]
    }""".trimIndent()
}

private fun installLayers(style:Style){
    if(style.getSource(SOURCE)==null)style.addSource(GeoJsonSource(SOURCE,FeatureCollection.fromFeatures(arrayOf()),GeoJsonOptions().withCluster(true).withClusterRadius(52).withClusterMaxZoom(13)))
    if(style.getLayer("opr-content-clusters")==null){
        style.addLayer(CircleLayer("opr-content-clusters",SOURCE).withFilter(has("point_count")).withProperties(
            circleColor("#EAF5EF"),circleRadius(25f),circleStrokeColor("#278C67"),circleStrokeWidth(3f)
        ))
    }
    if(style.getLayer("opr-content-cluster-count")==null){
        style.addLayer(SymbolLayer("opr-content-cluster-count",SOURCE).withFilter(has("point_count")).withProperties(
            textField(get("point_count_abbreviated")),textSize(14f),textColor("#15201D"),
            textAllowOverlap(true),textIgnorePlacement(true)
        ))
    }
    if(style.getLayer("opr-content-markers")==null){
        style.addLayer(SymbolLayer("opr-content-markers",SOURCE).withFilter(not(has("point_count"))).withProperties(
            iconImage(get("icon")),iconSize(0.42f),iconAnchor(ICON_ANCHOR_CENTER),
            iconAllowOverlap(true),iconIgnorePlacement(true)
        ))
    }
}
private fun updateSource(style:Style,items:List<GeoItem>,events:List<EventItem>){
    val features=ArrayList<Feature>(items.size+events.size)
    items.forEach{x->val lat=x.lat?:return@forEach;val lon=x.lon?:return@forEach;val p=JsonObject().apply{addProperty("id",x.id);addProperty("name",x.name);addProperty("icon",MapMarkerRegistry.icon(x));addProperty("score",x.score.toInt())};features+=Feature.fromGeometry(Point.fromLngLat(lon,lat),p)}
    events.forEach{x->val p=JsonObject().apply{addProperty("id",x.id);addProperty("name",x.title);addProperty("icon",MapMarkerRegistry.icon(x));addProperty("score",if(x.isFree)0 else x.priceMin?.toInt()?:0);addProperty("free",x.isFree);addProperty("event",true)};features+=Feature.fromGeometry(Point.fromLngLat(x.lon,x.lat),p)}
    (style.getSource(SOURCE) as? GeoJsonSource)?.setGeoJson(FeatureCollection.fromFeatures(features))
    Log.i("NativeMap","MARKER_SOURCE_UPDATE count=${features.size} geo=${items.size} events=${events.size}")
}

private fun glassMarkerBitmap(context:android.content.Context,res:Int,label:String,event:Boolean):Bitmap{
    val size=128
    val bitmap=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888)
    val canvas=android.graphics.Canvas(bitmap)
    val accent=if(event)Color.rgb(216,79,121) else Color.rgb(39,140,103)
    val panel=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.argb(218,238,246,241)}
    canvas.drawRoundRect(RectF(7f,7f,112f,112f),30f,30f,panel)
    val stroke=Paint(Paint.ANTI_ALIAS_FLAG).apply{style=Paint.Style.STROKE;strokeWidth=3f;color=Color.argb(205,Color.red(accent),Color.green(accent),Color.blue(accent))}
    canvas.drawRoundRect(RectF(7f,7f,112f,112f),30f,30f,stroke)
    BitmapFactory.decodeResource(context.resources,res)?.let{source->canvas.drawBitmap(source,null,Rect(28,26,92,90),Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))}
    val badge=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.argb(245,250,252,251)}
    canvas.drawCircle(101f,101f,24f,badge)
    val text=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.rgb(21,32,29);textSize=if(label.length>2)17f else 22f;textAlign=Paint.Align.CENTER;typeface=android.graphics.Typeface.DEFAULT_BOLD}
    canvas.drawText(label.take(4),101f,108f,text)
    return bitmap
}

private data class MapMarkerEntry(val id:String,val lat:Double,val lon:Double,val bitmap:Bitmap)
