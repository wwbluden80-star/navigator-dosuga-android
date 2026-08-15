package ru.navigatordosuga.app.ui.profile

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.navigatordosuga.app.ui.components.GlassSurface

@Composable
fun ProfileSetup(onDone:(String,String?,Set<String>,String,Int)->Unit,onSkip:()->Unit){
    val context=LocalContext.current
    var step by remember{mutableIntStateOf(0)}
    var name by remember{mutableStateOf("")}
    var avatar by remember{mutableStateOf<String?>(null)}
    var interests by remember{mutableStateOf(setOf<String>())}
    var transport by remember{mutableStateOf("car")}
    var distance by remember{mutableFloatStateOf(120f)}
    val avatarPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null){
            runCatching{context.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)}
            avatar=uri.toString()
        }
    }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0A1517),Color(0xFF183B35),Color(0xFF0B1112))))){
        GlassSurface(Modifier.align(Alignment.Center).padding(18.dp).fillMaxWidth(),alpha=.70f,radius=32){
            Column(Modifier.fillMaxWidth().padding(22.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(16.dp)){
                Text("Навигатор досуга",color=Color.White.copy(.68f),style=MaterialTheme.typography.labelLarge)
                Text(when(step){0->"Давайте познакомимся";1->"Что вам интересно?";else->"Как обычно путешествуете?"},color=Color.White,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.headlineSmall)
                when(step){
                    0->Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
                        OutlinedTextField(name,{name=it.take(40)},Modifier.fillMaxWidth(),singleLine=true,label={Text("Имя")},leadingIcon={Icon(Icons.Rounded.Person,null)})
                        OutlinedButton(onClick={avatarPicker.launch(arrayOf("image/*"))},shape=RoundedCornerShape(18.dp)){Icon(if(avatar==null)Icons.Rounded.AddAPhoto else Icons.Rounded.CheckCircle,null);Spacer(Modifier.width(8.dp));Text(if(avatar==null)"Добавить фото" else "Фото выбрано")}
                        Text("Фото хранится локально; приложение сохраняет постоянное право чтения только выбранного файла.",color=Color.White.copy(.55f),style=MaterialTheme.typography.bodySmall)
                    }
                    1->Column(verticalArrangement=Arrangement.spacedBy(8.dp)){listOf("mushrooms" to "Грибы","fishing" to "Рыбалка","beautiful" to "Природа и места","cinema" to "Кино","history" to "История","events" to "Мероприятия").forEach{(k,t)->FilterChip(selected=k in interests,onClick={interests=if(k in interests)interests-k else interests+k},label={Text(t)})}}
                    else->Column(verticalArrangement=Arrangement.spacedBy(12.dp)){Row(Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("car" to "Автомобиль","walk" to "Пешком","mixed" to "Смешанно").forEach{(k,t)->FilterChip(selected=transport==k,onClick={transport=k},label={Text(t)})}};Text("Готовы ехать: ${distance.toInt()} км",color=Color.White);Slider(distance,{distance=it},valueRange=30f..250f,steps=10)}
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){TextButton(onClick=onSkip){Text("Пока без настройки")};Button(onClick={if(step<2)step++ else onDone(name,avatar,interests,transport,distance.toInt())},shape=RoundedCornerShape(18.dp)){Text(if(step<2)"Далее" else "Открыть карту")}}
                LinearProgressIndicator(progress={(step+1)/3f},modifier=Modifier.fillMaxWidth())
                Text("Профиль хранится на этом устройстве. Облачная учётная запись не требуется.",color=Color.White.copy(.58f),style=MaterialTheme.typography.bodySmall)
            }
        }
    }
}
