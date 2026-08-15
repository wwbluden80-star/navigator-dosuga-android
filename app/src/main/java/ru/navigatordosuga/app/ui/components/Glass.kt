package ru.navigatordosuga.app.ui.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ru.navigatordosuga.app.sensors.GlassMotion
import ru.navigatordosuga.app.sensors.LiveGlassController

val LocalGlassMotion=compositionLocalOf{GlassMotion()}

@Composable
fun LiveGlassHost(context:Context,enabled:Boolean,content: @Composable () -> Unit){
    val controller=remember(context){LiveGlassController(context)}
    val motion by controller.motion.collectAsState()
    DisposableEffect(enabled){if(enabled)controller.start() else controller.stop();onDispose{controller.stop()}}
    CompositionLocalProvider(LocalGlassMotion provides if(enabled)motion else GlassMotion(),content=content)
}

@Composable fun GlassSurface(
    modifier:Modifier=Modifier,
    alpha:Float=.74f,
    radius:Int=24,
    content: @Composable BoxScope.() -> Unit
){
    val m=LocalGlassMotion.current
    val shape=RoundedCornerShape(radius.dp)
    val glow=Brush.linearGradient(
        colors=listOf(Color.White.copy(.12f),Color.Transparent,Color.White.copy(.025f)),
        start=Offset((.2f+m.x*.15f)*1000f,(.05f+m.y*.12f)*1000f),
        end=Offset((.85f+m.x*.08f)*1000f,(.9f+m.y*.08f)*1000f)
    )
    Box(modifier.shadow(14.dp,shape,ambientColor=Color.Black.copy(.16f),spotColor=Color.Black.copy(.18f))
        .background(Color(0xFF172321).copy(alpha),shape)
        .background(glow,shape)
        .border(BorderStroke(.6.dp,Color.White.copy(.26f+m.x*.025f)),shape)
        .padding(2.dp),content=content)
}

@Composable fun GlassButton(modifier:Modifier=Modifier,onClick:()->Unit,content: @Composable BoxScope.() -> Unit){
    val m=LocalGlassMotion.current
    val shape=RoundedCornerShape(20.dp)
    val glow=Brush.linearGradient(listOf(Color.White.copy(.11f),Color.Transparent),start=Offset((.2f+m.x*.18f)*600f,0f),end=Offset((.9f+m.x*.10f)*600f,600f))
    Box(modifier.clickable(onClick=onClick).background(Color(0xFF182724).copy(.68f),shape).background(glow,shape)
        .border(.55.dp,Color.White.copy(.28f),shape).padding(horizontal=13.dp,vertical=10.dp),content=content)
}
