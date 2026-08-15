package ru.navigatordosuga.app.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import ru.navigatordosuga.app.AppContainer
import ru.navigatordosuga.app.data.db.CarMarkerEntity
import ru.navigatordosuga.app.map.NativeMap
import ru.navigatordosuga.app.map.MapMarkerOverlay
import ru.navigatordosuga.app.model.*
import ru.navigatordosuga.app.ui.components.*
import ru.navigatordosuga.app.ui.games.GameHubScreen
import ru.navigatordosuga.app.ui.guides.GuideScreen
import ru.navigatordosuga.app.ui.offline.OfflineMapsScreen
import ru.navigatordosuga.app.ui.profile.ProfileManagerScreen
import ru.navigatordosuga.app.ui.profile.ProfileSetup
import ru.navigatordosuga.app.ui.theme.NavigatorTheme
import kotlin.math.roundToInt

private enum class Overlay { NONE, MODES, SEARCH, MENU, TOOLS, FILTERS, LIMITS }

@Composable
fun NavigatorRoot(c: AppContainer) {
    val vm: NavigatorViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            @Suppress("UNCHECKED_CAST") (NavigatorViewModel(c) as T)
    })
    val theme by vm.theme.collectAsState()
    val systemDark = isSystemInDarkTheme()
    val dark = when (theme) { "light" -> false; "dark" -> true; else -> systemDark }
    NavigatorTheme(dark = dark) { NavigatorContent(c, vm, dark, theme) }
}

@Composable
private fun NavigatorContent(c: AppContainer, vm: NavigatorViewModel, dark: Boolean, theme: String) {
    val mode by vm.mode.collectAsState()
    val camera by vm.camera.collectAsState()
    val mapItems by vm.items.collectAsState()
    val events by vm.events.collectAsState()
    val section by vm.bottom.collectAsState()
    val games by vm.games.collectAsState()
    val profileSetup by vm.profileSetup.collectAsState()
    val profileManager by vm.profileManager.collectAsState()
    val profiles by vm.profiles.collectAsState()
    val activeProfileId by vm.activeProfileId.collectAsState()
    val offline by vm.offlineMaps.collectAsState()
    val guides by vm.guides.collectAsState()
    val filter by vm.eventFilter.collectAsState()
    val query by vm.query.collectAsState()
    val searchResults by vm.searchResults.collectAsState()
    val selected by vm.selectedId.collectAsState()
    val sync by vm.syncStates.collectAsState()
    val saved by vm.savedItems.collectAsState()
    val trip by vm.tripItems.collectAsState()
    val car by vm.carMarker.collectAsState()
    val activeTrack by vm.activeTrackId.collectAsState()
    val liveGlass by vm.liveGlass.collectAsState()
    var overlay by remember { mutableStateOf(Overlay.NONE) }

    if (profileSetup) { ProfileSetup(onDone = vm::createProfile, onSkip = vm::closeProfileSetup); return }
    if (profileManager) { ProfileManagerScreen(profiles, activeProfileId, onSelect = vm::setActiveProfile, onAdd = vm::newProfile, onClose = { vm.profileManager(false) }); return }
    if (games) { GameHubScreen(c, onClose = { vm.games(false) }); return }
    if (offline) { OfflineMapsScreen(onClose = { vm.offlineMaps(false) }); return }
    if (guides) { GuideScreen(c.guides, onClose = { vm.guides(false) }); return }

    val accent = modeAccent(mode)
    LiveGlassHost(c.appContext, liveGlass) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            NativeMap(
                items = if (mode == ActivityMode.EVENTS) emptyList() else mapItems,
                events = if (mode == ActivityMode.EVENTS) events else emptyList(),
                camera = camera,
                dark = dark,
                modifier = Modifier.fillMaxSize(),
                onCameraChanged = vm::camera,
                onItemClick = vm::select
            )
            MapMarkerOverlay(
                items = if (mode == ActivityMode.EVENTS) emptyList() else mapItems,
                events = if (mode == ActivityMode.EVENTS) events else emptyList(),
                camera = camera,
                modifier = Modifier.fillMaxSize()
            )
            TopChrome(mode, accent, overlay, onOverlay = { overlay = if (overlay == it) Overlay.NONE else it })
            MapButtons(c, vm, overlay, onTools = { overlay = if (overlay == Overlay.TOOLS) Overlay.NONE else Overlay.TOOLS })
            BottomPanel(mode, section, mapItems, events, filter, saved, trip, car, activeTrack, selected, accent, vm) { overlay = Overlay.FILTERS }
            BottomDock(section, accent, vm::bottom)

            if (overlay != Overlay.NONE) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (dark) .26f else .12f)).clickable { overlay = Overlay.NONE })
                when (overlay) {
                    Overlay.MODES -> ModeChooser(mode, accent, onMode = { vm.mode(it); overlay = Overlay.NONE })
                    Overlay.SEARCH -> SearchOverlay(query, searchResults, vm::query) { vm.openSearchHit(it); overlay = Overlay.NONE }
                    Overlay.MENU -> MainMenu(
                        liveGlass, theme,
                        onProfiles = { overlay = Overlay.NONE; vm.profileManager(true) },
                        onGames = { overlay = Overlay.NONE; vm.games(true) },
                        onGuides = { overlay = Overlay.NONE; vm.guides(true) },
                        onOffline = { overlay = Overlay.NONE; vm.offlineMaps(true) },
                        onLiveGlass = vm::toggleLiveGlass,
                        onTheme = vm::cycleTheme,
                        onRefresh = { vm.refresh(); overlay = Overlay.NONE },
                        onLimits = { overlay = Overlay.LIMITS }
                    )
                    Overlay.TOOLS -> MapTools(
                        onNorth = { vm.camera(camera.copy(bearing = 0.0, tilt = 0.0)); overlay = Overlay.NONE },
                        onRefresh = { vm.refresh(); overlay = Overlay.NONE },
                        onOffline = { overlay = Overlay.NONE; vm.offlineMaps(true) },
                        onGuides = { overlay = Overlay.NONE; vm.guides(true) }
                    )
                    Overlay.FILTERS -> FilterOverlay(filter, vm, onClose = { overlay = Overlay.NONE })
                    Overlay.LIMITS -> LimitsOverlay { overlay = Overlay.NONE }
                    Overlay.NONE -> Unit
                }
            }

            val failed = sync.count { it.status == "error" }
            if (failed > 0 && overlay == Overlay.NONE) Text(
                "Работаем с сохранёнными данными",
                Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 76.dp)
                    .background(Color(0xD9403127), RoundedCornerShape(18.dp)).padding(horizontal = 12.dp, vertical = 7.dp),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun BoxScope.TopChrome(mode: ActivityMode, accent: Color, overlay: Overlay, onOverlay: (Overlay) -> Unit) {
    Row(
        Modifier.align(Alignment.TopCenter).fillMaxWidth().statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassButton(Modifier.weight(1f).height(64.dp), onClick = { onOverlay(Overlay.MODES) }) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(48.dp).background(accent.copy(.15f), RoundedCornerShape(18.dp)).border(1.dp, accent.copy(.48f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) { Icon(activityIcon(mode), null, tint = accent) }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(mode.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (mode == ActivityMode.EVENTS) "Москва · сегодня" else "Москва / МО", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(.62f), maxLines = 1)
                }
                Icon(Icons.Rounded.KeyboardArrowDown, null)
            }
        }
        SquareGlassButton(overlay == Overlay.SEARCH, { onOverlay(Overlay.SEARCH) }) { Icon(Icons.Rounded.Search, "Поиск") }
        SquareGlassButton(overlay == Overlay.MENU, { onOverlay(Overlay.MENU) }) { Icon(Icons.Rounded.MoreHoriz, "Меню") }
    }
}

@Composable
private fun SquareGlassButton(active: Boolean = false, onClick: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    GlassButton(
        Modifier.size(64.dp).then(if (active) Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(.55f), RoundedCornerShape(24.dp)) else Modifier),
        onClick = onClick,
        content = content
    )
}

@Composable
private fun BoxScope.MapButtons(c: AppContainer, vm: NavigatorViewModel, overlay: Overlay, onTools: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    fun locate() { scope.launch { c.location.currentLocation(true)?.let { vm.camera(MapCameraState(it.latitude, it.longitude, 14.5)) } } }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { p -> if (p.values.any { it }) locate() }
    Column(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(end = 16.dp, top = 112.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SquareGlassButton(onClick = {
            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) locate()
            else launcher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
        }) { Icon(Icons.Rounded.MyLocation, "Моё местоположение") }
        SquareGlassButton(overlay == Overlay.TOOLS, onTools) { Icon(Icons.Rounded.Settings, "Настройки карты") }
    }
}

@Composable
private fun BoxScope.ModeChooser(current: ActivityMode, accent: Color, onMode: (ActivityMode) -> Unit) {
    GlassSurface(
        Modifier.align(Alignment.TopCenter).statusBarsPadding().fillMaxWidth().padding(start = 18.dp, end = 88.dp, top = 92.dp),
        alpha = .94f,
        radius = 28
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("ЧТО ИССЛЕДУЕМ?", color = MaterialTheme.colorScheme.onSurface.copy(.58f), style = MaterialTheme.typography.labelLarge)
            ActivityMode.entries.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { mode ->
                        val selected = mode == current
                        Surface(
                            modifier = Modifier.weight(1f).height(94.dp).clickable { onMode(mode) },
                            shape = RoundedCornerShape(24.dp),
                            color = if (selected) accent.copy(.20f) else MaterialTheme.colorScheme.onSurface.copy(.055f),
                            border = BorderStroke(1.dp, if (selected) accent.copy(.60f) else MaterialTheme.colorScheme.onSurface.copy(.12f))
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(46.dp).background(modeAccent(mode).copy(.13f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                                    Icon(activityIcon(mode), null, tint = modeAccent(mode))
                                }
                                Spacer(Modifier.width(9.dp))
                                Column {
                                    Text(mode.title, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text(modeSubtitle(mode), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(.56f), maxLines = 2)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.SearchOverlay(query: String, results: List<SearchHit>, onQuery: (String) -> Unit, onClick: (SearchHit) -> Unit) {
    GlassSurface(
        Modifier.align(Alignment.TopCenter).statusBarsPadding().fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 92.dp),
        alpha = .94f,
        radius = 28
    ) {
        Column(Modifier.padding(10.dp)) {
            TextField(
                value = query,
                onValueChange = onQuery,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Место, событие, фильм…") },
                colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent),
                leadingIcon = { Icon(Icons.Rounded.Search, null) }
            )
            if (query.trim().length >= 2) SearchResults(results, onClick)
        }
    }
}

@Composable
private fun SearchResults(results: List<SearchHit>, onClick: (SearchHit) -> Unit) {
    if (results.isEmpty()) { Text("Ничего не найдено в локальной базе", Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onSurface.copy(.65f)); return }
    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
        items(results, key = { "${it.mode.wire}:${it.id}" }) { hit ->
            Row(Modifier.fillMaxWidth().clickable { onClick(hit) }.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(activityIcon(hit.mode), null, tint = modeAccent(hit.mode))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(hit.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${hit.mode.title} · ${hit.subtitle}", color = MaterialTheme.colorScheme.onSurface.copy(.58f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun BoxScope.MainMenu(
    liveGlass: Boolean,
    theme: String,
    onProfiles: () -> Unit,
    onGames: () -> Unit,
    onGuides: () -> Unit,
    onOffline: () -> Unit,
    onLiveGlass: () -> Unit,
    onTheme: () -> Unit,
    onRefresh: () -> Unit,
    onLimits: () -> Unit
) {
    GlassSurface(
        Modifier.align(Alignment.TopCenter).statusBarsPadding().fillMaxWidth().padding(start = 78.dp, end = 18.dp, top = 92.dp),
        alpha = .96f,
        radius = 28
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("НАВИГАТОР ДОСУГА", color = MaterialTheme.colorScheme.onSurface.copy(.55f), style = MaterialTheme.typography.labelLarge)
            MenuRow(Icons.Rounded.Person, "Профиль", "Настроить личный профиль", onProfiles)
            MenuRow(Icons.Rounded.SportsEsports, "Игры", "Башенки · физическая стройка", onGames)
            MenuRow(Icons.Rounded.MenuBook, "Справочники", "Грибы и рыба офлайн", onGuides)
            MenuRow(Icons.Rounded.Map, "Офлайн-карты", "Загруженные регионы", onOffline)
            MenuRow(Icons.Rounded.DarkMode, "Тема", "Система · светлая · тёмная", onTheme, themeTitle(theme))
            MenuRow(Icons.Rounded.AutoAwesome, "Живое стекло", "Свет реагирует на наклон", onLiveGlass, if (liveGlass) "Вкл" else "Выкл")
            MenuRow(Icons.Rounded.Refresh, "Данные", "Свежесть и состояние слоя", onRefresh)
            MenuRow(Icons.Rounded.ErrorOutline, "Ограничения", "Доступ, правила и точность", onLimits)
        }
    }
}

@Composable
private fun MenuRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit, badge: String? = null) {
    Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), color = MaterialTheme.colorScheme.onSurface.copy(.055f), shape = RoundedCornerShape(22.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurface.copy(.86f))
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurface.copy(.55f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (badge != null) Surface(shape = CircleShape, color = MaterialTheme.colorScheme.onSurface.copy(.08f)) {
                Text(badge, Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = MaterialTheme.colorScheme.onSurface.copy(.68f), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun BoxScope.MapTools(onNorth: () -> Unit, onRefresh: () -> Unit, onOffline: () -> Unit, onGuides: () -> Unit) {
    GlassSurface(Modifier.align(Alignment.TopEnd).statusBarsPadding().width(280.dp).padding(end = 18.dp, top = 180.dp), alpha = .96f, radius = 28) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MenuRow(Icons.Rounded.Layers, "Слои карты", "Карта и офлайн-регионы", onOffline)
            MenuRow(Icons.Rounded.North, "Север сверху", "Сбросить поворот карты", onNorth)
            MenuRow(Icons.Rounded.Refresh, "Данные", "Обновить текущий слой", onRefresh)
            MenuRow(Icons.Rounded.MenuBook, "Легенда карты", "Маркеры и справочники", onGuides)
        }
    }
}

@Composable
private fun BoxScope.FilterOverlay(filter: EventFilter, vm: NavigatorViewModel, onClose: () -> Unit) {
    GlassSurface(Modifier.align(Alignment.Center).fillMaxWidth().padding(22.dp), alpha = .97f, radius = 30) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Фильтры", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Закрыть") }
            }
            Text("Когда", fontWeight = FontWeight.Bold)
            ChipRow(listOf("today" to "Сегодня", "tomorrow" to "Завтра", "weekend" to "Выходные", "7d" to "7 дней")) { vm.eventRange(it) }
            Text("Стоимость", fontWeight = FontWeight.Bold)
            ChipRow(listOf("all" to "Все", "free" to "Бесплатно", "paid" to "Платно"), selected = filter.price) { vm.eventPrice(it) }
            GlassButton(Modifier.fillMaxWidth(), onClick = onClose) { Icon(Icons.Rounded.Check, null); Spacer(Modifier.width(8.dp)); Text("Готово", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun ChipRow(rows: List<Pair<String, String>>, selected: String? = null, onClick: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { (key, title) ->
            Surface(
                modifier = Modifier.clickable { onClick(key) }, shape = RoundedCornerShape(18.dp),
                color = if (selected == key) MaterialTheme.colorScheme.primary.copy(.22f) else MaterialTheme.colorScheme.onSurface.copy(.055f),
                border = BorderStroke(1.dp, if (selected == key) MaterialTheme.colorScheme.primary.copy(.55f) else MaterialTheme.colorScheme.onSurface.copy(.15f))
            ) { Text(title, Modifier.padding(horizontal = 16.dp, vertical = 10.dp), fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun BoxScope.LimitsOverlay(onClose: () -> Unit) {
    GlassSurface(Modifier.align(Alignment.Center).fillMaxWidth().padding(24.dp), alpha = .97f, radius = 30) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Ограничения и безопасность", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Точки на карте носят справочный характер. Проверяйте правила посещения территорий, погоду и официальные предупреждения. Для GPS и записи пути требуется разрешение геолокации.", color = MaterialTheme.colorScheme.onSurface.copy(.7f))
            GlassButton(Modifier.fillMaxWidth(), onClick = onClose) { Text("Понятно", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun BoxScope.BottomPanel(
    mode: ActivityMode, section: BottomSection, geo: List<GeoItem>, events: List<EventItem>, filter: EventFilter,
    saved: List<StoredItem>, trip: List<StoredItem>, car: CarMarkerEntity?, activeTrack: String?, selected: String?,
    accent: Color, vm: NavigatorViewModel, onFilters: () -> Unit
) {
    val rows: List<Any> = if (mode == ActivityMode.EVENTS) events.take(if (section == BottomSection.TOP) 20 else 8) else geo.take(if (section == BottomSection.TOP) 20 else 8)
    val expanded = section != BottomSection.MAP || selected != null
    GlassSurface(
        Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(start = 12.dp, end = 12.dp, bottom = 92.dp)
            .heightIn(min = if (expanded) 330.dp else 112.dp, max = if (expanded) 535.dp else 122.dp),
        alpha = .92f, radius = 30
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp)) {
            Box(Modifier.align(Alignment.CenterHorizontally).width(52.dp).height(5.dp).background(MaterialTheme.colorScheme.onSurface.copy(.34f), CircleShape))
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(panelTitle(mode, section), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    Text(panelSubtitle(mode, section, geo.size, events.size), color = MaterialTheme.colorScheme.onSurface.copy(.62f), style = MaterialTheme.typography.bodyMedium)
                }
                if (mode == ActivityMode.EVENTS || section == BottomSection.TOP) {
                    GlassButton(onClick = onFilters) { Icon(Icons.Rounded.FilterList, null); Spacer(Modifier.width(6.dp)); Text("Фильтры", fontWeight = FontWeight.Bold) }
                }
            }
            if (!expanded) return@Column
            Spacer(Modifier.height(12.dp))
            when (section) {
                BottomSection.TRIP -> {
                    TripTools(car, activeTrack, vm); Spacer(Modifier.height(10.dp))
                    StoredList(trip, "Маршрут пока пуст", "Добавляйте места и события кнопкой «В поездку».") { vm.removeTrip(it) }
                }
                BottomSection.SAVED -> StoredList(saved, "Пока ничего не сохранено", "Откройте место или событие и нажмите «Сохранить».") { vm.removeSaved(it) }
                else -> {
                    if (mode == ActivityMode.EVENTS && section == BottomSection.TOP) {
                        ChipRow(listOf("today" to "Сегодня", "tomorrow" to "Завтра", "weekend" to "Выходные", "7d" to "7 дней")) { vm.eventRange(it) }
                        Spacer(Modifier.height(10.dp))
                    }
                    val chosen = rows.firstOrNull { when (it) { is GeoItem -> it.id == selected; is EventItem -> it.id == selected; else -> false } }
                    if (chosen != null) DetailCard(chosen, mode, accent, vm)
                    else LazyColumn(Modifier.heightIn(max = 360.dp)) {
                        items(rows, key = { when (it) { is GeoItem -> "g:${it.id}"; is EventItem -> "e:${it.id}"; else -> it.hashCode().toString() } }) { row ->
                            when (row) { is GeoItem -> GeoRow(row, accent) { vm.select(row.id) }; is EventItem -> EventRow(row, accent) { vm.select(row.id) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TripTools(car: CarMarkerEntity?, activeTrack: String?, vm: NavigatorViewModel) {
    val ctx = LocalContext.current
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result -> if (result.values.any { it }) pending?.invoke(); pending = null }
    fun withLocation(action: () -> Unit) {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) action()
        else { pending = action; launcher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)) }
    }
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AssistChip(onClick = { withLocation(vm::rememberCar) }, label = { Text(if (car == null) "Запомнить машину" else "Обновить машину") }, leadingIcon = { Icon(Icons.Rounded.DirectionsCar, null) })
        if (car != null) AssistChip(onClick = {
            val uri = Uri.parse("geo:${car.lat},${car.lon}?q=${car.lat},${car.lon}(${Uri.encode("Моя машина")})")
            runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
        }, label = { Text("К машине") }, leadingIcon = { Icon(Icons.Rounded.NearMe, null) })
        AssistChip(onClick = { if (activeTrack == null) withLocation(vm::startTrack) else vm.stopTrack() }, label = { Text(if (activeTrack == null) "Запись пути" else "Остановить запись") }, leadingIcon = { Icon(if (activeTrack == null) Icons.Rounded.RadioButtonChecked else Icons.Rounded.StopCircle, null) })
    }
}

@Composable
private fun StoredList(rows: List<StoredItem>, empty: String, hint: String, remove: (StoredItem) -> Unit) {
    if (rows.isEmpty()) {
        Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurface.copy(.055f), shape = RoundedCornerShape(26.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(.12f))) {
            Column(Modifier.padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(empty, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge); Spacer(Modifier.height(6.dp))
                Text(hint, color = MaterialTheme.colorScheme.onSurface.copy(.62f))
            }
        }
        return
    }
    LazyColumn(Modifier.heightIn(max = 350.dp)) {
        items(rows, key = { "${it.dataset}:${it.itemId}" }) { x ->
            Surface(Modifier.fillMaxWidth().padding(vertical = 4.dp), color = MaterialTheme.colorScheme.onSurface.copy(.05f), shape = RoundedCornerShape(20.dp)) {
                Row(Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(x.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(datasetTitle(x.dataset), color = MaterialTheme.colorScheme.onSurface.copy(.55f), style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = { remove(x) }) { Icon(Icons.Rounded.Close, "Удалить") }
                }
            }
        }
    }
}

@Composable
private fun GeoRow(x: GeoItem, accent: Color, onClick: () -> Unit) {
    Surface(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick), color = MaterialTheme.colorScheme.onSurface.copy(.05f), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(.10f))) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).background(accent.copy(.16f), RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Navigation, null, tint = accent) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(x.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(x.summary.ifBlank { x.subCategory }, color = MaterialTheme.colorScheme.onSurface.copy(.58f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
            }
            Text(x.score.roundToInt().toString(), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EventRow(x: EventItem, accent: Color, onClick: () -> Unit) {
    Surface(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick), color = MaterialTheme.colorScheme.onSurface.copy(.05f), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(.10f))) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(accent.copy(.16f), RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Event, null, tint = accent) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(x.title, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${x.startDateTime.take(16).replace('T', ' ')} · ${x.venueName}", color = MaterialTheme.colorScheme.onSurface.copy(.58f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
            }
            Text(if (x.isFree) "Бесплатно" else x.priceMin?.let { "от ${it.roundToInt()} ₽" } ?: "₽", color = if (x.isFree) Color(0xFF50D9A4) else Color(0xFFFFB74D), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
        }
    }
}

private data class ActionTarget(val id: String, val name: String, val lat: Double?, val lon: Double?, val dataset: String, val snapshot: String, val url: String? = null)
private fun targetOf(row: Any, mode: ActivityMode): ActionTarget? = when (row) {
    is GeoItem -> ActionTarget(row.id, row.name, row.lat, row.lon, mode.wire, snapshot(row.id, row.name))
    is EventItem -> ActionTarget(row.id, row.title, row.lat, row.lon, "events", snapshot(row.id, row.title, row.startDateTime.take(10)), row.registrationUrl ?: row.ticketUrl)
    else -> null
}
private fun snapshot(id: String, name: String, date: String? = null): String {
    fun esc(v: String) = v.replace("\\", "\\\\").replace("\"", "\\\"")
