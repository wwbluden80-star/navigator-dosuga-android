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
import ru.navigatordosuga.app.BuildConfig
import ru.navigatordosuga.app.model.EventItem
import ru.navigatordosuga.app.model.GeoItem
import ru.navigatordosuga.app.model.MapCameraState

private const val SOURCE="opr-content-source"
private const val ITEMS="opr-content-items"
private const val CLUSTERS="opr-content-clusters"
private const val COUNT="opr-content-count"

@Composable
fun NativeMap(
    items:List<GeoItem>, events:List<EventItem>, camera:MapCameraState, modifier:Modifier=Modifier,
    onCameraChanged:(MapCameraState)->Unit={}, onItemClick:(String)->Unit={}
){
    val context=LocalContext.current; val owner=LocalLifecycleOwner.current
    val mapView=remember{MapView(context).apply{onCreate(Bundle())}}
    var map by remember{mutableStateOf<MapLibreMap?>(null)}
    var styleLoaded by remember{mutableStateOf(false)}
    DisposableEffect(owner,mapView){
        val obs=LifecycleEventObserver{_,e->when(e){Lifecycle.Event.ON_START->mapView.onStart();Lifecycle.Event.ON_RESUME->mapView.onResume();Lifecycle.Event.ON_PAUSE->mapView.onPause();Lifecycle.Event.ON_STOP->mapView.onStop();Lifecycle.Event.ON_DESTROY->mapView.onDestroy();else->{}}};owner.lifecycle.addObserver(obs);onDispose{owner.lifecycle.removeObserver(obs);mapView.onStop();mapView.onDestroy()}}
    AndroidView(factory={mapView},modifier=modifier,update={ view ->
        if(map==null)view.getMapAsync{m->map=m;m.cameraPosition=CameraPosition.Builder().target(LatLng(camera.lat,camera.lon)).zoom(camera.zoom).bearing(camera.bearing).tilt(camera.tilt).build();m.setStyle(Style.Builder().fromUri(BuildConfig.MAP_STYLE_URL)){style->MapMarkerRegistry.install(context,style);installLayers(style);styleLoaded=true;updateSource(style,items,events);m.addOnCameraIdleListener{val c=m.cameraPosition;onCameraChanged(MapCameraState(c.target?.latitude?:camera.lat,c.target?.longitude?:camera.lon,c.zoom,c.bearing,c.tilt))};m.addOnMapClickListener{latLng->val p=m.projection.toScreenLocation(latLng);val f=m.queryRenderedFeatures(p,ITEMS);val id=f.firstOrNull()?.getStringProperty("id");if(id!=null){onItemClick(id);true}else false}}}
        else if(styleLoaded)map?.style?.let{updateSource(it,items,events)}
    })
}

private fun installLayers(style:Style){
    if(style.getSource(SOURCE)==null)style.addSource(GeoJsonSource(SOURCE,FeatureCollection.fromFeatures(arrayOf()),GeoJsonOptions().withCluster(true).withClusterRadius(52).withClusterMaxZoom(13)))
    if(style.getLayer(CLUSTERS)==null)style.addLayer(CircleLayer(CLUSTERS,SOURCE).withFilter(has("point_count")).withProperties(circleColor("#783DD9B3"),circleRadius(interpolate(linear(),get("point_count"),stop(2,17f),stop(20,24f),stop(100,31f))),circleStrokeColor("#BFFFFFFF"),circleStrokeWidth(1f)))
    if(style.getLayer(COUNT)==null)style.addLayer(SymbolLayer(COUNT,SOURCE).withFilter(has("point_count")).withProperties(textField(toString(get("point_count"))),textSize(12f),textColor(Color.WHITE),textAllowOverlap(true)))
    if(style.getLayer(ITEMS)==null)style.addLayer(SymbolLayer(ITEMS,SOURCE).withFilter(not(has("point_count"))).withProperties(iconImage(get("icon")),iconSize(.72f),iconAllowOverlap(true),iconIgnorePlacement(false),iconPadding(5f)))
}
private fun updateSource(style:Style,items:List<GeoItem>,events:List<EventItem>){
    val features=ArrayList<Feature>(items.size+events.size)
    items.forEach{x->val lat=x.lat?:return@forEach;val lon=x.lon?:return@forEach;val p=JsonObject().apply{addProperty("id",x.id);addProperty("name",x.name);addProperty("icon",MapMarkerRegistry.icon(x));addProperty("score",x.score)};features+=Feature.fromGeometry(Point.fromLngLat(lon,lat),p)}
    events.forEach{x->val p=JsonObject().apply{addProperty("id",x.id);addProperty("name",x.title);addProperty("icon",MapMarkerRegistry.icon(x));addProperty("free",x.isFree)};features+=Feature.fromGeometry(Point.fromLngLat(x.lon,x.lat),p)}
    (style.getSource(SOURCE) as? GeoJsonSource)?.setGeoJson(FeatureCollection.fromFeatures(features))
}
