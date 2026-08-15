package ru.navigatordosuga.app.ui.offline

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.geometry.LatLng
import ru.navigatordosuga.app.offline.OfflineMapManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun OfflineMapsScreen(onClose:()->Unit){
    val context=LocalContext.current;val manager=remember{OfflineMapManager(context)};var refresh by remember{mutableIntStateOf(0)};val packs by remember(refresh){manager.list()}.collectAsStateWithLifecycle(initialValue=emptyList());var progress by remember{mutableStateOf<String?>(null)};val scope=rememberCoroutineScope()
    Scaffold(topBar={TopAppBar(title={Text("Офлайн-карты")},navigationIcon={IconButton(onClick=onClose){Icon(Icons.Rounded.ArrowBack,null)}})}){pad->
        LazyColumn(Modifier.padding(pad).fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
            item{Text("Скачайте Москву или область заранее. POI уже хранятся в локальной базе; этот раздел сохраняет именно картографические ресурсы.")}
            item{Button(onClick={scope.launch{val b=LatLngBounds.from(56.05,38.25,55.40,36.75);manager.download("Москва",b,7.0,15.0).collect{progress="Москва · ${it.progress}% · ${it.bytes/1024/1024} МБ";if(it.complete)refresh++}}}){Icon(Icons.Rounded.Download,null);Spacer(Modifier.width(8.dp));Text("Скачать Москву")}}
            item{Button(onClick={scope.launch{val b=LatLngBounds.from(56.95,40.20,54.10,35.10);manager.download("Московская область",b,6.0,13.0).collect{progress="МО · ${it.progress}% · ${it.bytes/1024/1024} МБ";if(it.complete)refresh++}}}){Icon(Icons.Rounded.Download,null);Spacer(Modifier.width(8.dp));Text("Скачать Московскую область")}}
            progress?.let{item{LinearProgressIndicator(Modifier.fillMaxWidth());Text(it)}}
            items(packs){p->Card{Row(Modifier.fillMaxWidth().padding(14.dp),horizontalArrangement=Arrangement.SpaceBetween){Column{Text(p.name);Text("${p.progress}% · ${p.bytes/1024/1024} МБ",style=MaterialTheme.typography.bodySmall)};IconButton(onClick={manager.delete(p.id){refresh++}}){Icon(Icons.Rounded.Delete,"Удалить")}}}}
        }
    }
}
