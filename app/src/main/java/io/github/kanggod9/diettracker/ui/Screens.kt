package io.github.kanggod9.diettracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateOf
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
import io.github.kanggod9.diettracker.domain.GuidanceProfiles
import io.github.kanggod9.diettracker.domain.JournalEntry
import io.github.kanggod9.diettracker.domain.NutrientAggregate
import io.github.kanggod9.diettracker.domain.NutrientAggregator
import io.github.kanggod9.diettracker.domain.NutrientKey
import io.github.kanggod9.diettracker.domain.QuickFood
import io.github.kanggod9.diettracker.domain.Suggestion
import io.github.kanggod9.diettracker.domain.SuggestionEngine
import io.github.kanggod9.diettracker.domain.TrendAnalyzer
import io.github.kanggod9.diettracker.domain.TrendWindow
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
    ANALYSIS("Analysis", Icons.Outlined.Insights),
    TARGET("Target", Icons.Outlined.TrackChanges),
    SETTINGS("Settings", Icons.Outlined.Settings),
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
    onPhoto: () -> Unit,
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
        item { DateNavigator(entries, selectedDate, targets[NutrientKey.ENERGY], onDateSelected) }
        item { NutrientHero(aggregate, targets) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onLog, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Restaurant, null)
                    Text(" Text / USDA", Modifier.padding(start = 6.dp))
                }
                OutlinedButton(onClick = onPhoto, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.PhotoCamera, null)
                    Text(" Photo", Modifier.padding(start = 6.dp))
                }
            }
        }
        item { Text("Daily nutrients", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
        item {
            ElevatedCard {
                Column(
                    Modifier.padding(18.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    targets.keys.sortedBy { it.ordinal }.forEach { key ->
                        NutrientProgressRow(key, aggregate.nutrients[key], targets[key])
                    }
                }
            }
        }
        if (quickFoods.isNotEmpty()) {
            item { Text("${quickFoods.size} quick food${if (quickFoods.size == 1) "" else "s"}", color = Color.Gray) }
        }
        if (suggestions.isNotEmpty()) {
            item {
                SectionCard("Guidance") {
                    suggestions.take(3).forEach { Text(it.message) }
                }
            }
        }
        item { Text("Journal", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
        if (current.isEmpty()) item { EmptyCard("Nothing logged yet") }
        items(current, key = { it.id }) { EntryCard(it, onEdit, onDelete) }
    }
}

@Composable
private fun DateNavigator(
    entries: List<JournalEntry>,
    selectedDate: LocalDate,
    energyTarget: Double?,
    onDateSelected: (LocalDate) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var visibleMonth by remember(selectedDate) { mutableStateOf(YearMonth.from(selectedDate)) }
    val dailyEnergy = remember(entries) {
        entries.groupBy { it.localDate() }.mapValues { (_, items) ->
            NutrientAggregator.aggregate(items.map { it.nutrients }).nutrients[NutrientKey.ENERGY]
        }
    }
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
            CalendarMonth(visibleMonth, selectedDate, dailyEnergy, energyTarget) {
                onDateSelected(it)
                visibleMonth = YearMonth.from(it)
            }
        } else {
            CalendarWeek(selectedDate, dailyEnergy, energyTarget, onDateSelected)
        }
    }
}

@Composable
private fun CalendarWeek(
    selectedDate: LocalDate,
    dailyEnergy: Map<LocalDate, Double?>,
    energyTarget: Double?,
    onDateSelected: (LocalDate) -> Unit,
) {
    val start = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        (0L..6L).forEach { offset ->
            val date = start.plusDays(offset)
            CalendarDay(date, selectedDate, true, true, dailyEnergy[date], energyTarget, onDateSelected)
        }
    }
}

@Composable
private fun CalendarMonth(
    month: YearMonth,
    selectedDate: LocalDate,
    dailyEnergy: Map<LocalDate, Double?>,
    energyTarget: Double?,
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
                CalendarDay(date, selectedDate, false, YearMonth.from(date) == month, dailyEnergy[date], energyTarget, onDateSelected)
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
    energy: Double?,
    energyTarget: Double?,
    onDateSelected: (LocalDate) -> Unit,
) {
    val selected = date == selectedDate
    Column(
        modifier = Modifier.width(44.dp).alpha(if (inMonth) 1f else 0.35f).clickable { onDateSelected(date) },
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
                val progress = if (energyTarget != null && energyTarget > 0.0) {
                    ((energy ?: 0.0) / energyTarget).coerceIn(0.0, 1.0).toFloat()
                } else 0f
                if (progress > 0f) {
                    drawArc(
                        color = Sage,
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
private fun NutrientHero(
    aggregate: NutrientAggregate,
    targets: Map<NutrientKey, Double>,
) {
    val energy = aggregate.nutrients[NutrientKey.ENERGY]
    val score = FoodScoreCalculator.calculate(aggregate.nutrients).score
    ElevatedCard(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Mint),
    ) {
        Row(
            Modifier.padding(20.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(104.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { (score ?: 0) / 100f },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 10.dp,
                        trackColor = Color.White.copy(alpha = 0.75f),
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(score?.toString() ?: "--", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("Score", style = MaterialTheme.typography.labelMedium)
                    }
                }
                Text("Food score", color = DeepSage, style = MaterialTheme.typography.labelLarge)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Logged energy", color = DeepSage)
                Text(
                    energy?.let { "${it.toInt()} kcal" } ?: "--",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                listOf(
                    NutrientKey.ENERGY,
                    NutrientKey.PROTEIN,
                    NutrientKey.TOTAL_CARBOHYDRATE,
                    NutrientKey.TOTAL_FAT,
                ).forEach { key ->
                    NutrientProgressRow(key, aggregate.nutrients[key], targets[key], compact = true)
                }
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

@Composable
private fun EntryCard(
    entry: JournalEntry,
    onEdit: (JournalEntry) -> Unit,
    onDelete: (JournalEntry) -> Unit,
) {
    Card {
        Row(Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(14.dp), color = Mint) {
                Icon(
                    if (entry.kind == EntryKind.DRINK) Icons.Outlined.LocalDrink else Icons.Outlined.Restaurant,
                    null,
                    tint = Sage,
                    modifier = Modifier.padding(10.dp).size(24.dp),
                )
            }
            Column(Modifier.padding(horizontal = 12.dp).weight(1f)) {
                Text(entry.name, fontWeight = FontWeight.SemiBold)
                Text("${entry.mealType.displayName} · ${entry.servingDescription}", style = MaterialTheme.typography.bodySmall)
                Text(entry.nutrients[NutrientKey.ENERGY]?.let { "${it.toInt()} kcal" } ?: "--")
            }
            IconButton(onClick = { onEdit(entry) }) { Icon(Icons.Outlined.Edit, "Edit") }
            IconButton(onClick = { onDelete(entry) }) { Icon(Icons.Outlined.Delete, "Delete") }
        }
    }
}

@Composable
internal fun AnalysisScreen(entries: List<JournalEntry>) {
    var window by remember { mutableStateOf(TrendWindow.DAYS_7) }
    val allDays = entries.toDailyTotals()
    val visibleDays = allDays.forWindow(window, LocalDate.now())
    val trend = TrendAnalyzer.summarize(allDays, window, LocalDate.now())
    val today = NutrientAggregator.aggregate(entries.filter { it.localDate() == LocalDate.now() }.map { it.nutrients })
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("Analysis", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold) }
        item {
            SingleChoiceSegmentedButtonRow {
                TrendWindow.entries.forEachIndexed { index, value ->
                    SegmentedButton(
                        selected = value == window,
                        onClick = { window = value },
                        shape = SegmentedButtonDefaults.itemShape(index, TrendWindow.entries.size),
                    ) { Text(value.displayLabel()) }
                }
            }
        }
        item {
            SectionCard("Energy") {
                EnergyBars(visibleDays)
                Text("${trend.daysWithEntries} logged day${if (trend.daysWithEntries == 1) "" else "s"}")
            }
        }
        item {
            SectionCard("Daily averages") {
                val visible = NutrientKey.entries.filter { trend.dailyAverage[it] != null }
                if (visible.isEmpty()) Text("--")
                visible.take(16).forEach { key ->
                    Text("${key.label}: ${"%.1f".format(trend.dailyAverage[key])} ${key.unit}")
                }
            }
        }
        GuidanceProfiles.all.forEach { profile ->
            val guidance = SuggestionEngine.generate(today, trend, profile)
            item {
                SectionCard(profile.region.displayName) {
                    if (guidance.isEmpty()) Text("--")
                    guidance.take(4).forEach { suggestion ->
                        Text(suggestion.message)
                        HorizontalDivider()
                    }
                    Text(profile.disclaimer, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { Text("Daily summaries", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        items(visibleDays.sortedByDescending { it.date }, key = { it.date.toString() }) { day ->
            OutlinedCard {
                Column(Modifier.padding(14.dp).fillMaxWidth()) {
                    Text(day.date.toString(), fontWeight = FontWeight.SemiBold)
                    Text(day.nutrients[NutrientKey.ENERGY]?.let { "${it.toInt()} kcal" } ?: "--")
                }
            }
        }
    }
}

@Composable
private fun EnergyBars(days: List<DailyTotals>) {
    val values = days.mapNotNull { day -> day.nutrients[NutrientKey.ENERGY]?.let { day.date to it } }.takeLast(14)
    if (values.isEmpty()) {
        Text("--")
        return
    }
    val maximum = values.maxOf { it.second }.coerceAtLeast(1.0)
    Canvas(Modifier.fillMaxWidth().height(120.dp)) {
        val gap = 6.dp.toPx()
        val width = (size.width - gap * (values.size - 1)) / values.size
        values.forEachIndexed { index, (_, value) ->
            val barHeight = (value / maximum * size.height).toFloat()
            drawRoundRect(
                color = Sage,
                topLeft = Offset(index * (width + gap), size.height - barHeight),
                size = Size(width, barHeight),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
            )
        }
    }
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

private fun List<DailyTotals>.forWindow(window: TrendWindow, today: LocalDate): List<DailyTotals> {
    val lower = window.days?.let { today.minusDays(it - 1) }
    return filter { !it.date.isAfter(today) && (lower == null || !it.date.isBefore(lower)) }
        .filter { it.aggregate.totalEntries > 0 }
        .sortedBy { it.date }
}

private fun TrendWindow.displayLabel(): String = when (this) {
    TrendWindow.DAYS_7 -> "7d"
    TrendWindow.DAYS_30 -> "30d"
    TrendWindow.DAYS_90 -> "90d"
    TrendWindow.ALL -> "All"
}

private fun formatValue(value: Double): String =
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
