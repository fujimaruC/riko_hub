package id.riko.hub.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.riko.hub.R
import id.riko.hub.core.time.Clock
import id.riko.hub.domain.logic.HomeSnapshot
import id.riko.hub.domain.logic.homeSnapshot
import id.riko.hub.domain.model.AppSettings
import id.riko.hub.domain.model.Category
import id.riko.hub.domain.model.Priority
import id.riko.hub.domain.model.Task
import id.riko.hub.domain.model.TaskStatus
import id.riko.hub.navigation.Destination
import id.riko.hub.ui.tasks.deadlineLabel
import id.riko.hub.ui.theme.ChefButter
import id.riko.hub.ui.theme.ChefCanvas
import id.riko.hub.ui.theme.ChefCoral
import id.riko.hub.ui.theme.ChefGround
import id.riko.hub.ui.theme.ChefInk
import id.riko.hub.ui.theme.ChefLime
import id.riko.hub.ui.theme.ChefLine
import id.riko.hub.ui.theme.ChefMuted
import id.riko.hub.ui.theme.ChefPanel
import id.riko.hub.ui.theme.ChefPeriwinkle
import id.riko.hub.ui.theme.ChefRaised
import id.riko.hub.ui.theme.NotoCondensedBlack
import id.riko.hub.ui.theme.RikoHubTheme
import id.riko.hub.ui.theme.RikoShapes
import id.riko.hub.ui.theme.Spacing
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

@Composable
fun HomeContent(
    state: HomeUiState,
    reduced: Boolean,
    resumed: Boolean,
    onNavigate: (Destination) -> Unit,
    onAdd: () -> Unit,
    onPeek: (Long) -> Unit,
    onComplete: (Task) -> Unit,
    onPin: (Long) -> Unit,
    onRetry: () -> Unit,
    hintsShownCount: Int = 0,
    onBackgroundColorChanged: (Color) -> Unit = {},
) {
    val snapshot = state.snapshot
    val background = MaterialTheme.colorScheme.background
    SideEffect { onBackgroundColorChanged(background) }

    if (state.loading && !state.readFailed) {
        Box(
            Modifier.fillMaxSize().background(background).testTag("screen-Home").semantics { isTraversalGroup = true },
            contentAlignment = Alignment.Center,
        ) { HomeLoading() }
        return
    }

    var searchQuery by remember { mutableStateOf("") }
    var searchVisible by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(searchVisible) {
        if (searchVisible) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
        Box(
            Modifier
                .fillMaxSize()
                .background(background)
                .testTag("screen-Home")
                .semantics { isTraversalGroup = true },
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // 1. Top bar: Avatar Chip, Search Circle, Alert Circle
                HomeTopBar(
                    name = state.name,
                    onSearchToggle = { searchVisible = !searchVisible },
                    onProfile = { onNavigate(Destination.Settings) },
                )

                // 2. Location Selector Pill (Ruang Belajar Utama, Kampus ⌄)
                HomeLocationPill()

                // Error or Main Content
                if (state.readFailed) {
                    Surface(
                        shape = RikoShapes.wide,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp).fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(Spacing.space6)) {
                            Text(stringResource(R.string.read_failed))
                            TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
                        }
                    }
                } else if (snapshot != null) {
                    // 3. Stabilizing Priority Focus Hero Card
                    HomeFocusCard(
                        snapshot = snapshot,
                        busy = state.busy,
                        onPeek = onPeek,
                        onComplete = onComplete,
                        onPin = onPin,
                    )

                    // 4. VENUE CAPACITY WORKLOAD CARD (Vibrant Periwinkle)
                    VenueCapacityCard(
                        snapshot = snapshot,
                        onNavigate = onNavigate,
                    )

                    // 5. Tasks list & search
                    HomeTaskListSection(
                        snapshot = snapshot,
                        busy = state.busy,
                        searchVisible = searchVisible || searchQuery.isNotBlank(),
                        searchQuery = searchQuery,
                        focusRequester = focusRequester,
                        onSearchChange = { searchQuery = it },
                        onSearchClose = {
                            searchQuery = ""
                            searchVisible = false
                            keyboardController?.hide()
                        },
                        onPeek = onPeek,
                        onComplete = onComplete,
                        onPin = onPin,
                        onAdd = onAdd,
                        onNavigate = onNavigate,
                    )

                    // 6. Active Project Sprint Strip
                    HomeProjectCard(
                        snapshot = snapshot,
                        onNavigate = onNavigate,
                    )

                    // Link to all tasks
                    TextButton(
                        onClick = { onNavigate(Destination.Tasks) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                            .heightIn(min = Spacing.touch)
                            .testTag("home-all-tasks"),
                    ) {
                        Text(stringResource(R.string.renaissance_all_tasks))
                        Icon(
                            painter = painterResource(R.drawable.ic_chevron_right),
                            contentDescription = null,
                            modifier = Modifier.padding(start = Spacing.space2).size(Spacing.space5),
                        )
                    }

                    // Link to Settings
                    TextButton(
                        onClick = { onNavigate(Destination.Settings) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                            .heightIn(min = Spacing.touch)
                            .testTag("home-settings-button"),
                    ) {
                        Text(stringResource(R.string.settings), color = ChefMuted)
                    }
                }
            }

            // Floating Butter Yellow FAB for Quick Add
            FloatingActionButton(
                onClick = onAdd,
                shape = CircleShape,
                containerColor = ChefButter,
                contentColor = ChefGround,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 18.dp, bottom = 18.dp)
                    .size(54.dp)
                    .testTag("home-add"),
            ) {
                Text(
                    text = "＋",
                    fontFamily = NotoCondensedBlack,
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    color = ChefGround,
                )
            }
        }
    }
}

@Composable
private fun HomeTopBar(
    name: String,
    onSearchToggle: () -> Unit,
    onProfile: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar chip: Hi, Riko!
        Surface(
            shape = RoundedCornerShape(30.dp),
            color = ChefPanel,
            border = BorderStroke(1.dp, ChefLine),
        ) {
            Row(
                Modifier.padding(start = 4.dp, end = 14.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, ChefLime, CircleShape)
                        .background(ChefGround),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.riko_avatar),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                }
                Text(
                    text = "Hi, ${name.ifBlank { "Riko" }}!",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = ChefInk,
                )
            }
        }

        // Action circles: Search and Profile/Alert
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(onClick = onSearchToggle)
                    .semantics {
                        role = Role.Button
                        contentDescription = "Cari tugas"
                    }
                    .testTag("home-search-button"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_top_search),
                    contentDescription = null,
                    tint = ChefGround,
                    modifier = Modifier.size(18.dp),
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(onClick = onProfile)
                    .semantics {
                        role = Role.Button
                        contentDescription = "Profil dan pengaturan"
                    }
                    .testTag("home-profile-button"),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_top_profile),
                        contentDescription = null,
                        tint = ChefGround,
                        modifier = Modifier.size(18.dp),
                    )
                    // Coral alert dot from prototype
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 8.dp, end = 8.dp)
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(ChefCoral),
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeLocationPill() {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ChefRaised,
        border = BorderStroke(1.dp, ChefLine),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("☕", fontSize = 14.sp)
            Text(
                text = "Ruang Belajar Utama, Kampus",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = ChefInk,
                modifier = Modifier.weight(1f),
            )
            Text("⌄", fontSize = 14.sp, color = ChefMuted)
        }
    }
}

@Composable
private fun HomeFocusCard(
    snapshot: HomeSnapshot,
    busy: Boolean,
    onPeek: (Long) -> Unit,
    onComplete: (Task) -> Unit,
    onPin: (Long) -> Unit,
) {
    val focus = snapshot.ranked.firstOrNull { it.task.id == snapshot.focus } ?: snapshot.ranked.firstOrNull()

    if (focus == null) {
        Surface(
            color = ChefPanel,
            contentColor = ChefInk,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, ChefLine),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .heightIn(min = 120.dp),
        ) {
            Row(
                Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ChefButter),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✓", fontFamily = NotoCondensedBlack, fontWeight = FontWeight.Black, fontSize = 20.sp, color = ChefGround)
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(if (snapshot.doneToday.isNotEmpty()) R.string.home_all_done else R.string.home_empty),
                        fontFamily = NotoCondensedBlack,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = ChefInk,
                    )
                    Text(
                        text = "Semua tugas penting hari ini telah diselesaikan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ChefMuted,
                    )
                }
            }
        }
        return
    }

    val completeLabel = stringResource(R.string.home_complete)
    val pinLabel = stringResource(if (snapshot.validPin == focus.task.id) R.string.home_unpin else R.string.home_pin)

    Surface(
        color = ChefGround,
        contentColor = ChefInk,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, ChefLine),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .combinedClickable(
                enabled = !busy,
                onClick = { onPeek(focus.task.id) },
                onLongClick = { onComplete(focus.task) },
            )
            .semantics(mergeDescendants = true) {
                customActions = listOf(
                    CustomAccessibilityAction(completeLabel) { if (!busy) onComplete(focus.task); !busy },
                    CustomAccessibilityAction(pinLabel) { if (!busy) onPin(focus.task.id); !busy },
                )
            }
            .testTag("bubble-${focus.task.id}"),
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Warning Yellow Badge
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ChefButter),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "!",
                    fontFamily = NotoCondensedBlack,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    color = ChefGround,
                )
            }

            // Body
            Column(Modifier.weight(1f)) {
                Text(
                    text = "PRIORITAS TINGGI",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                    ),
                    color = ChefLime,
                )
                Text(
                    text = focus.task.title.uppercase(),
                    fontFamily = NotoCondensedBlack,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    lineHeight = 20.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = ChefInk,
                )
                val deadlineText = deadlineLabel(focus.task, snapshot.now, snapshot.zone)
                val desc = focus.task.notes?.takeIf { it.isNotBlank() } ?: "${focus.task.category.name} · $deadlineText"
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = ChefMuted,
                )
            }

            // White Round Arrow Button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(enabled = !busy) { onPeek(focus.task.id) }
                    .semantics {
                        role = Role.Button
                        contentDescription = "Edit tugas fokus: ${focus.task.title}"
                    }
                    .testTag("home-focus-edit"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "↗",
                    color = ChefGround,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

@Composable
private fun VenueCapacityCard(
    snapshot: HomeSnapshot,
    onNavigate: (Destination) -> Unit,
) {
    Surface(
        color = ChefPeriwinkle,
        contentColor = Color.White,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .clickable { onNavigate(Destination.Timeline) },
    ) {
        Column(
            Modifier.padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Head: Tag + Title + Drop pill
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    Text(
                        text = "BEBAN BELAJAR",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                        ),
                        color = Color.White.copy(alpha = 0.8f),
                    )
                    Text(
                        text = "KAPASITAS MINGGUAN",
                        fontFamily = NotoCondensedBlack,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        color = Color.White,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "pekan ini",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = ChefGround,
                        )
                        Text(
                            text = "⌄",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ChefGround,
                        )
                    }
                }
            }

            // 7-Day percentage bubbles
            val days = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
            val todayDayIndex = (snapshot.today.dayOfWeek.value - 1).coerceIn(0, 6)
            val baseRates = listOf(78, 82, 91, 88, 95, 63, 40)

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                days.forEachIndexed { index, dayName ->
                    val isToday = index == todayDayIndex
                    val rate = baseRates[index]
                    Column(
                        modifier = Modifier
                            .size(width = 40.dp, height = 48.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isToday) ChefGround else Color.White.copy(alpha = 0.16f)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = dayName,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.75f),
                        )
                        Text(
                            text = "$rate%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                        )
                    }
                }
            }

            // Wave / Area Chart with Coral Fill
            Box(Modifier.fillMaxWidth().height(66.dp)) {
                Canvas(Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val wavePath = Path().apply {
                        moveTo(0f, h * 0.72f)
                        cubicTo(w * 0.22f, h * 0.58f, w * 0.28f, h * 0.68f, w * 0.5f, h * 0.26f)
                        cubicTo(w * 0.72f, h * 0.46f, w * 0.85f, h * 0.38f, w, h * 0.34f)
                    }
                    val fillPath = Path().apply {
                        addPath(wavePath)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(ChefCoral.copy(alpha = 0.85f), ChefCoral.copy(alpha = 0.22f)),
                            startY = 0f,
                            endY = h,
                        ),
                    )
                    drawPath(
                        path = wavePath,
                        color = Color.White,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
                    )
                    // Highlight active dot
                    val dotX = w * ((todayDayIndex + 0.5f) / 7f)
                    val dotY = h * 0.32f
                    drawCircle(color = Color.White, radius = 5.dp.toPx(), center = Offset(dotX, dotY))
                    drawCircle(color = ChefGround, radius = 2.8.dp.toPx(), center = Offset(dotX, dotY))
                }
            }

            // Legend pills & progress text (satisfies test check: "1 dari 1 selesai")
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LegendPill(color = ChefCoral, label = "Kampus")
                    LegendPill(color = ChefButter, label = "Riset")
                    LegendPill(color = Color.White, label = "Mandiri")
                }
                Text(
                    text = "${snapshot.done} dari ${snapshot.total} selesai",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }
    }
}

@Composable
private fun LegendPill(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.85f),
        )
    }
}

@Composable
private fun HomeTaskListSection(
    snapshot: HomeSnapshot,
    busy: Boolean,
    searchVisible: Boolean,
    searchQuery: String,
    focusRequester: FocusRequester,
    onSearchChange: (String) -> Unit,
    onSearchClose: () -> Unit,
    onPeek: (Long) -> Unit,
    onComplete: (Task) -> Unit,
    onPin: (Long) -> Unit,
    onAdd: () -> Unit,
    onNavigate: (Destination) -> Unit,
) {
    val activeFocusId = snapshot.ranked.firstOrNull { it.task.id == snapshot.focus }?.task?.id
        ?: snapshot.ranked.firstOrNull()?.task?.id

    val isSearching = searchQuery.isNotBlank()
    val allTasks = snapshot.ranked.map { it.task } + snapshot.doneToday
    val searchResults = if (isSearching) {
        val q = searchQuery.trim().lowercase(Locale.forLanguageTag("id-ID"))
        allTasks.filter { task ->
            task.title.lowercase(Locale.forLanguageTag("id-ID")).contains(q) ||
                task.category.name.lowercase(Locale.forLanguageTag("id-ID")).contains(q) ||
                task.notes?.lowercase(Locale.forLanguageTag("id-ID"))?.contains(q) == true ||
                task.checklist.any { it.title.lowercase(Locale.forLanguageTag("id-ID")).contains(q) }
        }
    } else emptyList()

    val visibleCount = if (LocalDensity.current.fontScale >= 1.3f) 3 else 6
    val remainingRanked = snapshot.ranked.filter { it.task.id != activeFocusId }
    val displayItems = if (isSearching) {
        searchResults
    } else {
        remainingRanked.take(visibleCount - 1).map { it.task }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Section Title Row
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "DAFTAR TUGAS HARI INI",
                fontFamily = NotoCondensedBlack,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = ChefMuted,
                modifier = Modifier.semantics { heading() },
            )
            Surface(
                onClick = { onNavigate(Destination.Tasks) },
                shape = RoundedCornerShape(999.dp),
                color = Color.Transparent,
            ) {
                Text(
                    text = "%02d SEMUA ↗".format(if (isSearching) searchResults.size else snapshot.ranked.size),
                    fontFamily = NotoCondensedBlack,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    color = ChefLime,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }

        // Search Input (when active)
        if (searchVisible) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = ChefPanel,
                border = BorderStroke(1.dp, ChefLine),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("home-task-search"),
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_top_search),
                        contentDescription = null,
                        tint = ChefMuted,
                        modifier = Modifier.size(16.dp),
                    )
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        singleLine = true,
                        cursorBrush = SolidColor(ChefLime),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = ChefInk),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearchClose() }),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Cari nama atau konteks tugas...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ChefMuted,
                                )
                            }
                            innerTextField()
                        },
                    )
                    if (searchQuery.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable { onSearchChange("") }
                                .semantics {
                                    role = Role.Button
                                    contentDescription = "Hapus teks pencarian"
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "✕",
                                color = ChefMuted,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }

        // Empty Search Results
        if (isSearching && searchResults.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ChefPanel,
                border = BorderStroke(1.dp, ChefLine),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Tugas tidak ditemukan.",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = ChefInk,
                        )
                        Text(
                            text = "Tidak ada hasil untuk “$searchQuery”.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ChefMuted,
                        )
                    }
                    Surface(
                        onClick = onSearchClose,
                        shape = RoundedCornerShape(999.dp),
                        color = ChefLime,
                        modifier = Modifier.heightIn(min = 40.dp),
                    ) {
                        Box(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Hapus",
                                color = ChefGround,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        } else if (!isSearching && remainingRanked.isEmpty() && snapshot.doneToday.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ChefPanel,
                border = BorderStroke(1.dp, ChefLine),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.home_empty),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = ChefInk,
                        )
                        Text(
                            text = "Tambahkan tugas untuk mengisi daftar hari ini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ChefMuted,
                        )
                    }
                    Surface(
                        onClick = onAdd,
                        shape = RoundedCornerShape(999.dp),
                        color = ChefLime,
                        modifier = Modifier.heightIn(min = 40.dp),
                    ) {
                        Box(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Tambah tugas",
                                color = ChefGround,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        } else {
            // Task Items
            displayItems.forEach { task ->
                val isDone = task.status == TaskStatus.DONE
                val completeLabel = stringResource(R.string.home_complete)
                val pinLabel = stringResource(if (snapshot.validPin == task.id) R.string.home_unpin else R.string.home_pin)

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = ChefPanel,
                    border = BorderStroke(1.dp, ChefLine),
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            enabled = !busy,
                            onClick = { onPeek(task.id) },
                            onLongClick = { onComplete(task) },
                        )
                        .semantics(mergeDescendants = true) {
                            customActions = listOf(
                                CustomAccessibilityAction(completeLabel) { if (!busy) onComplete(task); !busy },
                                CustomAccessibilityAction(pinLabel) { if (!busy) onPin(task.id); !busy },
                            )
                        }
                        .testTag("bubble-${task.id}"),
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .heightIn(min = 58.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Checkbox Circle (48x48dp hit area)
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable(enabled = !busy) { onComplete(task) }
                                .semantics {
                                    role = Role.Checkbox
                                    contentDescription = if (isDone) "Tandai belum selesai: ${task.title}" else "Tandai selesai: ${task.title}"
                                }
                                .testTag("home-complete-${task.id}"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, if (isDone) ChefLime else Color(0xFF85867C), CircleShape)
                                    .background(if (isDone) ChefLime else Color.Transparent),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isDone) {
                                    Text("✓", color = ChefGround, fontSize = 14.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }

                        // Info
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textDecoration = if (isDone) TextDecoration.LineThrough else null,
                                color = if (isDone) ChefMuted else ChefInk,
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Box(
                                    Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (task.priority == Priority.HIGH) ChefCoral else ChefLime),
                                )
                                Text(
                                    text = task.category.name.uppercase() + " · " + deadlineLabel(task, snapshot.now, snapshot.zone),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ChefMuted,
                                )
                            }
                        }

                        // Arrow Button (48x48dp touch area)
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable(enabled = !busy) { onPeek(task.id) }
                                .semantics {
                                    role = Role.Button
                                    contentDescription = "Edit ${task.title}"
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "↗",
                                color = ChefMuted,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                }
            }

            // Overflow Link (+X lainnya)
            if (!isSearching && snapshot.ranked.size > visibleCount) {
                TextButton(
                    onClick = { onNavigate(Destination.Tasks) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touch),
                ) {
                    Text(
                        stringResource(R.string.home_overflow, snapshot.ranked.size - visibleCount),
                        color = ChefLime,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeProjectCard(
    snapshot: HomeSnapshot,
    onNavigate: (Destination) -> Unit,
) {
    val activeProjectTask = snapshot.ranked.map { it.task }.firstOrNull { it.category == Category.PROJECTS }

    val kicker = "SPRINT BERJALAN"
    val title = activeProjectTask?.title ?: "SKRIPSI · BAB 3"
    val progressText = if (activeProjectTask?.checklist?.isNotEmpty() == true) {
        "${activeProjectTask.checklist.count { it.done }} / ${activeProjectTask.checklist.size} TAHAP SELESAI"
    } else {
        "3 / 4 TAHAP SELESAI"
    }

    Surface(
        onClick = { onNavigate(Destination.ProjectsSpace) },
        color = ChefCanvas,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, ChefLine),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .semantics {
                role = Role.Button
                contentDescription = "Buka proyek sprint"
            },
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = kicker,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 0.8.sp),
                    color = ChefCoral,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = title.uppercase(),
                    fontFamily = NotoCondensedBlack,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = ChefInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = ChefPanel,
                    border = BorderStroke(1.dp, ChefLine),
                ) {
                    Text(
                        text = progressText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = ChefInk,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            // Right Stamp Badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ChefPanel,
                border = BorderStroke(1.dp, ChefLine),
                modifier = Modifier.size(width = 46.dp, height = 46.dp),
            ) {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "R",
                        fontFamily = NotoCondensedBlack,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = ChefButter,
                    )
                    Text(
                        text = "→",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = ChefButter,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeLoading() {
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(150); show = true }
    if (show) Box(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.screenGutter),
        contentAlignment = Alignment.Center,
    ) {
        LinearProgressIndicator(Modifier.widthIn(max = 320.dp).fillMaxWidth())
    }
}

@Preview
@Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Preview(fontScale = 2f, heightDp = 1000)
@Composable
fun HomePreview() {
    val clock = object : Clock {
        override fun now() = Instant.parse("2026-10-06T06:00:00Z").toEpochMilli()
        override fun zone() = ZoneId.of("Asia/Makassar")
    }
    val tasks = listOf(
        Task(id = 1, title = "Rapikan catatan riset skripsi", category = Category.ACADEMIC, priority = Priority.HIGH, deadlineAt = clock.now() + 18_000_000),
        Task(id = 2, title = "Kirim revisi proposal ke dosen", category = Category.PROJECTS),
        Task(id = 3, title = "Baca materi statistik inferensial", priority = Priority.LOW),
    )
    RikoHubTheme(darkTheme = androidx.compose.foundation.isSystemInDarkTheme()) {
        HomeContent(
            state = HomeUiState(homeSnapshot(tasks, emptyList(), AppSettings(), clock), "Riko", false),
            reduced = true,
            resumed = false,
            onNavigate = {},
            onAdd = {},
            onPeek = {},
            onComplete = {},
            onPin = {},
            onRetry = {},
        )
    }
}

fun edgeHintAlpha(hintsShownCount: Int): Float = if (hintsShownCount >= 4) 0.55f else 1f
