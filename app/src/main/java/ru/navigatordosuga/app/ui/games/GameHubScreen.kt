package ru.navigatordosuga.app.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.navigatordosuga.app.AppContainer
import ru.navigatordosuga.app.game.tower.TowerGameRepository
import ru.navigatordosuga.app.ui.components.GlassButton
import ru.navigatordosuga.app.ui.components.GlassSurface

@Composable
fun GameHubScreen(c: AppContainer, onClose: () -> Unit) {
    var tower by remember { mutableStateOf(false) }
    if (tower) { TowerGameScreen(c) { tower = false }; return }

    val profileId by c.profiles.activeId.collectAsState(initial = "guest")
    val repo = remember { TowerGameRepository(c.db) }
    val stats by remember(profileId) { repo.stats(profileId ?: "guest") }.collectAsState(initial = null)
    val blue = Color(0xFF86C9EA)
    val orange = Color(0xFFFFB74D)

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF8FB3C1), Color(0xFF9EB3B1), Color(0xFF718884)))
        )
    ) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassButton(Modifier.size(58.dp), onClick = onClose) { Icon(Icons.Rounded.ArrowBack, "Назад") }
            Spacer(Modifier.width(18.dp))
            Column {
                Text("НАВИГАТОР ДОСУГА", color = Color.White.copy(.68f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text("Игры", color = Color.White, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            }
        }

        GlassSurface(
            Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 22.dp).heightIn(max = 620.dp),
            alpha = .88f,
            radius = 34
        ) {
            Column(Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth().height(250.dp).background(blue, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))) {
                    TowerPreview(Modifier.fillMaxSize())
                    Box(Modifier.padding(20.dp).size(74.dp).background(Color(0xFF213D4A), RoundedCornerShape(24.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Apartment, null, tint = orange, modifier = Modifier.size(40.dp))
                    }
                }
                Column(Modifier.padding(26.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("GAME 01", color = Color.White.copy(.60f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text("Башенки", color = Color.White, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    Text(
                        "Построй самый высокий панельный небоскрёб Москвы. Один тап — и дальше всё решает физика.",
                        color = Color.White.copy(.66f), style = MaterialTheme.typography.titleMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatPill("${stats?.bestFloors ?: 0}", "этажей")
                        StatPill("${stats?.bestScore ?: 0}", "рекорд")
                    }
                    GlassButton(
                        Modifier.width(154.dp).background(Brush.horizontalGradient(listOf(Color(0xFFFFD77C), orange)), RoundedCornerShape(22.dp)),
                        onClick = { tower = true }
                    ) { Text("Играть", color = Color(0xFF172020), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
private fun RowScope.StatPill(value: String, label: String) {
    GlassSurface(Modifier.weight(1f), alpha = .70f, radius = 20) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.Bottom) {
            Text(value, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(6.dp))
            Text(label, color = Color.White.copy(.60f), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun TowerPreview(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val ground = size.height * .86f
        drawRect(Color(0xFFB6B4A2), Offset(0f, ground), Size(size.width, size.height - ground))
        drawRect(Color(0xFF30444C), Offset(size.width * .18f, size.height * .18f), Size(size.width * .72f, 5f))
        drawRect(Color(0xFF30444C), Offset(size.width * .82f, size.height * .18f), Size(5f, size.height * .33f))
        drawLine(Color(0xFFF2AD45), Offset(size.width * .82f, size.height * .51f), Offset(size.width * .86f, size.height * .51f), 6f)
        val left = size.width * .27f
        val width = size.width * .48f
        val floorH = size.height * .105f
        repeat(5) { floor ->
            val top = ground - floorH * (floor + 1)
            drawRect(if (floor % 2 == 0) Color(0xFFD2D6D2) else Color(0xFFC6CFD0), Offset(left, top), Size(width, floorH - 2f))
            repeat(4) { col ->
                drawRect(Color(0xFF80A7B8), Offset(left + width * (.10f + col * .23f), top + floorH * .22f), Size(width * .11f, floorH * .46f))
            }
        }
    }
}
