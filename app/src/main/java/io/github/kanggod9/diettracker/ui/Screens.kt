package io.github.kanggod9.diettracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.kanggod9.diettracker.domain.DailyTotals
import io.github.kanggod9.diettracker.domain.EntryKind
import io.github.kanggod9.diettracker.domain.GuidanceProfiles
import io.github.kanggod9.diettracker.domain.GuidanceRegion
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

internal enum class Screen(val label: String, val icon: ImageVector) {
    TODAY("Today", Icons.Outlined.Today),
    HISTORY("History", Icons.AutoMirrored.Outlined.List),
    ANALYSIS("Analysis", Icons.Outlined.Insights),
    SETTINGS("Settings", Icons.Outlined.Settings),
}

@Composable
internal fun TodayScreen(
    entries: List<JournalEntry>,
    quickFoods: List<QuickFood>,
    profile: GuidanceRegion,
    suggestions: List<Suggestion>,
    onLog: () -> Unit,
    onPhoto: () -> Unit,
    onEdit: (JournalEntry) -> Unit,
    onDelete: (JournalEntry) -> Unit,
) {
    val today = LocalDate.now()
    val current = entries.filter { it.localDate() == today }
    val aggregate = NutrientAggregator.aggregate(current.map { it.nutrients })
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Diet Tracker", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text(today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")), color = Color.Gray)
            AssistChip(onClick = {}, label = { Text("Local-first journal") })
        }
        item { NutrientHero(aggregate) }
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
        if (quickFoods.isNotEmpty()) item {
            SectionCard("Quick foods") {
                Text("Open Log to review and reuse one of ${quickFoods.size} saved local shortcuts.")
            }
        }
        item {
            SectionCard("${profile.displayName} guidance") {
                if (suggestions.isEmpty()) Text("No suggestion is shown unless reported fields have enough coverage.")
                suggestions.take(3).forEach { suggestion ->
                    Text(suggestion.message)
                    Text(suggestion.evidence, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    HorizontalDivider()
                }
                Text("General adult references only; not personalised medical advice.", style = MaterialTheme.typography.bodySmall)
            }
        }
        item { Text("Journal", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
        if (current.isEmpty()) item {
            EmptyCard("Nothing logged yet", "Photos, searches, and manual drafts save nothing until you review and confirm.")
        }
        items(current, key = { it.id }) { EntryCard(it, onEdit, onDelete) }
    }
}

@Composable
private fun NutrientHero(aggregate: NutrientAggregate) {
    val energy = aggregate.nutrients[NutrientKey.ENERGY]
    ElevatedCard(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Mint),
    ) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Logged energy", color = DeepSage)
            Text(
                energy?.let { "${it.toInt()} kcal" } ?: "Not available",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf(NutrientKey.PROTEIN, NutrientKey.TOTAL_CARBOHYDRATE, NutrientKey.TOTAL_FAT).forEach { key ->
                    Column {
                        Text(key.label, style = MaterialTheme.typography.labelMedium)
                        Text(aggregate.nutrients[key]?.let { "%.1f g".format(it) } ?: "Missing")
                    }
                }
            }
            Text(
                "Missing values are never counted as zero. ${aggregate.totalEntries} item(s) contribute today.",
                style = MaterialTheme.typography.bodySmall,
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
                Text("${entry.mealType.displayName} - ${entry.servingDescription}", style = MaterialTheme.typography.bodySmall)
                Text(entry.nutrients[NutrientKey.ENERGY]?.let { "${it.toInt()} kcal" } ?: "Energy unavailable")
            }
            IconButton(onClick = { onEdit(entry) }) { Icon(Icons.Outlined.Edit, "Edit") }
            IconButton(onClick = { onDelete(entry) }) { Icon(Icons.Outlined.Delete, "Delete") }
        }
    }
}

@Composable
internal fun HistoryScreen(
    entries: List<JournalEntry>,
    onEdit: (JournalEntry) -> Unit,
    onDelete: (JournalEntry) -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("History", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("Editable local journal - newest first")
        }
        if (entries.isEmpty()) item { EmptyCard("No history", "Confirmed entries will appear here.") }
        items(entries, key = { it.id }) { EntryCard(it, onEdit, onDelete) }
    }
}
@Composable
internal fun AnalysisScreen(entries: List<JournalEntry>) {
    var window by remember { mutableStateOf(TrendWindow.DAYS_7) }
    val allDays = entries.toDailyTotals()
    val visibleDays = allDays.forWindow(window, LocalDate.now())
    val trend = TrendAnalyzer.summarize(allDays, window, LocalDate.now())
    val today = NutrientAggregator.aggregate(entries.filter { it.localDate() == LocalDate.now() }.map { it.nutrients })
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Analysis", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("Logged-day patterns, with missing days left missing")
        }
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
            SectionCard("Energy on logged days") {
                EnergyBars(visibleDays)
                Text("${trend.daysWithEntries} logged day(s); blank days are not rendered as zero.")
            }
        }
        item {
            SectionCard("Daily averages") {
                val visible = NutrientKey.entries.filter { trend.dailyAverage[it] != null }
                if (visible.isEmpty()) Text("No reported nutrient averages for this window.")
                visible.take(16).forEach { key ->
                    Text("${key.label}: ${"%.1f".format(trend.dailyAverage[key])} ${key.unit}")
                }
                Text("Each nutrient uses only days with adequate field coverage.", style = MaterialTheme.typography.bodySmall)
            }
        }
        GuidanceProfiles.all.forEach { profile ->
            val guidance = SuggestionEngine.generate(today, trend, profile)
            item {
                SectionCard(profile.title) {
                    Text(profile.sourceEffectiveVersion, style = MaterialTheme.typography.bodySmall)
                    if (guidance.isEmpty()) Text("No evidence-based suggestion for the available data.")
                    guidance.take(6).forEach { suggestion ->
                        Text(suggestion.message)
                        Text(suggestion.evidence, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
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
                    Text(day.nutrients[NutrientKey.ENERGY]?.let { "${it.toInt()} kcal" } ?: "Energy missing")
                    Text("${day.aggregate.totalEntries} item(s); missing fields remain unavailable.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun EnergyBars(days: List<DailyTotals>) {
    val values = days.mapNotNull { day -> day.nutrients[NutrientKey.ENERGY]?.let { day.date to it } }.takeLast(14)
    if (values.isEmpty()) {
        Text("No energy values are available.")
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
        Column(Modifier.padding(20.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
internal fun EmptyCard(title: String, body: String) {
    OutlinedCard {
        Column(Modifier.padding(20.dp).fillMaxWidth()) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(body)
        }
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
    note = "Created from a reviewed local quick food.",
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