package ru.navigatordosuga.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.navigatordosuga.app.model.Profile
import ru.navigatordosuga.app.ui.components.GlassSurface

@Composable
fun ProfileManagerScreen(profiles:List<Profile>,activeId:String?,onSelect:(String)->Unit,onAdd:()->Unit,onClose:()->Unit){
    Box(Modifier.fillMaxSize().background(Color(0xFF0B1112))){
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(14.dp)){
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){IconButton(onClick=onClose){Icon(Icons.Rounded.ArrowBack,"Назад",tint=Color.White)};Text("Профили",Modifier.weight(1f),color=Color.White,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge);IconButton(onClick=onAdd){Icon(Icons.Rounded.PersonAdd,"Новый профиль",tint=Color.White)}}
            Text("Сохранённые места, поездки и рекорды разделяются между локальными профилями.",color=Color.White.copy(.62f),style=MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(10.dp))
            LazyColumn{items(profiles,key={it.id}){p->GlassSurface(Modifier.fillMaxWidth().padding(vertical=5.dp).clickable{onSelect(p.id)},alpha=if(p.id==activeId).78f else .61f,radius=22){Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(44.dp).background(Color.White.copy(.14f),CircleShape),contentAlignment=Alignment.Center){Text(p.displayName.take(1).uppercase(),color=Color.White,fontWeight=FontWeight.Bold)};Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(p.displayName,color=Color.White,fontWeight=FontWeight.SemiBold);Text("${p.transportPreference} · до ${p.maxTripDistanceKm} км",color=Color.White.copy(.58f),style=MaterialTheme.typography.bodySmall)};if(p.id==activeId)Icon(Icons.Rounded.CheckCircle,"Активный",tint=Color(0xFF8FE6B6))}}}}
            Spacer(Modifier.height(10.dp));Button(onClick=onAdd,Modifier.fillMaxWidth()){Icon(Icons.Rounded.PersonAdd,null);Spacer(Modifier.width(8.dp));Text("Новый профиль")}
        }
    }
}
