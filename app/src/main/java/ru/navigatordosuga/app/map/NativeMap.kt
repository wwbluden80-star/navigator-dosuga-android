package ru.navigatordosuga.app.map

import android.graphics.Color
import android.os.Bundle
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
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
import org.maplibre.android.style.expressions.Expression.*
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonOptions
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import ru.navigatordosuga.app.model.EventItem
import ru.navigatordosuga.app.model.GeoItem
import ru.navigatordosuga.app.model.MapCameraState

private const val SOURCE="opr-content-source"
private const val ITEMS="opr-content-items"
private const val CLUSTERS="opr-content-clusters"
private const val COUNT="opr-content-count"
private const val BACKDROP="opr-content-backdrop"
private const val SCORES="opr-content-scores"

@Composable
fun NativeMap(
    items:List<GeoItem>, events:List<EventItem>, camera:MapCameraState, modifier:Modifier=Modifier,
    dark:Boolean=false,
    onCameraChanged:(MapCameraState)->Unit={}, onItemClick:(String)->Unit={}
){
    val context=LocalContext.current; val owner=LocalLifecycleOwner.current
    val mapView=remember{MapView(context).apply{onCreate(Bundle())}}
    var map by remember{mutableStateOf<MapLibreMap?>(null)}
    var styleLoaded by remember{mutableStateOf(false)}
    var requestedStyle by remember{mutableStateOf<String?>(null)}
    var listenersInstalled by remember{mutableStateOf(false)}
    val latestCameraChanged by rememberUpdatedState(onCameraChanged)
    val latestItemClick by rememberUpdatedState(onItemClick)
    fun applyStyle(m:MapLibreMap){
        val key=if(dark)"dark" else "light"
        if(requestedStyle==key)return
        requestedStyle=key;styleLoaded=false
        m.setStyle(Style.Builder().fromJson(baseMapStyle(dark))){style->
            MapMarkerRegistry.install(context,style)
            installLayers(style)
            styleLoaded=true
            updateSource(style,items,events)
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
    AndroidView(factory={mapView},modifier=modifier,update={ view ->
        if(map==null)view.getMapAsync{m->
            map=m
            m.cameraPosition=CameraPosition.Builder().target(LatLng(camera.lat,camera.lon)).zoom(camera.zoom).bearing(camera.bearing).tilt(camera.tilt).build()
            if(!listenersInstalled){
                listenersInstalled=true
                m.addOnCameraIdleListener{val c=m.cameraPosition;latestCameraChanged(MapCameraState(c.target?.latitude?:camera.lat,c.target?.longitude?:camera.lon,c.zoom,c.bearing,c.tilt))}
                m.addOnMapClickListener{latLng->val p=m.projection.toScreenLocation(latLng);val f=m.queryRenderedFeatures(p,ITEMS);val id=f.firstOrNull()?.getStringProperty("id");if(id!=null){latestItemClick(id);true}else false}
            }
            applyStyle(m)
        } else {
            map?.let(::applyStyle)
            if(styleLoaded)map?.style?.let{updateSource(it,items,events)}
        }
    })
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
    if(style.getLayer(CLUSTERS)==null)style.addLayer(CircleLayer(CLUSTERS,SOURCE).withFilter(has("point_count")).withProperties(circleColor("#B92D403A"),circleRadius(interpolate(linear(),get("point_count"),stop(2,19f),stop(20,27f),stop(100,34f))),circleStrokeColor("#D9E9F4EE"),circleStrokeWidth(1.2f),circleBlur(.08f)))
    if(style.getLayer(COUNT)==null)style.addLayer(SymbolLayer(COUNT,SOURCE).withFilter(has("point_count")).withProperties(textField(toString(get("point_count"))),textSize(12f),textColor(Color.WHITE),textAllowOverlap(true)))
    if(style.getLayer(BACKDROP)==null)style.addLayer(CircleLayer(BACKDROP,SOURCE).withFilter(not(has("point_count"))).withProperties(circleColor(get("markerColor")),circleRadius(27f),circleStrokeColor("#CFEAF2EC"),circleStrokeWidth(1.25f),circleBlur(.05f)))
    if(style.getLayer(ITEMS)==null)style.addLayer(SymbolLayer(ITEMS,SOURCE).withFilter(not(has("point_count"))).withProperties(iconImage(get("icon")),iconSize(.76f),iconAllowOverlap(true),iconIgnorePlacement(false),iconPadding(6f)))
    if(style.getLayer(SCORES)==null)style.addLayer(SymbolLayer(SCORES,SOURCE).withFilter(not(has("point_count"))).withProperties(textField(toString(get("score"))),textSize(10f),textColor("#FF15201D"),textHaloColor("#F2FFFFFF"),textHaloWidth(5f),textOffset(arrayOf(2.0f,-2.0f)),textAllowOverlap(true),textIgnorePlacement(true)))
}
private fun updateSource(style:Style,items:List<GeoItem>,events:List<EventItem>){
    val features=ArrayList<Feature>(items.size+events.size)
    items.forEach{x->val lat=x.lat?:return@forEach;val lon=x.lon?:return@forEach;val p=JsonObject().apply{addProperty("id",x.id);addProperty("name",x.name);addProperty("icon",MapMarkerRegistry.icon(x));addProperty("score",x.score.toInt());addProperty("markerColor","#A83B5D50")};features+=Feature.fromGeometry(Point.fromLngLat(lon,lat),p)}
    events.forEach{x->val p=JsonObject().apply{addProperty("id",x.id);addProperty("name",x.title);addProperty("icon",MapMarkerRegistry.icon(x));addProperty("score",if(x.isFree)0 else x.priceMin?.toInt()?:0);addProperty("free",x.isFree);addProperty("markerColor","#A864394A")};features+=Feature.fromGeometry(Point.fromLngLat(x.lon,x.lat),p)}
    (style.getSource(SOURCE) as? GeoJsonSource)?.setGeoJson(FeatureCollection.fromFeatures(features))
}
