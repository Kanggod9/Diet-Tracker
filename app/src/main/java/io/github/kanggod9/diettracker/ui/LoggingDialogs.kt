package io.github.kanggod9.diettracker.ui
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.Icons

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.kanggod9.diettracker.domain.AmountUnit
import io.github.kanggod9.diettracker.domain.DataSet
import io.github.kanggod9.diettracker.domain.EntryAmount
import io.github.kanggod9.diettracker.domain.EntryKind
import io.github.kanggod9.diettracker.domain.FoodScoreCalculator
import io.github.kanggod9.diettracker.domain.JournalEntry
import io.github.kanggod9.diettracker.domain.ManualEntryEstimator
import io.github.kanggod9.diettracker.domain.ManualEntryParser
import io.github.kanggod9.diettracker.domain.MealType
import io.github.kanggod9.diettracker.domain.NutrientKey
import io.github.kanggod9.diettracker.domain.NutrientProvenance
import io.github.kanggod9.diettracker.domain.Nutrients
import io.github.kanggod9.diettracker.domain.ParsedManualEntry
import io.github.kanggod9.diettracker.domain.QuickFood
import io.github.kanggod9.diettracker.integration.PhotoDraft
import io.github.kanggod9.diettracker.integration.UsdaFood
import io.github.kanggod9.diettracker.integration.UsdaFoodDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.UUID

internal enum class ReviewReturnDestination { CLOSE, LOG_CHOOSER, PHOTO_SOURCE }

internal data class ReviewSeed(
    val entry: JournalEntry?,
    val title: String,
    val sourceNotes: List<String> = emptyList(),
    val photoDraft: PhotoDraft? = null,
    val readOnly: Boolean = false,
    val returnDestination: ReviewReturnDestination = ReviewReturnDestination.CLOSE,
)

internal data class UsdaLookupRequest(
    val query: String,
    val parsed: ParsedManualEntry? = null,
    val photoDraft: PhotoDraft? = null,
    val returnSeed: ReviewSeed? = null,
)

internal data class PhotoPayload(val bytes: ByteArray, val mimeType: String)

@Composable
internal fun LogChooserDialog(
    quickFoods: List<QuickFood>,
    onlineConfigured: Boolean,
    onDismiss: () -> Unit,
    onDetailedManual: () -> Unit,
    onTextParsed: (ParsedManualEntry) -> Unit,
    onUsdaSearch: () -> Unit,
    onPhoto: () -> Unit,
    onQuickFood: (QuickFood) -> Unit,
    onDeleteQuickFood: (QuickFood) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("g") }
    var error by remember { mutableStateOf<String?>(null) }
    var pendingQuickFoodDelete by remember { mutableStateOf<QuickFood?>(null) }
    val units = listOf("g", "kg", "mL", "L", "serving", "kcal")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log food or drink") },
        text = {
            LazyColumn(
                Modifier.heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(120); error = null },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it; error = null },
                        label = { Text("Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        units.forEach { value ->
                            FilterChip(selected = unit == value, onClick = { unit = value }, label = { Text(value) })
                        }
                    }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Button(
                        onClick = {
                            val parsed = ManualEntryParser.parse("${name.trim()} ${quantity.trim()} $unit")
                            if (parsed == null) error = "Enter a name and quantity." else onTextParsed(parsed)
                        },
                        enabled = onlineConfigured && name.isNotBlank() && quantity.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Review with USDA") }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onDetailedManual, modifier = Modifier.weight(1f)) {
                            Text("Detailed manual")
                        }
                        OutlinedButton(onClick = onUsdaSearch, enabled = onlineConfigured, modifier = Modifier.weight(1f)) {
                            Text("USDA search")
                        }
                    }
                    OutlinedButton(onClick = onPhoto, enabled = onlineConfigured, modifier = Modifier.fillMaxWidth()) {
                        Text("Photo")
                    }
                }
                if (quickFoods.isNotEmpty()) {
                    item { Text("Quick foods", fontWeight = FontWeight.Bold) }
                    items(quickFoods, key = { it.id }) { quick ->
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth().combinedClickable(
                                onClick = { onQuickFood(quick) },
                                onLongClickLabel = "Delete quick food",
                                onLongClick = { pendingQuickFoodDelete = quick },
                            ),
                        ) {
                            Text(
                                "${quick.name} · ${quick.servingDescription}",
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
    pendingQuickFoodDelete?.let { quick ->
        AlertDialog(
            onDismissRequest = { pendingQuickFoodDelete = null },
            title = { Text("Delete quick food?") },
            text = { Text("Remove ${quick.name} from Quick foods? This does not delete journal entries.") },
            confirmButton = {
                Button(onClick = {
                    pendingQuickFoodDelete = null
                    onDeleteQuickFood(quick)
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingQuickFoodDelete = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
internal fun PhotoSourceDialog(
    onDismiss: () -> Unit,
    onCamera: () -> Unit,
    onAlbum: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Photo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCamera, modifier = Modifier.fillMaxWidth()) { Text("Camera") }
                OutlinedButton(onClick = onAlbum, modifier = Modifier.fillMaxWidth()) { Text("Album") }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
internal fun PhotoConsentDialog(
    endpoint: String?,
    onDismiss: () -> Unit,
    onAnalyze: (Boolean) -> Unit,
) {
    var dontShowAgain by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Send photo for AI analysis?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("The photo will be sent to your private gateway and OpenAI.")
                Text(endpoint ?: "Gateway not configured", style = MaterialTheme.typography.bodySmall)
                Text("Nothing is saved until you review and confirm.")
                Row(
                    modifier = Modifier.clickable { dontShowAgain = !dontShowAgain },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = dontShowAgain, onCheckedChange = { dontShowAgain = it })
                    Text("Don't show next time")
                }
            }
        },
        confirmButton = { Button(onClick = { onAnalyze(dontShowAgain) }) { Text("I consent") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
internal fun UsdaSearchDialog(
    request: UsdaLookupRequest,
    dataSource: UsdaFoodDataSource,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    onReview: (ReviewSeed) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var query by remember(request) { mutableStateOf(request.query) }
    var grams by remember(request) {
        mutableStateOf(
            request.photoDraft?.amount?.takeIf { it.unit == AmountUnit.GRAM }?.value?.toString()
                ?: request.parsed?.amount?.takeIf { it.unit == AmountUnit.GRAM }?.value?.toString()
                ?: "100",
        )
    }
    var results by remember { mutableStateOf<List<UsdaFood>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun select(food: UsdaFood) {
        val candidate = when {
            request.photoDraft != null -> {
                val weight = grams.toDoubleOrNull()?.takeIf { it > 0.0 }
                if (weight == null) {
                    error = "Enter the photographed amount in grams."
                    null
                } else {
                    val merged = request.photoDraft.withMissingUsda(food, weight)
                    ReviewSeed(
                        entry = merged.toConfirmedEntry("Photo fields filled from reviewed USDA ${food.dataType.wireValue} match ${food.fdcId}."),
                        title = "Review photo plus USDA",
                        sourceNotes = listOf(
                            "Existing values were preserved; USDA added available fields.",
                            "USDA ${food.dataType.wireValue} #${food.fdcId}: ${food.description}.",
                        ),
                        photoDraft = merged,
                        returnDestination = request.returnSeed?.returnDestination
                            ?: ReviewReturnDestination.PHOTO_SOURCE,
                    )
                }
            }
            request.parsed != null -> {
                ManualEntryEstimator.fromUsda(request.parsed, food)?.let {
                    ReviewSeed(
                        it,
                        "Review text log with USDA",
                        listOf("USDA ${food.dataType.wireValue} #${food.fdcId}: ${food.description}."),
                    )
                } ?: run {
                    error = "This USDA reference is per 100 g. Use a gram or kcal quantity, or detailed manual logging."
                    null
                }
            }
            else -> {
                val weight = grams.toDoubleOrNull()?.takeIf { it > 0.0 }
                if (weight == null) {
                    error = "Enter a valid weight in grams."
                    null
                } else {
                    val amount = EntryAmount(weight, AmountUnit.GRAM)
                    ReviewSeed(
                        JournalEntry(
                            name = food.description,
                            kind = EntryKind.FOOD,
                            mealType = MealType.UNKNOWN,
                            servingDescription = "${"%.1f".format(weight)} g",
                            servingGrams = weight,
                            amount = amount,
                            nutrients = food.nutrientsForGrams(weight),
                            note = "Nutrition scaled from reviewed USDA reference; confirm before saving.",
                        ),
                        "Review USDA food",
                        listOf("USDA ${food.dataType.wireValue} #${food.fdcId}."),
                    )
                }
            }
        }
        candidate?.let(onReview)
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            Modifier.fillMaxSize().padding(18.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            LazyColumn(
                Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
                        }
                        Column {
                            Text(
                                "USDA FoodData Central",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text("Only Foundation and SR Legacy records are accepted.")
                        }
                    }
                    OutlinedTextField(
                        query,
                        { query = it; error = null },
                        label = { Text("Search food") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        grams,
                        { grams = it; error = null },
                        label = { Text("Amount in grams") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                loading = true
                                error = null
                                try {
                                    results = dataSource.search(query.trim())
                                    if (results.isEmpty()) error = "No Foundation or SR Legacy matches were returned."
                                } catch (exception: Exception) {
                                    error = exception.message ?: "USDA search could not be completed."
                                } finally {
                                    loading = false
                                }
                            }
                        },
                        enabled = query.trim().length >= 2 && !loading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (loading) CircularProgressIndicator(Modifier.padding(end = 8.dp))
                        Text("Search verified data")
                    }
                    error?.let { Text(it.take(240), color = MaterialTheme.colorScheme.error) }
                    Text("Choose a result to create an in-memory draft. It is not saved yet.", style = MaterialTheme.typography.bodySmall)
                }
                items(results, key = { it.fdcId }) { food ->
                    OutlinedButton(onClick = { select(food) }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth()) {
                            Text(food.description, fontWeight = FontWeight.SemiBold)
                            Text("${food.dataType.wireValue} - FDC ${food.fdcId}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                item { TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") } }
            }
        }
    }
}
@Composable
internal fun EntryEditorDialog(
    seed: ReviewSeed,
    nutrientTargets: Map<NutrientKey, Double>,
    onDismiss: () -> Unit,
    onSave: (JournalEntry, Boolean) -> Unit,
    onFindUsda: (() -> Unit)? = null,
) {
    val existing = seed.entry
    var name by remember(seed) { mutableStateOf(existing?.name.orEmpty()) }
    var kind by remember(seed) { mutableStateOf(existing?.kind ?: EntryKind.FOOD) }
    var meal by remember(seed) { mutableStateOf(existing?.mealType ?: MealType.UNKNOWN) }
    var amount by remember(seed) { mutableStateOf(existing?.amount?.value?.toString() ?: "1") }
    var unit by remember(seed) { mutableStateOf(existing?.amount?.unit ?: AmountUnit.SERVING) }
    var note by remember(seed) { mutableStateOf(existing?.note.orEmpty()) }
    var showAll by remember { mutableStateOf(false) }
    var editingNutrient by remember { mutableStateOf<NutrientKey?>(null) }
    var review by remember(seed) { mutableStateOf(seed.readOnly) }
    var saveQuick by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var candidate by remember(seed) { mutableStateOf(existing.takeIf { seed.readOnly }) }
    val nutrientText = remember(seed) {
        mutableStateMapOf<NutrientKey, String>().apply {
            existing?.nutrients?.values?.forEach { (key, value) -> put(key, value.toString()) }
        }
    }
    val edited = remember(seed) { mutableStateMapOf<NutrientKey, Boolean>() }
    val common = listOf(
        NutrientKey.ENERGY,
        NutrientKey.PROTEIN,
        NutrientKey.TOTAL_CARBOHYDRATE,
        NutrientKey.TOTAL_FAT,
        NutrientKey.SATURATED_FAT,
        NutrientKey.DIETARY_FIBER,
        NutrientKey.TOTAL_SUGAR,
        NutrientKey.ADDED_SUGAR,
        NutrientKey.SODIUM,
        NutrientKey.WATER,
    )

    fun buildCandidate(): JournalEntry? {
        val numericAmount = amount.toDoubleOrNull()?.takeIf { it > 0.0 && it <= 100_000.0 }
        if (name.isBlank() || numericAmount == null) {
            error = "Enter a name and a positive amount."
            return null
        }
        val invalid = nutrientText.entries.firstOrNull { (_, text) ->
            text.isNotBlank() && text.toDoubleOrNull()?.let { !it.isFinite() || it < 0.0 || it > 100_000.0 } != false
        }
        if (invalid != null) {
            error = "${invalid.key.label} must be blank or a non-negative number."
            return null
        }
        val values = nutrientText.mapNotNull { (key, text) -> text.toDoubleOrNull()?.let { key to it } }.toMap()
        val provenance = values.keys.associateWith { key ->
            val previous = existing?.nutrients?.provenance?.get(key)
            if (edited[key] != true && previous != null) previous else NutrientProvenance(
                dataSet = DataSet.MANUAL,
                sourceLabel = "User entered or edited",
                retrievedAt = Instant.now(),
                verified = false,
            )
        }
        val entryAmount = EntryAmount(numericAmount, unit)
        error = null
        return JournalEntry(
            id = existing?.id ?: UUID.randomUUID().toString(),
            name = name.trim(),
            kind = kind,
            mealType = meal,
            servingDescription = "${entryAmount.value} ${unit.symbol}",
            servingGrams = entryAmount.value.takeIf { unit == AmountUnit.GRAM },
            amount = entryAmount,
            loggedAt = existing?.loggedAt ?: Instant.now(),
            nutrients = Nutrients(values, provenance),
            note = note.trim(),
        )
    }

    fun goBack() {
        if (review && !seed.readOnly) {
            review = false
            candidate = null
        } else {
            onDismiss()
        }
    }

    Dialog(onDismissRequest = ::goBack, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            Modifier.fillMaxSize().padding(18.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = ::goBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
                    }
                    Text(
                        if (review) "Review before saving" else seed.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                LazyColumn(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (review) {
                        val reviewed = candidate
                        item {
                            seed.sourceNotes.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                            if (seed.sourceNotes.isNotEmpty()) HorizontalDivider()
                            Text(reviewed?.name.orEmpty(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("${reviewed?.mealType?.displayName} - ${reviewed?.servingDescription}")
                            if (!reviewed?.note.isNullOrBlank()) Text(reviewed?.note.orEmpty())
                        }
                        reviewed?.let { entry ->
                            items(entry.nutrients.values.entries.sortedBy { it.key.ordinal }, key = { it.key.name }) { (key, value) ->
                                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    NutrientProgressRow(key, value, nutrientTargets[key])
                                    entry.nutrients.provenance[key]?.sourceLabel?.let {
                                        Text(it, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    }
                                }
                            }
                            item {
                                val score = FoodScoreCalculator.calculate(entry.nutrients)
                                SectionCard("Food Score") {
                                    Text(score.score?.let { "$it / 100" } ?: "--", style = MaterialTheme.typography.titleLarge)
                                    FoodScoreDetails(score)
                                }
                            }
                            if (!seed.readOnly) item {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(saveQuick, { saveQuick = it })
                                    Text("Also save as a local quick food")
                                }
                            }
                            if (onFindUsda != null) item {
                                OutlinedButton(onClick = onFindUsda, modifier = Modifier.fillMaxWidth()) {
                                    Text("Fill -- fields from USDA")
                                }
                            }
                        }
                    } else {
                        item {
                            OutlinedTextField(name, { name = it; error = null }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                EntryKind.entries.forEach { value ->
                                    FilterChip(value == kind, { kind = value }, label = { Text(value.displayName) })
                                }
                            }
                        }
                        item {
                            OutlinedTextField(amount, { amount = it; error = null }, label = { Text("Amount") }, modifier = Modifier.fillMaxWidth())
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                AmountUnit.entries.forEach { value ->
                                    FilterChip(value == unit, { unit = value }, label = { Text(value.symbol) })
                                }
                            }
                        }
                        item {
                            Text("Meal")
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                MealType.entries.forEach { value ->
                                    AssistChip(onClick = { meal = value }, label = { Text(value.displayName) })
                                }
                            }
                            Text("Selected: ${meal.displayName}", style = MaterialTheme.typography.bodySmall)
                        }
                        val keys = if (showAll) NutrientKey.entries else common
                        items(keys, key = { it.name }) { key ->
                            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                NutrientProgressRow(
                                    key = key,
                                    value = nutrientText[key]?.toDoubleOrNull(),
                                    target = nutrientTargets[key],
                                    onValueClick = { editingNutrient = key },
                                )
                                if (editingNutrient == key) {
                                    OutlinedTextField(
                                        value = nutrientText[key].orEmpty(),
                                        onValueChange = { nutrientText[key] = it; edited[key] = true; error = null },
                                        label = { Text(key.unit) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        }
                        item {
                            TextButton(onClick = { showAll = !showAll }) {
                                Text(if (showAll) "Show common nutrients" else "Show all Health Connect nutrients")
                            }
                            OutlinedTextField(
                                note,
                                { note = it.take(2_000) },
                                label = { Text("Note") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = ::goBack) {
                        Text(if (review && !seed.readOnly) "Back" else "Close")
                    }
                    if (!seed.readOnly) Button(onClick = {
                        if (!review) {
                            buildCandidate()?.let { candidate = it; review = true }
                        } else {
                            candidate?.let { onSave(it, saveQuick) }
                        }
                    }) { Text(if (review) "Confirm and save" else "Review") }
                }
            }
        }
    }
}
@Composable
internal fun HealthImportReviewDialog(
    entries: List<JournalEntry>,
    onDismiss: () -> Unit,
    onConfirm: (List<JournalEntry>) -> Unit,
) = EntrySelectionDialog(
    title = "Review Health Connect import",
    body = "Nothing is copied into the local journal until you confirm the selected records.",
    confirmLabel = "Import selected",
    entries = entries,
    onDismiss = onDismiss,
    onConfirm = onConfirm,
)

@Composable
internal fun HealthExportDialog(
    entries: List<JournalEntry>,
    onDismiss: () -> Unit,
    onConfirm: (List<JournalEntry>) -> Unit,
) = EntrySelectionDialog(
    title = "Write to Health Connect",
    body = "Selected records are replaced in Health Connect. Future edits and deletions stay synchronized while permissions remain granted.",
    confirmLabel = "Write selected",
    entries = entries,
    onDismiss = onDismiss,
    onConfirm = onConfirm,
)

@Composable
private fun EntrySelectionDialog(
    title: String,
    body: String,
    confirmLabel: String,
    entries: List<JournalEntry>,
    onDismiss: () -> Unit,
    onConfirm: (List<JournalEntry>) -> Unit,
) {
    val selected = remember(entries) { mutableStateMapOf<String, Boolean>() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(Modifier.heightIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Text(body) }
                if (entries.isEmpty()) item { Text("No compatible records are available.") }
                items(entries, key = { it.id }) { entry ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = selected[entry.id] == true,
                            onCheckedChange = { selected[entry.id] = it },
                        )
                        Column(Modifier.weight(1f)) {
                            Text(entry.name, fontWeight = FontWeight.SemiBold)
                            Text("${entry.loggedAt} - ${entry.servingDescription}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(entries.filter { selected[it.id] == true }) },
                enabled = selected.values.any { it },
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

internal suspend fun readPhotoForAnalysis(context: Context, uri: Uri): PhotoPayload = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    val mime = resolver.getType(uri)?.lowercase() ?: "image/jpeg"
    require(mime in setOf("image/jpeg", "image/png", "image/webp", "image/heic", "image/heif")) {
        "Choose a JPEG, PNG, WebP, HEIC, or HEIF image."
    }
    val bytes = resolver.openInputStream(uri)?.use { input ->
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(16_384)
        var total = 0
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            require(total <= MAX_PHOTO_BYTES) { "Choose an image no larger than 8 MB." }
            output.write(buffer, 0, count)
        }
        output.toByteArray()
    } ?: error("The selected image could not be opened.")
    require(bytes.isNotEmpty()) { "The selected image is empty." }
    PhotoPayload(bytes, mime)
}

private const val MAX_PHOTO_BYTES = 8 * 1024 * 1024