package ru.navigatordosuga.app.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import ru.navigatordosuga.app.map.NativeMap
import ru.navigatordosuga.app.model.*
import ru.navigatordosuga.app.ui.components.*
import ru.navigatordosuga.app.ui.games.GameHubScreen
import ru.navigatordosuga.app.ui.guides.GuideScreen
import ru.navigatordosuga.app.ui.offline.OfflineMapsScreen
import ru.navigatordosuga.app.ui.profile.ProfileSetup
import ru.navigatordosuga.app.ui.profile.ProfileManagerScreen
import kotlin.math.roundToInt

@Composable
fun NavigatorRoot(c: AppContainer) {
    val vm: NavigatorViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            @Suppress("UNCHECKED_CAST") (NavigatorViewModel(c) as T)
    })
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

    if (profileSetup) { ProfileSetup(onDone = vm::createProfile, onSkip = vm::closeProfileSetup); return }
    if (profileManager) { ProfileManagerScreen(profiles,activeProfileId,onSelect=vm::setActiveProfile,onAdd=vm::newProfile,onClose={vm.profileManager(false)}); return }
    if (games) { GameHubScreen(c, onClose = { vm.games(false) }); return }
    if (offline) { OfflineMapsScreen(onClose = { vm.offlineMaps(false) }); return }
    if (guides) { GuideScreen(c.guides,onClose={vm.guides(false)}); return }

    LiveGlassHost(c.appContext,liveGlass) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        NativeMap(
            items = if (mode == ActivityMode.EVENTS) emptyList() else mapItems,
            events = if (mode == ActivityMode.EVENTS) events else emptyList(),
            camera = camera,
            modifier = Modifier.fillMaxSize(),
            onCameraChanged = vm::camera,
            onItemClick = vm::select
        )
        TopControls(
            mode = mode,
            query = query,
            results = searchResults,
            onQuery = vm::query,
            onSearchHit = vm::openSearchHit,
            onMode = vm::mode,
            onRefresh = vm::refresh,
            onGames = { vm.games(true) },
            onOffline = { vm.offlineMaps(true) },
            onGuides = { vm.guides(true) },
            onProfiles = { vm.profileManager(true) },
            liveGlass = liveGlass,
            onLiveGlass = vm::toggleLiveGlass
        )
        LocationButton(c, vm)
        if (mode == ActivityMode.EVENTS) EventFilters(filter, vm)
        BottomPanel(mode, section, mapItems, events, saved, trip, car, activeTrack, selected, vm)
        BottomDock(section, vm::bottom)

        val failed = sync.count { it.status == "error" }
        if (failed > 0) Text(
            "Офлайн/обновление недоступно · сохранённые данные",
            Modifier.align(Alignment.TopCenter).padding(top = 112.dp)
                .background(Color(0xCC332B1E), RoundedCornerShape(18.dp)).padding(10.dp),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall
        )
    }
    }
}

@Composable
private fun TopControls(
    mode: ActivityMode,
    query: String,
    results: List<SearchHit>,
    onQuery: (String) -> Unit,
    onSearchHit: (SearchHit) -> Unit,
    onMode: (ActivityMode) -> Unit,
    onRefresh: () -> Unit,
    onGames: () -> Unit,
    onOffline: () -> Unit,
    onGuides: () -> Unit,
    onProfiles: () -> Unit,
    liveGlass: Boolean,
    onLiveGlass: () -> Unit
) {
    var switcher by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }

    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassButton(onClick = { switcher = true }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(activityIcon(mode), null, tint = Color.White)
                Spacer(Modifier.width(7.dp))
                Text(mode.title, fontWeight = FontWeight.SemiBold, color = Color.White)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Rounded.KeyboardArrowDown, null, tint = Color.White)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassButton(onClick = { search = !search; if (!search) onQuery("") }) { Icon(Icons.Rounded.Search, "Поиск", tint = Color.White) }
            GlassButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreHoriz, "Меню", tint = Color.White) }
        }
    }

    if (search) {
        GlassSurface(
            Modifier.fillMaxWidth().statusBarsPadding().padding(top = 67.dp, start = 12.dp, end = 12.dp),
            alpha = .83f
        ) {
            Column {
                TextField(
                    value = query,
                    onValueChange = onQuery,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Найти место, событие, фильм…") },
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent),
                    leadingIcon = { Icon(Icons.Rounded.Search, null) }
                )
                if (query.trim().length >= 2) SearchResults(results) { hit -> onSearchHit(hit); search = false }
            }
        }
    }

    DropdownMenu(expanded = switcher, onDismissRequest = { switcher = false }) {
        ActivityMode.entries.forEach { m -> DropdownMenuItem(
            text = { Text(m.title) },
            leadingIcon = { Icon(activityIcon(m), null) },
            onClick = { onMode(m); switcher = false }
        ) }
    }
    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
        DropdownMenuItem(text = { Text("Профили") }, leadingIcon = { Icon(Icons.Rounded.Person, null) }, onClick = { menu = false; onProfiles() })
        DropdownMenuItem(text = { Text("Игры") }, leadingIcon = { Icon(Icons.Rounded.SportsEsports, null) }, onClick = { menu = false; onGames() })
        DropdownMenuItem(text = { Text("Справочники") }, leadingIcon = { Icon(Icons.Rounded.MenuBook, null) }, onClick = { menu = false; onGuides() })
        DropdownMenuItem(text = { Text("Офлайн-карты") }, leadingIcon = { Icon(Icons.Rounded.Map, null) }, onClick = { menu = false; onOffline() })
        DropdownMenuItem(text = { Text(if(liveGlass) "Живое стекло · вкл" else "Живое стекло · выкл") }, leadingIcon = { Icon(Icons.Rounded.AutoAwesome, null) }, trailingIcon={Switch(checked=liveGlass,onCheckedChange=null)}, onClick = { onLiveGlass() })
        DropdownMenuItem(text = { Text("Обновить данные") }, leadingIcon = { Icon(Icons.Rounded.Refresh, null) }, onClick = { menu = false; onRefresh() })
    }
}

@Composable
private fun SearchResults(results: List<SearchHit>, onClick: (SearchHit) -> Unit) {
    if (results.isEmpty()) {
        Text("Ничего не найдено в локальной базе", Modifier.padding(12.dp), color = Color.White.copy(.72f))
        return
    }
    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 270.dp)) {
        items(results, key = { "${it.mode.wire}:${it.id}" }) { hit ->
            Row(
                Modifier.fillMaxWidth().clickable { onClick(hit) }.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(activityIcon(hit.mode), null, tint = Color.White.copy(.9f))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(hit.title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${hit.mode.title} · ${hit.subtitle}", color = Color.White.copy(.62f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun BoxScope.LocationButton(c: AppContainer, vm: NavigatorViewModel) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    fun locate() { scope.launch { c.location.currentLocation(true)?.let { vm.camera(MapCameraState(it.latitude, it.longitude, 14.5)) } } }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { p -> if (p.values.any { it }) locate() }
    GlassButton(
        Modifier.align(Alignment.TopEnd).padding(end = 14.dp, top = 132.dp),
        onClick = {
            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) locate()
            else launcher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
        }
    ) { Icon(Icons.Rounded.MyLocation, "Моё местоположение", tint = Color.White) }
}

@Composable
private fun EventFilters(f: EventFilter, vm: NavigatorViewModel) {
    Column(Modifier.fillMaxWidth().padding(top = 126.dp, start = 12.dp, end = 12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("today" to "Сегодня", "tomorrow" to "Завтра", "weekend" to "Выходные", "7d" to "7 дней").forEach { (k, t) ->
                FilterChip(selected = false, onClick = { vm.eventRange(k) }, label = { Text(t) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("all" to "Все", "free" to "Бесплатно", "paid" to "Платно").forEach { (k, t) ->
                FilterChip(selected = f.price == k, onClick = { vm.eventPrice(k) }, label = { Text(t) })
            }
        }
    }
}

@Composable
private fun BoxScope.BottomPanel(
    mode: ActivityMode,
    section: BottomSection,
    geo: List<GeoItem>,
    events: List<EventItem>,
    saved: List<StoredItem>,
    trip: List<StoredItem>,
    car: ru.navigatordosuga.app.data.db.CarMarkerEntity?,
    activeTrack: String?,
    selected: String?,
    vm: NavigatorViewModel
) {
    val rows: List<Any> = if (mode == ActivityMode.EVENTS) events.take(if (section == BottomSection.TOP) 20 else 8) else geo.take(if (section == BottomSection.TOP) 20 else 8)
    GlassSurface(
        Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = 86.dp).heightIn(min = 72.dp, max = 310.dp),
        alpha = .76f,
        radius = 26
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when (section) {
                        BottomSection.MAP -> if (mode == ActivityMode.EVENTS) "Что происходит" else "Лучшее рядом"
                        BottomSection.TOP -> "TOP"
                        BottomSection.TRIP -> "Поездка"
                        BottomSection.SAVED -> "Мои места"
                    },
                    Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Color.White
                )
                if (section == BottomSection.TRIP && trip.isNotEmpty()) TextButton(onClick = vm::clearTrip) { Text("Очистить") }
            }
            Spacer(Modifier.height(6.dp))
            when (section) {
                BottomSection.TRIP -> {
                    TripTools(car, activeTrack, vm)
                    Spacer(Modifier.height(6.dp))
                    StoredList(trip, "Маршрут пока пуст") { vm.removeTrip(it) }
                }
                BottomSection.SAVED -> StoredList(saved, "Сохранённых мест пока нет") { vm.removeSaved(it) }
                else -> {
                    val chosen = rows.firstOrNull {
                        when (it) { is GeoItem -> it.id == selected; is EventItem -> it.id == selected; else -> false }
                    }
                    if (chosen != null) DetailActions(chosen, mode, vm)
                    LazyColumn(Modifier.heightIn(max = if (chosen == null) 220.dp else 140.dp)) {
                        items(rows, key = { when (it) { is GeoItem -> "g:${it.id}"; is EventItem -> "e:${it.id}"; else -> it.hashCode().toString() } }) { row ->
                            when (row) {
                                is GeoItem -> GeoRow(row, selected == row.id) { vm.select(row.id) }
                                is EventItem -> EventRow(row, selected == row.id) { vm.select(row.id) }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun TripTools(car: ru.navigatordosuga.app.data.db.CarMarkerEntity?, activeTrack:String?, vm:NavigatorViewModel){
    val ctx=LocalContext.current
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    val launcher=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){result ->
        if(result.values.any{it}) pending?.invoke()
        pending=null
    }
    fun withLocation(action:()->Unit){
        if(ContextCompat.checkSelfPermission(ctx,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(ctx,Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED) action()
        else { pending=action; launcher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION)) }
    }
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){
        AssistChip(onClick={withLocation(vm::rememberCar)},label={Text(if(car==null) "Запомнить машину" else "Обновить машину")},leadingIcon={Icon(Icons.Rounded.DirectionsCar,null)})
        if(car!=null) AssistChip(onClick={
            val uri=Uri.parse("geo:${car.lat},${car.lon}?q=${car.lat},${car.lon}(${Uri.encode("Моя машина")})")
            runCatching{ctx.startActivity(Intent(Intent.ACTION_VIEW,uri))}
        },label={Text("К машине")},leadingIcon={Icon(Icons.Rounded.NearMe,null)})
        AssistChip(onClick={if(activeTrack==null) withLocation(vm::startTrack) else vm.stopTrack()},label={Text(if(activeTrack==null) "Запись пути" else "Остановить запись")},leadingIcon={Icon(if(activeTrack==null) Icons.Rounded.RadioButtonChecked else Icons.Rounded.StopCircle,null)})
    }
}

@Composable
private fun StoredList(rows: List<StoredItem>, empty: String, remove: (StoredItem) -> Unit) {
    if (rows.isEmpty()) { Text(empty, color = Color.White.copy(.7f)); return }
    LazyColumn(Modifier.heightIn(max = 220.dp)) {
        items(rows, key = { "${it.dataset}:${it.itemId}" }) { x ->
            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(x.title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(datasetTitle(x.dataset), color = Color.White.copy(.58f), style = MaterialTheme.typography.labelSmall)
                }
                IconButton(onClick = { remove(x) }) { Icon(Icons.Rounded.Close, "Удалить", tint = Color.White.copy(.75f)) }
            }
        }
    }
}

@Composable
private fun GeoRow(x: GeoItem, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).background(if (selected) Color.White.copy(.13f) else Color.Transparent, RoundedCornerShape(15.dp)).padding(9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(x.name, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(x.summary.ifBlank { x.subCategory }, color = Color.White.copy(.68f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
        }
        Text(x.score.roundToInt().toString(), color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EventRow(x: EventItem, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).background(if (selected) Color.White.copy(.13f) else Color.Transparent, RoundedCornerShape(15.dp)).padding(9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(x.title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${x.startDateTime.take(16).replace('T', ' ')} · ${x.venueName}", color = Color.White.copy(.68f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
        }
        Text(if (x.isFree) "0 ₽" else x.priceMin?.let { "от ${it.roundToInt()} ₽" } ?: "₽", color = if (x.isFree) Color(0xFF80F2C0) else Color(0xFFFFCC80), fontWeight = FontWeight.Bold)
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
    return buildString { append("{\"id\":\"").append(esc(id)).append("\",\"name\":\"").append(esc(name)).append('"'); if (date != null) append(",\"date\":\"").append(esc(date)).append('"'); append('}') }
}

@Composable
private fun DetailActions(row: Any, mode: ActivityMode, vm: NavigatorViewModel) {
    val ctx = LocalContext.current
    val t = targetOf(row, mode) ?: return
    if (row is EventItem) {
        Text(row.description.ifBlank { row.subtitle }, color = Color.White.copy(.78f), style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(6.dp))
    }
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (t.lat != null && t.lon != null) AssistChip(
            onClick = {
                val uri = Uri.parse("geo:${t.lat},${t.lon}?q=${t.lat},${t.lon}(${Uri.encode(t.name)})")
                runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
            },
            label = { Text("Маршрут") }, leadingIcon = { Icon(Icons.Rounded.Directions, null) }
        )
        if (t.url != null) AssistChip(
            onClick = { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(t.url))) } },
            label = { Text(if (row is EventItem && row.registrationRequired) "Регистрация" else "Билеты") },
            leadingIcon = { Icon(Icons.Rounded.ConfirmationNumber, null) }
        )
        AssistChip(onClick = { vm.toggleSaved(t.dataset, t.id, t.snapshot) }, label = { Text("Сохранить") }, leadingIcon = { Icon(Icons.Rounded.FavoriteBorder, null) })
        AssistChip(onClick = { vm.addTrip(t.dataset, t.id, t.snapshot) }, label = { Text("В поездку") }, leadingIcon = { Icon(Icons.Rounded.AddRoad, null) })
    }
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun BoxScope.BottomDock(active: BottomSection, onSelect: (BottomSection) -> Unit) {
    GlassSurface(
        Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(10.dp).height(64.dp),
        alpha = .72f,
        radius = 28
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 6.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
            DockItem(active == BottomSection.MAP, Icons.Rounded.Map, "Карта") { onSelect(BottomSection.MAP) }
            DockItem(active == BottomSection.TOP, Icons.Rounded.Star, "TOP") { onSelect(BottomSection.TOP) }
            DockItem(active == BottomSection.TRIP, Icons.Rounded.Route, "Поездка") { onSelect(BottomSection.TRIP) }
            DockItem(active == BottomSection.SAVED, Icons.Rounded.Favorite, "Мои") { onSelect(BottomSection.SAVED) }
        }
    }
}

@Composable
private fun DockItem(active: Boolean, icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        Modifier.clickable(onClick = onClick).background(if (active) Color.White.copy(.16f) else Color.Transparent, CircleShape).padding(horizontal = 14.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, label, tint = Color.White)
        if (active) Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}

private fun activityIcon(m: ActivityMode): ImageVector = when (m) {
    ActivityMode.MUSHROOMS -> Icons.Rounded.Forest
    ActivityMode.FISHING -> Icons.Rounded.Water
    ActivityMode.BEAUTIFUL -> Icons.Rounded.Landscape
    ActivityMode.CINEMA -> Icons.Rounded.Movie
    ActivityMode.HISTORY -> Icons.Rounded.AccountBalance
    ActivityMode.EVENTS -> Icons.Rounded.Event
}

private fun datasetTitle(v: String) = when (v) {
    "mushrooms" -> "Грибы"
    "fishing" -> "Рыбалка"
    "beautiful" -> "Места"
    "cinema" -> "Кино"
    "history" -> "История"
    "events" -> "Мероприятия"
    else -> v
}
