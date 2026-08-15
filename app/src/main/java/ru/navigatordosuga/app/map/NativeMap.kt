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
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.gson.JsonObject
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
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
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tan
import kotlin.math.PI
import kotlin.math.roundToInt

private const val SOURCE="opr-content-source"

@Composable
fun MapMarkerOverlay(items:List<GeoItem>,events:List<EventItem>,camera:MapCameraState,selectedId:String?,userLocation:ru.navigatordosuga.app.model.UserLocationState?,modifier:Modifier=Modifier){
    val context=LocalContext.current
    val icons=remember(items,events){buildMap<String,Bitmap>{
        items.forEach{put(it.id,BitmapFactory.decodeResource(context.resources,MapMarkerRegistry.resource(it)))}
        events.forEach{put(it.id,BitmapFactory.decodeResource(context.resources,MapMarkerRegistry.resource(it)))}
    }}
    Canvas(modifier){
        val world=256.0*2.0.pow(camera.zoom)
        fun mercator(lat:Double,lon:Double):Pair<Double,Double>{
            val clamped=lat.coerceIn(-85.05112878,85.05112878)
            val x=(lon+180.0)/360.0*world
            val rad=Math.toRadians(clamped)
            val y=(1.0-ln(tan(PI/4.0+rad/2.0))/PI)/2.0*world
            return x to y
        }
        val (cx,cy)=mercator(camera.lat,camera.lon)
        val angle=Math.toRadians(-camera.bearing);val ca=cos(angle);val sa=sin(angle)
        data class P(val id:String,val lat:Double,val lon:Double,val event:Boolean,val score:Double)
        data class S(val p:P,val x:Float,val y:Float)
        val points=buildList<P>{
            items.forEach{x->x.lat?.let{lat->x.lon?.let{lon->add(P(x.id,lat,lon,false,x.score))}}}
            events.forEach{add(P(it.id,it.lat,it.lon,true,if(it.isFree)90.0 else 70.0))}
        }
        val screen=points.mapNotNull{p->
            val (px,py)=mercator(p.lat,p.lon);var dx=px-cx
            if(dx>world/2)dx-=world else if(dx < -world/2)dx+=world
            val dy=py-cy
            val sx=size.width/2f+(dx*ca-dy*sa).toFloat()
            val sy=size.height/2f+(dx*sa+dy*ca).toFloat()
            if(sx in -40f..size.width+40f&&sy in -40f..size.height+40f)S(p,sx,sy) else null
        }
        val cell=when{camera.zoom<10.5->150f;camera.zoom<12.2->96f;else->58f}
        val groups=screen.groupBy{"${(it.x/cell).toInt()}:${(it.y/cell).toInt()}"}
        val labelPaint=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.rgb(24,43,36);textAlign=Paint.Align.CENTER;textSize=25f;typeface=android.graphics.Typeface.DEFAULT_BOLD}
        val iconPaint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val visibleGroups=groups.values.sortedWith(compareByDescending<List<S>>{g->g.any{it.p.id==selectedId}}.thenByDescending{g->g.maxOf{it.p.score}})
            .take(when{camera.zoom<10.5->14;camera.zoom<12.2->26;else->80})
        visibleGroups.forEach{group->
            val selected=group.firstOrNull{it.p.id==selectedId}
            if(group.size>1&&camera.zoom<12.2&&selected==null){
                val sx=group.map{it.x}.average().toFloat();val sy=group.map{it.y}.average().toFloat();val o=androidx.compose.ui.geometry.Offset(sx,sy)
                drawCircle(ComposeColor.White.copy(alpha=.96f),24f,o);drawCircle(ComposeColor(0xFFBFEBDC),19f,o)
                drawContext.canvas.nativeCanvas.drawText(group.size.toString(),sx,sy+8f,labelPaint)
            }else{
                val candidates=(selected?.let{listOf(it)}?:group.sortedByDescending{it.p.score}.take(1))
                candidates.forEach{s->
                    val chosen=s.p.id==selectedId;val o=androidx.compose.ui.geometry.Offset(s.x,s.y)
                    if(chosen)drawCircle(ComposeColor(0xFF35D7A2).copy(alpha=.24f),27f,o)
                    val radius=if(chosen)27f else 22f
                    drawCircle(ComposeColor.White.copy(alpha=.94f),radius+2f,o)
                    icons[s.p.id]?.let{bitmap->drawContext.canvas.nativeCanvas.drawBitmap(bitmap,null,RectF(s.x-radius,s.y-radius,s.x+radius,s.y+radius),iconPaint)}
                }
            }
        }
        userLocation?.let{u->
            val (px,py)=mercator(u.lat,u.lon);var dx=px-cx;if(dx>world/2)dx-=world else if(dx < -world/2)dx+=world
            val dy=py-cy;val sx=size.width/2f+(dx*ca-dy*sa).toFloat();val sy=size.height/2f+(dx*sa+dy*ca).toFloat();val o=androidx.compose.ui.geometry.Offset(sx,sy)
            val metersPerPixel=(cos(Math.toRadians(u.lat))*2*PI*6378137/world).coerceAtLeast(.1)
            drawCircle(ComposeColor(0xFF2F80ED).copy(alpha=.15f),(u.accuracyMeters/metersPerPixel).toFloat().coerceIn(18f,90f),o)
            drawCircle(ComposeColor.White,13f,o);drawCircle(ComposeColor(0xFF2F80ED),9f,o)
        }
    }
}

@Composable
fun NativeMap(
    items:List<GeoItem>, events:List<EventItem>, camera:MapCameraState, modifier:Modifier=Modifier,
    dark:Boolean=false,
    onCameraChanged:(MapCameraState)->Unit={}, onItemClick:(String)->Unit={}
){
    val context=LocalContext.current; val owner=LocalLifecycleOwner.current
    val mapView=remember{
        MapView(
            context,
            MapLibreMapOptions.createFromAttributes(context,null).textureMode(true)
        ).apply{onCreate(Bundle())}
    }
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
            updateAnnotations(m,markerEntries,context)
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
        if(styleLoaded)map?.let{m->m.style?.let{updateSource(it,items,events)};updateAnnotations(m,markerEntries,context)}
    }
    Box(modifier){
        AndroidView(factory={mapView},modifier=Modifier.fillMaxSize(),update={view->
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
    }
}

private fun updateAnnotations(map:MapLibreMap,entries:List<MapMarkerEntry>,context:android.content.Context){
    map.removeAnnotations()
    val icons=IconFactory.getInstance(context)
    map.addMarkers(entries.map{entry->MarkerOptions().position(LatLng(entry.lat,entry.lon)).icon(icons.fromBitmap(entry.bitmap)).title(entry.id)})
    Log.i("NativeMap","MARKER_ANNOTATIONS_UPDATE count=${entries.size}")
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
    if(style.getLayer("opr-content-presence")==null){
        style.addLayer(CircleLayer("opr-content-presence",SOURCE).withProperties(
            circleColor("#35D7A2"),circleRadius(18f),circleStrokeColor("#F7FFFC"),circleStrokeWidth(3f)
        ))
    }
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
