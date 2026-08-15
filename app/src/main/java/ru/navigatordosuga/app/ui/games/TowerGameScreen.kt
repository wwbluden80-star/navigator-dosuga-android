package ru.navigatordosuga.app.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive
import ru.navigatordosuga.app.AppContainer
import ru.navigatordosuga.app.game.tower.*
import ru.navigatordosuga.app.ui.components.GlassButton
import ru.navigatordosuga.app.ui.components.GlassSurface
import java.time.Instant
import kotlin.math.*

@Composable fun TowerGameScreen(c:AppContainer,onClose:()->Unit){
    val profileId by c.profiles.activeId.collectAsState(initial="guest")
    val repo=remember{TowerGameRepository(c.db)};var sim by remember{mutableStateOf(TowerSimulation())};var snap by remember{mutableStateOf(sim.snapshot())};var paused by remember{mutableStateOf(false)};var saved by remember{mutableStateOf(false)};var started by remember{mutableStateOf(Instant.now())};var elapsed by remember{mutableIntStateOf(0)}
    LaunchedEffect(sim,paused){var last=0L;while(isActive){withFrameNanos{now->if(last!=0L&&!paused){val dt=((now-last)/1e9).coerceAtMost(.05);if(sim.state==TowerState.COLLAPSING)sim.tickCollapse(dt)else sim.step(dt);snap=sim.snapshot();elapsed=(Instant.now().epochSecond-started.epochSecond).toInt()};last=now}}}
    LaunchedEffect(snap.state){if(snap.state==TowerState.GAME_OVER&&!saved){saved=true;repo.save(profileId?:"guest",snap.score,started,elapsed)}}
    Box(Modifier.fillMaxSize().background(Color(0xFF8FC7E8))){
        TowerCanvas(snap,onTap={if(!paused)sim.release()})
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(14.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
            Hud("Этажи",snap.score.floors.toString());Hud("Очки",snap.score.score.toString());GlassButton(Modifier.size(54.dp),onClick={paused=!paused;if(paused)sim.pause() else sim.resume()}){Icon(if(paused)Icons.Rounded.PlayArrow else Icons.Rounded.Pause,"Пауза",tint=Color.White)};GlassButton(Modifier.size(54.dp),onClick=onClose){Icon(Icons.Rounded.Close,"Выйти",tint=Color.White)}
        }
        snap.message?.let{Text(it,Modifier.align(Alignment.Center).background(Color.Black.copy(.42f),androidx.compose.foundation.shape.RoundedCornerShape(18.dp)).padding(horizontal=18.dp,vertical=10.dp),color=Color.White,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge)}
        if(snap.state==TowerState.GAME_OVER){
            Box(Modifier.fillMaxSize().background(Color(0x66405255)))
            GlassSurface(Modifier.align(Alignment.Center).fillMaxWidth().padding(24.dp),alpha=.95f,radius=32){
                Column(Modifier.padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(16.dp)){
                    Text("СТРОЙКА ОКОНЧЕНА",color=Color.White.copy(.62f),style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.Bold)
                    Text("Башня рухнула",color=Color.White,style=MaterialTheme.typography.displaySmall,fontWeight=FontWeight.Bold)
                    Row(verticalAlignment=Alignment.Bottom){Text(snap.score.floors.toString(),color=Color.White,style=MaterialTheme.typography.displayLarge,fontWeight=FontWeight.Bold);Spacer(Modifier.width(8.dp));Text("этажей",color=Color.White.copy(.64f),style=MaterialTheme.typography.headlineSmall)}
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
                        ResultStat(Modifier.weight(1f),snap.score.score.toString(),"очков")
                        ResultStat(Modifier.weight(1f),"×${maxOf(1,snap.score.bestCombo)}","лучшая серия")
                    }
                    GlassButton(Modifier.fillMaxWidth().height(68.dp).background(Brush.horizontalGradient(listOf(Color(0xFFFFD77C),Color(0xFFFFB548))),androidx.compose.foundation.shape.RoundedCornerShape(24.dp)),onClick={sim=TowerSimulation(seed=(System.nanoTime()%Int.MAX_VALUE).toInt());snap=sim.snapshot();saved=false;started=Instant.now();elapsed=0}){Text("Ещё раз",color=Color(0xFF172020),style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)}
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
                        GlassButton(Modifier.weight(1f),onClick={}){Text("Турнир",color=Color.White)}
                        GlassButton(Modifier.weight(1f),onClick={}){Text("Поделиться",color=Color.White)}
                        GlassButton(Modifier.weight(1f),onClick=onClose){Text("Выйти",color=Color.White)}
                    }
                }
            }
        }
        if(snap.state==TowerState.SWINGING)Text("Нажмите, чтобы отпустить этаж",Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom=28.dp).background(Color.Black.copy(.36f),androidx.compose.foundation.shape.RoundedCornerShape(18.dp)).padding(12.dp),color=Color.White)
    }
}

@Composable private fun ResultStat(modifier:Modifier,value:String,label:String){GlassSurface(modifier,alpha=.72f,radius=20){Column(Modifier.fillMaxWidth().padding(14.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(value,color=Color.White,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Text(label,color=Color.White.copy(.62f),style=MaterialTheme.typography.bodyMedium)}}}

@Composable private fun Hud(label:String,value:String){Column(horizontalAlignment=Alignment.CenterHorizontally){Text(label,color=Color.White.copy(.72f),style=MaterialTheme.typography.labelSmall);Text(value,color=Color.White,fontWeight=FontWeight.Bold)}}

@Composable private fun TowerCanvas(s:TowerSnapshot,onTap:()->Unit){Canvas(Modifier.fillMaxSize().pointerInput(Unit){detectTapGestures{onTap()}}){
    val sx=size.width/390f
    val sy=size.height/844f
    fun px(x:Double)=x.toFloat()*sx
    fun py(y:Double)=(y+s.cameraY).toFloat()*sy
    drawRect(Brush.verticalGradient(listOf(Color(0xFF79B8DD),Color(0xFFC9D9DD),Color(0xFF85969D))),size=size)
    // Moscow skyline / layered city
    val horizon=size.height*.70f;for(i in 0..18){val w=size.width/18f+4;val h=(28+(i*17%95)).toFloat();drawRect(Color(0xFF5C6870).copy(.46f),Offset(i*w,horizon-h),Size(w-2,h))};drawRect(Color(0xFF45525A).copy(.25f),Offset(0f,horizon),Size(size.width,size.height-horizon))
    // distant City towers and Ostankino cue
    drawRoundRect(Color(0xFF7A8D96).copy(.52f),Offset(size.width*.10f,horizon-170),Size(34f,170f),CornerRadius(8f,8f));drawRoundRect(Color(0xFF6F818B).copy(.5f),Offset(size.width*.18f,horizon-130),Size(29f,130f),CornerRadius(8f,8f));drawRect(Color(0xFF667780).copy(.50f),Offset(size.width*.83f,horizon-200),Size(7f,200f));drawCircle(Color(0xFF667780).copy(.5f),18f,Offset(size.width*.835f,horizon-135))
    // crane carriage, cable lines inferred from current block
    s.current?.let{b->val bx=px(b.x);val by=py(b.y);drawRect(Color(0xFF394147),Offset(0f,44f),Size(size.width,8f));drawRect(Color(0xFFFFC43D),Offset(bx-18,36f),Size(36f,18f));drawLine(Color(0xFF353B3F),Offset(bx-16,54f),Offset(px(b.x-b.w*.31),by-b.h.toFloat()*sy/2),2f);drawLine(Color(0xFF353B3F),Offset(bx+16,54f),Offset(px(b.x+b.w*.31),by-b.h.toFloat()*sy/2),2f)}
    fun block(b:TowerBody,index:Int){val x=px(b.x);val y=py(b.y);val w=b.w.toFloat()*sx;val h=b.h.toFloat()*sy;rotate(Math.toDegrees(b.angle).toFloat(),Offset(x,y)){val tone=if(index%4==0)Color(0xFFD8D1C5) else if(index%4==1)Color(0xFFC7CED0) else if(index%4==2)Color(0xFFD1C2AF) else Color(0xFFB9C6CC);drawRoundRect(tone,Offset(x-w/2,y-h/2),Size(w,h),CornerRadius(6f,6f));drawRect(Color.Black.copy(.09f),Offset(x+w*.43f,y-h/2),Size(w*.07f,h));val cols=4;for(c in 0 until cols){val ww=w*.13f;val wx=x-w*.36f+c*w*.22f;drawRoundRect(Color(0xFF5B8FA8).copy(.84f),Offset(wx,y-h*.23f),Size(ww,h*.34f),CornerRadius(4f,4f));drawRect(Color.White.copy(.35f),Offset(wx+ww*.12f,y-h*.20f),Size(ww*.18f,h*.28f))};if(index%3==0)drawRect(Color(0xFF6B777B).copy(.45f),Offset(x-w*.16f,y+h*.06f),Size(w*.32f,h*.23f))}}
    s.placed.forEachIndexed{index,b->block(b,index)};s.current?.let{block(it,s.placed.size)}
    if(s.helicopter){val hx=size.width*.76f;val hy=size.height*.24f;drawOval(Color(0xFF37454C).copy(.75f),Offset(hx,hy),Size(48f,16f));drawLine(Color(0xFF37454C).copy(.7f),Offset(hx-15,hy-5),Offset(hx+55,hy-5),3f)}
}}
