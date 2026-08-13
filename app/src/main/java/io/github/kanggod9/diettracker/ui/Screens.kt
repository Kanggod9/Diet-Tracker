package io.github.kanggod9.diettracker.ui
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import java.time.format.TextStyle
import io.github.kanggod9.diettracker.domain.MealType
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.FilterChip
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.BakeryDining
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ArrowDropUp
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.kanggod9.diettracker.domain.DailyTotals
import io.github.kanggod9.diettracker.domain.EntryKind
import io.github.kanggod9.diettracker.domain.FoodScoreCalculator
import io.github.kanggod9.diettracker.domain.JournalEntry
import io.github.kanggod9.diettracker.domain.NutrientAggregate
import io.github.kanggod9.diettracker.domain.NutrientAggregator
import io.github.kanggod9.diettracker.domain.NutrientKey
import io.github.kanggod9.diettracker.domain.QuickFood
import io.github.kanggod9.diettracker.domain.Suggestion
import io.github.kanggod9.diettracker.domain.localDate
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import java.util.UUID

internal enum class Screen(val label: String, val icon: ImageVector) {
    LOGS("Logs", Icons.Outlined.Today),
    TARGET("Target", Icons.Outlined.TrackChanges),
    SETTINGS("Settings", Icons.Outlined.Settings),
}

internal val dashboardNutrientOrder = listOf(
    NutrientKey.ENERGY, NutrientKey.PROTEIN, NutrientKey.TOTAL_CARBOHYDRATE,
    NutrientKey.TOTAL_FAT, NutrientKey.SATURATED_FAT, NutrientKey.DIETARY_FIBER,
    NutrientKey.TOTAL_SUGAR, NutrientKey.ADDED_SUGAR, NutrientKey.SODIUM,
    NutrientKey.CHOLESTEROL, NutrientKey.CAFFEINE, NutrientKey.WATER,
) + NutrientKey.entries.filterNot {
    it in setOf(
        NutrientKey.ENERGY, NutrientKey.PROTEIN, NutrientKey.TOTAL_CARBOHYDRATE,
        NutrientKey.TOTAL_FAT, NutrientKey.SATURATED_FAT, NutrientKey.DIETARY_FIBER,
        NutrientKey.TOTAL_SUGAR, NutrientKey.ADDED_SUGAR, NutrientKey.SODIUM,
        NutrientKey.CHOLESTEROL, NutrientKey.CAFFEINE, NutrientKey.WATER,
    )
}

internal fun dashboardPages(
    order: List<NutrientKey> = dashboardNutrientOrder,
): List<List<NutrientKey>> =
    listOf(order.take(3)) + order.drop(3).chunked(6)

internal fun dashboardNutrientLabel(key: NutrientKey): String = when (key) {
    NutrientKey.MONOUNSATURATED_FAT -> "MUFA"
    NutrientKey.POLYUNSATURATED_FAT -> "PUFA"
    NutrientKey.UNSATURATED_FAT -> "Unsat. fat"
    else -> key.label
}

internal data class NutrientProgressState(
    val fill: Float,
    val exceeded: Boolean,
)

internal fun nutrientProgressState(value: Double?, target: Double?): NutrientProgressState {
    val raw = if (value != null && target != null && target > 0.0) value / target else 0.0
    return NutrientProgressState(
        fill = raw.coerceIn(0.0, 1.0).toFloat(),
        exceeded = raw > 1.0,
    )
}

internal fun mealJournalGroups(
    entries: List<JournalEntry>,
): List<Pair<MealType, List<JournalEntry>>> =
    MealType.entries.mapNotNull { meal ->
        entries.filter { it.mealType == meal }
            .takeIf { it.isNotEmpty() }
            ?.let { meal to it }
    }

@Composable
internal fun LogsScreen(
    entries: List<JournalEntry>,
    quickFoods: List<QuickFood>,
    selectedDate: LocalDate,
    targets: Map<NutrientKey, Double>,
    suggestions: List<Suggestion>,
    onDateSelected: (LocalDate) -> Unit,
    onLog: () -> Unit,
    onFoodScoreSelected: () -> Unit,
    onNutrientSelected: (NutrientKey) -> Unit,
    onEdit: (JournalEntry) -> Unit,
    onDelete: (JournalEntry) -> Unit,
) {
    val current = entries.filter { it.localDate() == selectedDate }
    val aggregate = NutrientAggregator.aggregate(current.map { it.nutrients })
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { DateNavigator(entries, selectedDate, onDateSelected) }
        item { DailyNutrientDashboard(aggregate, targets, onFoodScoreSelected, onNutrientSelected) }
        item {
            Button(
                onClick = onLog,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(28.dp),
            ) { Text("+ Log", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        }
        item { Text("Journal", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item { MealJournal(current, onEdit, onDelete) }
        if (quickFoods.isNotEmpty()) {
            item {
                Text(
                    "${quickFoods.size} quick food${if (quickFoods.size == 1) "" else "s"} available from + Log",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        if (suggestions.isNotEmpty()) {
            item { SectionCard("Guidance") { suggestions.take(3).forEach { Text(it.message) } } }
        }
    }
}

@Composable
private fun DailyNutrientDashboard(
    aggregate: NutrientAggregate,
    targets: Map<NutrientKey, Double>,
    onFoodScoreSelected: () -> Unit,
    onNutrientSelected: (NutrientKey) -> Unit,
) {
    val pages = dashboardPages()
    val pageCount = pages.size
    val pagerState = rememberPagerState(pageCount = { pageCount })
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(244.dp)
                .semantics { contentDescription = "Daily nutrient dashboard" },
            pageSpacing = 10.dp,
        ) { page ->
            if (page == 0) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ScoreTile(aggregate, onFoodScoreSelected, Modifier.weight(1f).fillMaxHeight())
                    Column(
                        Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        pages.first().forEach { key ->
                            NutrientDashboardTile(
                                key, aggregate.nutrients[key], targets[key],
                                { onNutrientSelected(key) }, Modifier.weight(1f),
                            )
                        }
                        repeat((3 - pages.first().size).coerceAtLeast(0)) { Spacer(Modifier.weight(1f)) }
                    }
                }
            } else {
                val nutrients = pages[page]
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) { row ->
                        Row(
                            Modifier.weight(1f).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            repeat(2) { column ->
                                val key = nutrients.getOrNull(row * 2 + column)
                                if (key == null) Spacer(Modifier.weight(1f)) else {
                                    NutrientDashboardTile(
                                        key, aggregate.nutrients[key], targets[key],
                                        { onNutrientSelected(key) }, Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        if (pageCount > 1) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                repeat(pageCount) { index ->
                    Surface(
                        modifier = Modifier.padding(horizontal = 3.dp)
                            .width(if (pagerState.currentPage == index) 20.dp else 8.dp).height(8.dp),
                        shape = CircleShape,
                        color = if (pagerState.currentPage == index) DarkTurquoise else Color(0xFFD8DEDF),
                    ) {}
                }
            }
        }
    }
}

@Composable
private fun ScoreTile(aggregate: NutrientAggregate, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val score = FoodScoreCalculator.calculate(aggregate.nutrients).score
    Card(
        modifier = modifier.semantics { contentDescription = "Food score tile" }.clickable(onClick = onClick),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F7F6)),
    ) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(Modifier.size(128.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { (score ?: 0) / 100f },
                    modifier = Modifier.fillMaxSize(),
                    color = Turquoise,
                    strokeWidth = 13.dp,
                    trackColor = Color.White,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        score?.toString() ?: "--",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text("Score", style = MaterialTheme.typography.labelLarge)
                }
            }
            Text("Food score", color = DarkTurquoise, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun NutrientDashboardTile(
    key: NutrientKey,
    value: Double?,
    target: Double?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = nutrientProgressState(value, target)
    val exceeded = progress.exceeded
    val fill = progress.fill
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFFE9EFF2))
            .semantics { contentDescription = "Nutrient tile ${key.name}" }
            .clickable(onClick = onClick),
    ) {
        Box(
            Modifier.fillMaxWidth(fill).fillMaxHeight().align(Alignment.CenterStart)
                .background((if (exceeded) FlameOrange else Turquoise).copy(alpha = 0.72f)),
        )
        Row(
            Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.72f)) {
                Icon(
                    nutrientIcon(key), null, modifier = Modifier.padding(8.dp).size(21.dp),
                    tint = if (exceeded) Color(0xFFA44311) else DarkTurquoise,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(dashboardNutrientLabel(key), style = MaterialTheme.typography.labelLarge, maxLines = 1)
                Text(
                    value?.let { "${formatValue(it)} ${key.unit}" } ?: "--",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }
    }
}

private fun nutrientIcon(key: NutrientKey): ImageVector = when (key) {
    NutrientKey.ENERGY -> Icons.Outlined.LocalFireDepartment
    NutrientKey.PROTEIN -> Icons.Outlined.FitnessCenter
    NutrientKey.TOTAL_CARBOHYDRATE -> Icons.Outlined.BakeryDining
    NutrientKey.TOTAL_FAT, NutrientKey.SATURATED_FAT, NutrientKey.MONOUNSATURATED_FAT,
    NutrientKey.POLYUNSATURATED_FAT, NutrientKey.UNSATURATED_FAT,
    NutrientKey.TRANS_FAT -> Icons.Outlined.WaterDrop
    NutrientKey.DIETARY_FIBER -> Icons.Outlined.Grass
    NutrientKey.WATER -> Icons.Outlined.LocalDrink
    NutrientKey.SODIUM, NutrientKey.CALCIUM, NutrientKey.IRON,
    NutrientKey.MAGNESIUM, NutrientKey.POTASSIUM, NutrientKey.ZINC -> Icons.Outlined.Science
    else -> Icons.Outlined.Circle
}

@Composable
private fun MealJournal(
    entries: List<JournalEntry>,
    onEdit: (JournalEntry) -> Unit,
    onDelete: (JournalEntry) -> Unit,
) {
    if (entries.isEmpty()) {
        EmptyCard("Nothing logged yet")
        return
    }
    var selectedMeal by remember(entries) { mutableStateOf<MealType?>(null) }
    val expanded = remember { mutableStateMapOf<MealType, Boolean>() }
    val groups = mealJournalGroups(entries)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = selectedMeal == null,
                    onClick = { selectedMeal = null },
                    label = { Text("All") },
                )
            }
            items(groups, key = { it.first.name }) { (meal, _) ->
                FilterChip(
                    selected = selectedMeal == meal,
                    onClick = { selectedMeal = meal },
                    label = { Text(meal.displayName) },
                )
            }
        }
        groups.filter { selectedMeal == null || it.first == selectedMeal }.forEach { (meal, items) ->
            MealGroupCard(
                meal, items, expanded[meal] == true,
                { expanded[meal] = expanded[meal] != true }, onEdit, onDelete,
            )
        }
    }
}

@Composable
private fun MealGroupCard(
    meal: MealType,
    entries: List<JournalEntry>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onEdit: (JournalEntry) -> Unit,
    onDelete: (JournalEntry) -> Unit,
) {
    val calories = entries.mapNotNull { it.nutrients[NutrientKey.ENERGY] }.sum()
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE9EFF2)),
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(meal.displayName, style = MaterialTheme.typography.headlineSmall)
                    Text("${entries.size} item${if (entries.size == 1) "" else "s"}", color = Color.DarkGray)
                }
                Text(
                    if (calories > 0.0) "${formatValue(calories)} kcal" else "--",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                )
                Icon(
                    if (expanded) Icons.Outlined.ArrowDropUp else Icons.Outlined.ArrowDropDown,
                    if (expanded) "Collapse" else "Expand",
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            if (expanded) {
                entries.forEach { JournalEntryRow(it, onEdit, onDelete) }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun JournalEntryRow(
    entry: JournalEntry,
    onEdit: (JournalEntry) -> Unit,
    onDelete: (JournalEntry) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (entry.kind == EntryKind.DRINK) Icons.Outlined.LocalDrink else Icons.Outlined.Restaurant,
            null, tint = DarkTurquoise, modifier = Modifier.size(22.dp),
        )
        Column(Modifier.padding(horizontal = 10.dp).weight(1f)) {
            Text(entry.name, fontWeight = FontWeight.Medium)
            Text(entry.servingDescription, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
        }
        Text(
            entry.nutrients[NutrientKey.ENERGY]?.let { "${formatValue(it)} kcal" } ?: "--",
            style = MaterialTheme.typography.bodyMedium,
        )
        IconButton(onClick = { onEdit(entry) }) { Icon(Icons.Outlined.Edit, "Edit") }
        IconButton(onClick = { onDelete(entry) }) { Icon(Icons.Outlined.Delete, "Delete") }
    }
}

internal fun calendarFoodScores(
    entries: List<JournalEntry>,
    zone: ZoneId = ZoneId.systemDefault(),
): Map<LocalDate, Int?> = entries.groupBy { it.localDate(zone) }.mapValues { (_, items) ->
    FoodScoreCalculator.calculate(NutrientAggregator.aggregate(items.map { it.nutrients }).nutrients).score
}

@Composable
private fun DateNavigator(
    entries: List<JournalEntry>,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var visibleMonth by remember(selectedDate) { mutableStateOf(YearMonth.from(selectedDate)) }
    val dailyScores = remember(entries) { calendarFoodScores(entries) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { expanded = !expanded }) {
                Text(selectedDate.format(DateTimeFormatter.ofPattern("MMM d")))
                Icon(if (expanded) Icons.Outlined.ArrowDropUp else Icons.Outlined.ArrowDropDown, null)
            }
            Spacer(Modifier.weight(1f))
            if (selectedDate != LocalDate.now()) {
                OutlinedButton(onClick = { onDateSelected(LocalDate.now()) }) { Text("Today") }
            }
        }
        if (expanded) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { visibleMonth = visibleMonth.minusMonths(1) }) {
                    Icon(Icons.Outlined.ChevronLeft, "Previous month")
                }
                Text(
                    visibleMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(onClick = { visibleMonth = visibleMonth.plusMonths(1) }) {
                    Icon(Icons.Outlined.ChevronRight, "Next month")
                }
            }
            CalendarMonth(visibleMonth, selectedDate, dailyScores) {
                onDateSelected(it)
                visibleMonth = YearMonth.from(it)
            }
        } else {
            CalendarWeek(selectedDate, dailyScores, onDateSelected)
        }
    }
}

@Composable
private fun CalendarWeek(
    selectedDate: LocalDate,
    dailyScores: Map<LocalDate, Int?>,
    onDateSelected: (LocalDate) -> Unit,
) {
    val start = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        (0L..6L).forEach { offset ->
            val date = start.plusDays(offset)
            CalendarDay(date, selectedDate, true, true, dailyScores[date], onDateSelected)
        }
    }
}

@Composable
private fun CalendarMonth(
    month: YearMonth,
    selectedDate: LocalDate,
    dailyScores: Map<LocalDate, Int?>,
    onDateSelected: (LocalDate) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach {
            Text(it, modifier = Modifier.width(44.dp), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
    val first = month.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    repeat(6) { week ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            repeat(7) { day ->
                val date = first.plusDays((week * 7 + day).toLong())
                CalendarDay(date, selectedDate, false, YearMonth.from(date) == month, dailyScores[date], onDateSelected)
            }
        }
    }
}

@Composable
private fun CalendarDay(
    date: LocalDate,
    selectedDate: LocalDate,
    showWeekday: Boolean,
    inMonth: Boolean,
    foodScore: Int?,
    onDateSelected: (LocalDate) -> Unit,
) {
    val selected = date == selectedDate
    Column(
        modifier = Modifier.width(44.dp)
            .alpha(if (inMonth) 1f else 0.35f)
            .semantics { contentDescription = "Calendar day $date, Food Score ${foodScore?.toString() ?: "unavailable"}" }
            .clickable { onDateSelected(date) },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (showWeekday) {
            Text(
                date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.titlecase() },
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) Sage else Color.Gray,
            )
        }
        Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(color = Color(0xFFE1E6E2), style = Stroke(3.dp.toPx()))
                val progress = ((foodScore ?: 0) / 100f).coerceIn(0f, 1f)
                if (progress > 0f) {
                    drawArc(
                        color = Turquoise,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(3.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
            }
            Surface(
                shape = CircleShape,
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            ) {
                Text(
                    date.dayOfMonth.toString(),
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
internal fun NutrientProgressRow(
    key: NutrientKey,
    value: Double?,
    target: Double?,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onValueClick: (() -> Unit)? = null,
) {
    val progress = if (value != null && target != null && target > 0.0) {
        (value / target).coerceIn(0.0, 1.0).toFloat()
    } else 0f
    Column(modifier, verticalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                key.label,
                modifier = Modifier.weight(1f),
                style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleSmall,
                fontWeight = if (compact) FontWeight.Normal else FontWeight.SemiBold,
            )
            val label = value?.let { "${formatValue(it)} ${key.unit}" } ?: "--"
            if (onValueClick != null) {
                Text(
                    label,
                    modifier = Modifier.clickable(onClick = onValueClick).padding(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            } else {
                Text(label, fontWeight = FontWeight.SemiBold)
            }
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(if (compact) 6.dp else 9.dp),
            color = if (progress >= 1f) DeepSage else Sage,
            trackColor = Color.White.copy(alpha = if (compact) 0.8f else 1f),
        )
        if (!compact) {
            Text(
                target?.let { "Target ${formatValue(it)} ${key.unit}" } ?: "Target --",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
            )
        }
    }
}

internal enum class HistoryPeriod(val label: String) {
    DAY("D"), WEEK("W"), MONTH("M"), THREE_MONTHS("3M"), YEAR("Y"),
}

internal data class HistoryBucket(val label: String, val value: Double)

internal fun nutrientHistoryBuckets(
    entries: List<JournalEntry>,
    key: NutrientKey,
    period: HistoryPeriod,
    anchor: LocalDate,
    zone: ZoneId = ZoneId.systemDefault(),
): List<HistoryBucket> {
    val withValue = entries.mapNotNull { entry ->
        entry.nutrients[key]?.let { Triple(entry, entry.localDate(zone), it) }
    }
    return when (period) {
        HistoryPeriod.DAY -> (0..23).map { hour ->
            val value = withValue.filter {
                it.second == anchor && it.first.loggedAt.atZone(zone).hour == hour
            }.sumOf { it.third }
            val label = when (hour) {
                0 -> "12AM"
                12 -> "12PM"
                in 1..11 -> "${hour}AM"
                else -> "${hour - 12}PM"
            }
            HistoryBucket(label, value)
        }
        HistoryPeriod.WEEK -> {
            val start = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            (0L..6L).map { offset ->
                val date = start.plusDays(offset)
                HistoryBucket(
                    date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    withValue.filter { it.second == date }.sumOf { it.third },
                )
            }
        }
        HistoryPeriod.MONTH -> {
            val month = YearMonth.from(anchor)
            (1..month.lengthOfMonth()).map { day ->
                val date = month.atDay(day)
                HistoryBucket(day.toString(), withValue.filter { it.second == date }.sumOf { it.third })
            }
        }
        HistoryPeriod.THREE_MONTHS -> {
            val end = YearMonth.from(anchor)
            (2 downTo 0).map { offset ->
                val month = end.minusMonths(offset.toLong())
                HistoryBucket(
                    month.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    withValue.filter { YearMonth.from(it.second) == month }.sumOf { it.third },
                )
            }
        }
        HistoryPeriod.YEAR -> (1..12).map { monthNumber ->
            val month = YearMonth.of(anchor.year, monthNumber)
            HistoryBucket(
                month.month.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                withValue.filter { YearMonth.from(it.second) == month }.sumOf { it.third },
            )
        }
    }
}

internal data class FoodScoreHistoryBucket(
    val label: String,
    val average: Double?,
    val scoreCount: Int,
)

internal data class FoodScorePeriodSummary(
    val average: Double?,
    val scoreCount: Int,
)

private fun aggregateFoodScore(entries: List<JournalEntry>): Double? =
    entries.takeIf { it.isNotEmpty() }
        ?.let { NutrientAggregator.aggregate(it.map(JournalEntry::nutrients)).nutrients }
        ?.let(FoodScoreCalculator::calculate)
        ?.score
        ?.toDouble()

internal fun foodScoreHistoryBuckets(
    entries: List<JournalEntry>,
    period: HistoryPeriod,
    anchor: LocalDate,
    zone: ZoneId = ZoneId.systemDefault(),
): List<FoodScoreHistoryBucket> {
    val byDate = entries.groupBy { it.localDate(zone) }
    fun daily(date: LocalDate): Double? = aggregateFoodScore(byDate[date].orEmpty())
    fun monthAverage(month: YearMonth): FoodScoreHistoryBucket {
        val values = byDate.keys.filter { YearMonth.from(it) == month }.mapNotNull(::daily)
        return FoodScoreHistoryBucket(
            month.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
            values.takeIf { it.isNotEmpty() }?.average(),
            values.size,
        )
    }
    return when (period) {
        HistoryPeriod.DAY -> entries
            .filter { it.localDate(zone) == anchor }
            .sortedBy { it.loggedAt }
            .map { entry ->
                val score = FoodScoreCalculator.calculate(entry.nutrients).score?.toDouble()
                FoodScoreHistoryBucket(
                    entry.loggedAt.atZone(zone).format(DateTimeFormatter.ofPattern("h:mm a")),
                    score,
                    if (score == null) 0 else 1,
                )
            }
        HistoryPeriod.WEEK -> {
            val start = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            (0L..6L).map { offset ->
                val date = start.plusDays(offset)
                val score = daily(date)
                FoodScoreHistoryBucket(
                    date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    score,
                    if (score == null) 0 else 1,
                )
            }
        }
        HistoryPeriod.MONTH -> {
            val month = YearMonth.from(anchor)
            (1..month.lengthOfMonth()).map { day ->
                val score = daily(month.atDay(day))
                FoodScoreHistoryBucket(day.toString(), score, if (score == null) 0 else 1)
            }
        }
        HistoryPeriod.THREE_MONTHS -> {
            val end = YearMonth.from(anchor)
            (2 downTo 0).map { monthAverage(end.minusMonths(it.toLong())) }
        }
        HistoryPeriod.YEAR -> (1..12).map { monthAverage(YearMonth.of(anchor.year, it)) }
    }
}

internal fun foodScorePeriodSummary(
    entries: List<JournalEntry>,
    period: HistoryPeriod,
    anchor: LocalDate,
    zone: ZoneId = ZoneId.systemDefault(),
): FoodScorePeriodSummary {
    val scores = entries.groupBy { it.localDate(zone) }
        .filterKeys { historyContains(period, anchor, it) }
        .values
        .mapNotNull(::aggregateFoodScore)
    return FoodScorePeriodSummary(scores.takeIf { it.isNotEmpty() }?.average(), scores.size)
}

@Composable
internal fun FoodScoreDetails(
    score: io.github.kanggod9.diettracker.domain.FoodScore,
    showBasis: Boolean = true,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (showBasis) Text(score.basis, style = MaterialTheme.typography.bodySmall)
        Text(
            "The score starts at 50. Encouraged nutrients add points; nutrients to limit deduct points.",
            style = MaterialTheme.typography.bodySmall,
        )
        if (score.score == null) {
            Text(score.unavailableReason ?: "Food Score is unavailable.", color = MaterialTheme.colorScheme.error)
        } else {
            Text(
                "${score.components.size} of ${score.components.size + score.missingComponents.size} score nutrients reported " +
                    "(${formatValue(score.completeness * 100.0)}% complete).",
                style = MaterialTheme.typography.bodySmall,
            )
            val additions = score.components.filter {
                it.direction == io.github.kanggod9.diettracker.domain.ScoreDirection.ENCOURAGE
            }
            val deductions = score.components.filter {
                it.direction == io.github.kanggod9.diettracker.domain.ScoreDirection.LIMIT
            }
            Text("Adds points", fontWeight = FontWeight.Bold, color = DarkTurquoise)
            if (additions.isEmpty()) Text("No encouraged nutrients were reported.")
            additions.forEach { component ->
                Text(component.explanation, style = MaterialTheme.typography.bodySmall)
            }
            Text("Deducts points", fontWeight = FontWeight.Bold, color = Color(0xFFA44311))
            if (deductions.isEmpty()) Text("No nutrients to limit were reported.")
            deductions.forEach { component ->
                Text(component.explanation, style = MaterialTheme.typography.bodySmall)
            }
            if (score.missingComponents.isNotEmpty()) {
                Text(
                    "Not reported: ${score.missingComponents.joinToString { it.label }}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.DarkGray,
                )
            }
        }
        Text(FoodScoreCalculator.DISCLAIMER, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
internal fun FoodScoreHistoryScreen(
    entries: List<JournalEntry>,
    initialDate: LocalDate,
    onBack: () -> Unit,
) {
    var period by remember { mutableStateOf(HistoryPeriod.DAY) }
    var anchor by remember(initialDate) { mutableStateOf(initialDate) }
    val buckets = remember(entries, period, anchor) { foodScoreHistoryBuckets(entries, period, anchor) }
    val summary = remember(entries, period, anchor) { foodScorePeriodSummary(entries, period, anchor) }
    val dailyEntries = remember(entries, period, anchor) {
        if (period == HistoryPeriod.DAY) entries.filter { it.localDate() == anchor }.sortedByDescending { it.loggedAt }
        else emptyList()
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") }
                Text("Food Score", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            }
        }
        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                HistoryPeriod.entries.forEachIndexed { index, value ->
                    SegmentedButton(
                        selected = value == period,
                        onClick = { period = value },
                        shape = SegmentedButtonDefaults.itemShape(index, HistoryPeriod.entries.size),
                        label = { Text(value.label) },
                    )
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(historyTitle(period, anchor), Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                IconButton(onClick = { anchor = shiftHistoryAnchor(anchor, period, -1) }) {
                    Icon(Icons.Outlined.ChevronLeft, "Previous period")
                }
                IconButton(
                    onClick = { anchor = shiftHistoryAnchor(anchor, period, 1) },
                    enabled = shiftHistoryAnchor(anchor, period, 1) <= LocalDate.now(),
                ) { Icon(Icons.Outlined.ChevronRight, "Next period") }
                IconButton(onClick = { anchor = LocalDate.now() }) {
                    Icon(Icons.Outlined.RestartAlt, "Current period")
                }
            }
        }
        item {
            Text(
                summary.average?.let { "${formatValue(it)} / 100" } ?: "-- / 100",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Light,
            )
            Text(
                if (period == HistoryPeriod.DAY) "Daily Food Score"
                else "Average from ${summary.scoreCount} scored day${if (summary.scoreCount == 1) "" else "s"}",
                color = Color.DarkGray,
            )
        }
        item {
            HistoryChart(
                buckets.map { HistoryBucket(it.label, it.average ?: 0.0) },
                target = null,
                fixedMaximum = 100.0,
            )
            Text(
                if (period == HistoryPeriod.DAY) "Each bar is one log's Food Score."
                else if (period in setOf(HistoryPeriod.WEEK, HistoryPeriod.MONTH)) "Each bar is one day's Food Score."
                else "Each bar is the average of scored days in that month.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.DarkGray,
            )
        }
        if (period == HistoryPeriod.DAY) {
            item { Text("Log nutrient contributions", style = MaterialTheme.typography.headlineSmall) }
            if (dailyEntries.isEmpty()) item { EmptyCard("No logs on this day") }
            else items(dailyEntries, key = { it.id }) { entry ->
                val score = FoodScoreCalculator.calculate(entry.nutrients)
                OutlinedCard(shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(entry.name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                            Text(score.score?.let { "$it / 100" } ?: "--", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            entry.loggedAt.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("h:mm a")),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray,
                        )
                        FoodScoreDetails(score, showBasis = false)
                    }
                }
            }
        }
    }
}

@Composable
internal fun NutrientHistoryScreen(
    key: NutrientKey,
    entries: List<JournalEntry>,
    initialDate: LocalDate,
    target: Double?,
    onBack: () -> Unit,
) {
    var period by remember { mutableStateOf(HistoryPeriod.DAY) }
    var anchor by remember(initialDate) { mutableStateOf(initialDate) }
    val buckets = remember(entries, key, period, anchor) {
        nutrientHistoryBuckets(entries, key, period, anchor)
    }
    val total = buckets.sumOf { it.value }
    val average = if (buckets.isEmpty()) 0.0 else total / buckets.size
    val visibleDetails = remember(entries, key, period, anchor) {
        entries.filter { entry ->
            entry.nutrients[key] != null && historyContains(period, anchor, entry.localDate())
        }.sortedByDescending { it.loggedAt }
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
                }
                Text(key.label, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            }
        }
        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                HistoryPeriod.entries.forEachIndexed { index, value ->
                    SegmentedButton(
                        selected = value == period,
                        onClick = { period = value },
                        shape = SegmentedButtonDefaults.itemShape(index, HistoryPeriod.entries.size),
                        label = { Text(value.label) },
                    )
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    historyTitle(period, anchor),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineSmall,
                )
                IconButton(onClick = { anchor = shiftHistoryAnchor(anchor, period, -1) }) {
                    Icon(Icons.Outlined.ChevronLeft, "Previous period")
                }
                IconButton(
                    onClick = { anchor = shiftHistoryAnchor(anchor, period, 1) },
                    enabled = shiftHistoryAnchor(anchor, period, 1) <= LocalDate.now(),
                ) { Icon(Icons.Outlined.ChevronRight, "Next period") }
                IconButton(onClick = { anchor = LocalDate.now() }) {
                    Icon(Icons.Outlined.RestartAlt, "Current period")
                }
            }
        }
        item {
            Text(
                if (period == HistoryPeriod.DAY) "${formatValue(total)} ${key.unit}"
                else "${formatValue(average)} ${key.unit} per ${historyAverageUnit(period)} (avg)",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Light,
            )
            if (period != HistoryPeriod.DAY) {
                Text("Total ${formatValue(total)} ${key.unit}", color = Color.DarkGray)
            }
        }
        item { HistoryChart(buckets, historyBucketTarget(target, period)) }
        item {
            Text(
                if (period == HistoryPeriod.DAY) "Entries" else "Entries in this period",
                style = MaterialTheme.typography.headlineSmall,
            )
        }
        if (visibleDetails.isEmpty()) {
            item { EmptyCard("No ${key.label.lowercase()} logged in this period") }
        } else {
            items(visibleDetails, key = { it.id }) { entry ->
                OutlinedCard(shape = RoundedCornerShape(20.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(entry.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                entry.loggedAt.atZone(ZoneId.systemDefault())
                                    .format(DateTimeFormatter.ofPattern("MMM d, h:mm a")),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.DarkGray,
                            )
                        }
                        Text(
                            "${formatValue(entry.nutrients[key] ?: 0.0)} ${key.unit}",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryChart(
    buckets: List<HistoryBucket>,
    target: Double?,
    fixedMaximum: Double? = null,
) {
    val maximum = fixedMaximum ?: maxOf(buckets.maxOfOrNull { it.value } ?: 0.0, target ?: 0.0, 1.0)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(250.dp)) {
            val gap = 4.dp.toPx()
            val barWidth = ((size.width - gap * (buckets.size - 1).coerceAtLeast(0)) /
                buckets.size.coerceAtLeast(1)).coerceAtLeast(2.dp.toPx())
            target?.let {
                val y = size.height - (it / maximum * size.height).toFloat()
                drawLine(
                    color = DarkTurquoise.copy(alpha = 0.55f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 2.dp.toPx(),
                )
            }
            buckets.forEachIndexed { index, bucket ->
                val barHeight = (bucket.value / maximum * size.height).toFloat()
                if (barHeight > 0f) {
                    drawRoundRect(
                        color = if (target != null && bucket.value > target) FlameOrange else Turquoise,
                        topLeft = Offset(index * (barWidth + gap), size.height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                    )
                }
            }
        }
        if (buckets.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(buckets.first().label, style = MaterialTheme.typography.labelSmall)
                Text(buckets[buckets.size / 2].label, style = MaterialTheme.typography.labelSmall)
                Text(buckets.last().label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private fun historyContains(period: HistoryPeriod, anchor: LocalDate, date: LocalDate): Boolean = when (period) {
    HistoryPeriod.DAY -> date == anchor
    HistoryPeriod.WEEK -> {
        val start = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        !date.isBefore(start) && !date.isAfter(start.plusDays(6))
    }
    HistoryPeriod.MONTH -> YearMonth.from(date) == YearMonth.from(anchor)
    HistoryPeriod.THREE_MONTHS -> {
        val end = YearMonth.from(anchor)
        val value = YearMonth.from(date)
        value >= end.minusMonths(2) && value <= end
    }
    HistoryPeriod.YEAR -> date.year == anchor.year
}

private fun shiftHistoryAnchor(anchor: LocalDate, period: HistoryPeriod, direction: Long): LocalDate = when (period) {
    HistoryPeriod.DAY -> anchor.plusDays(direction)
    HistoryPeriod.WEEK -> anchor.plusWeeks(direction)
    HistoryPeriod.MONTH -> anchor.plusMonths(direction)
    HistoryPeriod.THREE_MONTHS -> anchor.plusMonths(direction * 3)
    HistoryPeriod.YEAR -> anchor.plusYears(direction)
}

private fun historyTitle(period: HistoryPeriod, anchor: LocalDate): String = when (period) {
    HistoryPeriod.DAY -> anchor.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    HistoryPeriod.WEEK -> {
        val start = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        "${start.format(DateTimeFormatter.ofPattern("MMM d"))} - ${start.plusDays(6).format(DateTimeFormatter.ofPattern("MMM d"))}"
    }
    HistoryPeriod.MONTH -> anchor.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    HistoryPeriod.THREE_MONTHS -> {
        val end = YearMonth.from(anchor)
        "${end.minusMonths(2).format(DateTimeFormatter.ofPattern("MMM"))} - ${end.format(DateTimeFormatter.ofPattern("MMM yyyy"))}"
    }
    HistoryPeriod.YEAR -> anchor.year.toString()
}

private fun historyAverageUnit(period: HistoryPeriod): String = when (period) {
    HistoryPeriod.DAY, HistoryPeriod.WEEK, HistoryPeriod.MONTH -> "day"
    HistoryPeriod.THREE_MONTHS, HistoryPeriod.YEAR -> "month"
}

private fun historyBucketTarget(target: Double?, period: HistoryPeriod): Double? = when (period) {
    HistoryPeriod.DAY -> null
    HistoryPeriod.WEEK, HistoryPeriod.MONTH -> target
    HistoryPeriod.THREE_MONTHS, HistoryPeriod.YEAR -> target?.times(30.0)
}

@Composable
internal fun SectionCard(
    title: String,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    ElevatedCard {
        Column(Modifier.padding(18.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
internal fun EmptyCard(title: String) {
    OutlinedCard {
        Text(title, modifier = Modifier.padding(20.dp).fillMaxWidth(), fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun BusyOverlay(message: String) {
    Surface(color = Color.Black.copy(alpha = 0.35f), modifier = Modifier.fillMaxSize()) {
        Box(contentAlignment = Alignment.Center) {
            ElevatedCard {
                Column(
                    Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    CircularProgressIndicator()
                    Text(message)
                }
            }
        }
    }
}

internal fun List<JournalEntry>.toDailyTotals(zone: ZoneId = ZoneId.systemDefault()): List<DailyTotals> =
    groupBy { it.localDate(zone) }.map { (date, items) ->
        DailyTotals(date, NutrientAggregator.aggregate(items.map { it.nutrients }))
    }


internal fun formatValue(value: Double): String =
    if (value >= 100.0 || value % 1.0 == 0.0) String.format(Locale.US, "%.0f", value)
    else String.format(Locale.US, "%.1f", value)

internal fun QuickFood.toJournalEntry(): JournalEntry = JournalEntry(
    id = UUID.randomUUID().toString(),
    name = name,
    kind = kind,
    mealType = mealType,
    servingDescription = servingDescription,
    servingGrams = servingGrams,
    amount = amount,
    loggedAt = Instant.now(),
    nutrients = nutrients,
    note = "",
)

internal fun JournalEntry.toQuickFood(): QuickFood = QuickFood(
    id = UUID.randomUUID().toString(),
    name = name,
    kind = kind,
    mealType = mealType,
    servingDescription = servingDescription,
    servingGrams = servingGrams,
    amount = amount,
    nutrients = nutrients,
)
