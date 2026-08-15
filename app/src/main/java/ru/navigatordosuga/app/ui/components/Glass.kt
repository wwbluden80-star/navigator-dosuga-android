package ru.navigatordosuga.app.ui.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import ru.navigatordosuga.app.sensors.GlassMotion
import ru.navigatordosuga.app.sensors.LiveGlassController

val LocalGlassMotion=compositionLocalOf{GlassMotion()}
val LocalGlassEnabled=compositionLocalOf{false}

@Composable
fun LiveGlassHost(context:Context,enabled:Boolean,content: @Composable () -> Unit){
    val controller=remember(context){LiveGlassController(context)}
    val motion by controller.motion.collectAsState()
    DisposableEffect(enabled){if(enabled)controller.start() else controller.stop();onDispose{controller.stop()}}
    CompositionLocalProvider(LocalGlassMotion provides if(enabled)motion else GlassMotion(),LocalGlassEnabled provides enabled,content=content)
}

@Composable fun GlassSurface(
    modifier:Modifier=Modifier,
    alpha:Float=.74f,
    radius:Int=24,
    content: @Composable BoxScope.() -> Unit
){
    val m=LocalGlassMotion.current
    val live=LocalGlassEnabled.current
    val dark=MaterialTheme.colorScheme.background.luminance()<.45f
    val shape=RoundedCornerShape(radius.dp)
    val glow=Brush.linearGradient(
        colors=listOf(Color.White.copy(if(live).16f else .09f),Color.Transparent,Color.White.copy(if(live).055f else .02f)),
        start=Offset((.2f+m.x*.15f)*1000f,(.05f+m.y*.12f)*1000f),
        end=Offset((.85f+m.x*.08f)*1000f,(.9f+m.y*.08f)*1000f)
    )
    val base=if(dark)Color(0xFF17221F) else Color(0xFFEAF3E8)
    val effectiveAlpha=if(live)(alpha-.07f).coerceAtLeast(.56f) else alpha.coerceAtLeast(.78f)
    val border=if(dark)Color.White.copy(if(live).28f else .20f) else Color(0xFF577167).copy(if(live).28f else .20f)
    Box(modifier.shadow(14.dp,shape,ambientColor=Color.Black.copy(.16f),spotColor=Color.Black.copy(.18f))
        .background(base.copy(effectiveAlpha),shape)
        .background(glow,shape)
        .border(BorderStroke(.7.dp,border.copy(alpha=(border.alpha+m.x*.025f).coerceIn(.12f,.36f))),shape)
        .padding(2.dp),content=content)
}

@Composable fun GlassButton(modifier:Modifier=Modifier,onClick:()->Unit,content: @Composable BoxScope.() -> Unit){
    val m=LocalGlassMotion.current
    val live=LocalGlassEnabled.current
    val dark=MaterialTheme.colorScheme.background.luminance()<.45f
    val shape=RoundedCornerShape(20.dp)
    val glow=Brush.linearGradient(listOf(Color.White.copy(if(live).15f else .08f),Color.Transparent),start=Offset((.2f+m.x*.18f)*600f,0f),end=Offset((.9f+m.x*.10f)*600f,600f))
    val base=if(dark)Color(0xFF17221F) else Color(0xFFEAF3E8)
    val foreground=if(dark)Color.White.copy(.24f) else Color(0xFF577167).copy(.24f)
    Box(modifier.defaultMinSize(minWidth=54.dp,minHeight=54.dp).clickable(onClick=onClick).background(base.copy(if(live).70f else .82f),shape).background(glow,shape)
        .border(.65.dp,foreground,shape).padding(horizontal=13.dp,vertical=10.dp),contentAlignment=Alignment.Center,content=content)
}
