package ru.navigatordosuga.app.ui.guides

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.navigatordosuga.app.data.repository.*
import ru.navigatordosuga.app.ui.components.GlassSurface

private enum class GuideTab{MUSHROOMS,FISH}

@Composable
fun GuideScreen(repo:GuideRepository,onClose:()->Unit){
    var tab by remember{mutableStateOf(GuideTab.MUSHROOMS)}
    var query by remember{mutableStateOf("")}
    var mushroom by remember{mutableStateOf<MushroomGuideItem?>(null)}
    var fish by remember{mutableStateOf<FishGuideItem?>(null)}
    Box(Modifier.fillMaxSize().background(Color(0xFF0B1112))){
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(12.dp)){
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                IconButton(onClick=onClose){Icon(Icons.Rounded.ArrowBack,"Назад",tint=Color.White)}
                Text("Справочники",Modifier.weight(1f),color=Color.White,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge)
            }
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                FilterChip(selected=tab==GuideTab.MUSHROOMS,onClick={tab=GuideTab.MUSHROOMS;mushroom=null;fish=null},label={Text("Грибы")},leadingIcon={Icon(Icons.Rounded.Forest,null)})
                FilterChip(selected=tab==GuideTab.FISH,onClick={tab=GuideTab.FISH;mushroom=null;fish=null},label={Text("Рыбы")},leadingIcon={Icon(Icons.Rounded.Water,null)})
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(query,{query=it},Modifier.fillMaxWidth(),singleLine=true,placeholder={Text(if(tab==GuideTab.MUSHROOMS)"Белый, лисичка, Amanita…" else "Щука, лещ, окунь…")},leadingIcon={Icon(Icons.Rounded.Search,null)})
            Spacer(Modifier.height(8.dp))
            if(tab==GuideTab.MUSHROOMS){
                Text("⚠ Не определяйте съедобность только по фото. При сомнении гриб не брать.",color=Color(0xFFFFD58A),style=MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
            }
            when{
                mushroom!=null->MushroomDetail(mushroom!!){mushroom=null}
                fish!=null->FishDetail(fish!!){fish=null}
                tab==GuideTab.MUSHROOMS->MushroomList(repo.mushrooms,query){mushroom=it}
                else->FishList(repo.fish,query){fish=it}
            }
        }
    }
}

@Composable private fun MushroomList(all:List<MushroomGuideItem>,q:String,onOpen:(MushroomGuideItem)->Unit){
    val rows=remember(all,q){val n=q.trim().lowercase();all.filter{n.isBlank()||(it.name+" "+it.scientificName+" "+it.group+" "+it.edibleStatus+" "+it.features.joinToString(" ")).lowercase().contains(n)}}
    LazyColumn(Modifier.fillMaxSize()){items(rows,key={it.id}){x->GuideRow(x.name,x.scientificName,"${x.group} · ${x.edibleStatus}",Icons.Rounded.Forest){onOpen(x)}}}
}
@Composable private fun FishList(all:List<FishGuideItem>,q:String,onOpen:(FishGuideItem)->Unit){
    val rows=remember(all,q){val n=q.trim().lowercase();all.filter{n.isBlank()||(it.name+" "+it.scientificName+" "+it.methods.joinToString(" ")).lowercase().contains(n)}}
    LazyColumn(Modifier.fillMaxSize()){items(rows,key={it.id}){x->GuideRow(x.name,x.scientificName,legalLabel(x.legalStatus),Icons.Rounded.Water){onOpen(x)}}}
}
@Composable private fun GuideRow(title:String,latin:String,sub:String,icon:androidx.compose.ui.graphics.vector.ImageVector,onClick:()->Unit){
    GlassSurface(Modifier.fillMaxWidth().padding(vertical=4.dp).clickable(onClick=onClick),alpha=.64f,radius=20){
        Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){Icon(icon,null,tint=Color.White);Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(title,color=Color.White,fontWeight=FontWeight.SemiBold);Text(latin,color=Color.White.copy(.58f),style=MaterialTheme.typography.bodySmall);Text(sub,color=Color.White.copy(.72f),style=MaterialTheme.typography.labelSmall,maxLines=1,overflow=TextOverflow.Ellipsis)};Icon(Icons.Rounded.ChevronRight,null,tint=Color.White.copy(.65f))}
    }
}
@Composable private fun MushroomDetail(x:MushroomGuideItem,onBack:()->Unit){
    val ctx=LocalContext.current
    LazyColumn(Modifier.fillMaxSize()){item{
        TextButton(onClick=onBack){Text("← К списку")}
        Text(x.name,color=Color.White,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.headlineSmall)
        Text(x.scientificName,color=Color.White.copy(.6f));Text(x.edibleStatus,color=if(x.edibleStatus.contains("яд",true))Color(0xFFFF9D93) else Color(0xFF8FE6B6),fontWeight=FontWeight.SemiBold)
        Info("Сезон",x.season);Info("Где искать",x.habitat);Info("Признаки",x.features.joinToString("\n• ",prefix=if(x.features.isEmpty())"" else "• "))
        if(x.lookalikes.isNotEmpty())Info("Опасные двойники",x.lookalikes.joinToString("\n• ",prefix="• "))
        Info("Важно",x.note)
        if(x.imageSource!=null) TextButton(onClick={runCatching{ctx.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(x.imageSource)))}}){Text("Источник фотографии / лицензия")}
    }}
}
@Composable private fun FishDetail(x:FishGuideItem,onBack:()->Unit){LazyColumn(Modifier.fillMaxSize()){item{TextButton(onClick=onBack){Text("← К списку")};Text(x.name,color=Color.White,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.headlineSmall);Text(x.scientificName,color=Color.White.copy(.6f));Info("Правовой статус",legalLabel(x.legalStatus));x.minLegalSizeCm?.let{Info("Минимальный размер","${it.toInt()} см")};x.dailyLimit?.let{Info("Суточный лимит",it)};Info("Базовые способы",x.methods.joinToString(" · "));Info("Типичные места",x.habitat);Info("Важно",x.notes)}}}
@Composable private fun Info(title:String,text:String){if(text.isBlank())return;GlassSurface(Modifier.fillMaxWidth().padding(vertical=5.dp),alpha=.58f,radius=18){Column(Modifier.padding(12.dp)){Text(title,color=Color.White,fontWeight=FontWeight.SemiBold);Text(text,color=Color.White.copy(.75f),style=MaterialTheme.typography.bodySmall)}}}
private fun legalLabel(v:String)=when(v){"PROHIBITED_IN_MOSCOW_MO"->"⚠ Запрещён к добыче в Москве/МО";"PRIVATE_WATER_RULES_ONLY"->"Только по правилам платного хозяйства";else->"Проверяйте актуальные региональные правила"}
