package ru.navigatordosuga.app.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.navigatordosuga.app.AppContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun GameHubScreen(c:AppContainer,onClose:()->Unit){var tower by remember{mutableStateOf(false)};if(tower){TowerGameScreen(c){tower=false};return};Scaffold(topBar={TopAppBar(title={Text("Игры")},navigationIcon={IconButton(onClick=onClose){Icon(Icons.Rounded.ArrowBack,null)}})}){p->Column(Modifier.padding(p).fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0E1718),Color(0xFF17342F)))).padding(18.dp)){Card(onClick={tower=true},modifier=Modifier.fillMaxWidth()){Row(Modifier.padding(18.dp),horizontalArrangement=Arrangement.spacedBy(16.dp)){Icon(Icons.Rounded.Apartment,null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(48.dp));Column{Text("Башенки",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge);Text("Построй самый высокий панельный небоскрёб Москвы. Физика, инерция и рекорды работают офлайн.")}}}}}
}
