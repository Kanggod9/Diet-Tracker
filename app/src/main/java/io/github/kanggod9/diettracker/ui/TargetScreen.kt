package io.github.kanggod9.diettracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.kanggod9.diettracker.data.LocalStore
import io.github.kanggod9.diettracker.domain.GuidanceRegion
import io.github.kanggod9.diettracker.domain.NutrientKey
import io.github.kanggod9.diettracker.domain.NutrientTargets
import java.util.Locale

private val targetOrder = listOf(
    NutrientKey.ENERGY,
    NutrientKey.PROTEIN,
    NutrientKey.TOTAL_CARBOHYDRATE,
    NutrientKey.TOTAL_FAT,
    NutrientKey.SATURATED_FAT,
    NutrientKey.DIETARY_FIBER,
    NutrientKey.TOTAL_SUGAR,
    NutrientKey.ADDED_SUGAR,
    NutrientKey.SODIUM,
    NutrientKey.CHOLESTEROL,
    NutrientKey.CAFFEINE,
    NutrientKey.WATER,
) + NutrientKey.entries.filterNot {
    it in setOf(
        NutrientKey.ENERGY,
        NutrientKey.PROTEIN,
        NutrientKey.TOTAL_CARBOHYDRATE,
        NutrientKey.TOTAL_FAT,
        NutrientKey.SATURATED_FAT,
        NutrientKey.DIETARY_FIBER,
        NutrientKey.TOTAL_SUGAR,
        NutrientKey.ADDED_SUGAR,
        NutrientKey.SODIUM,
        NutrientKey.CHOLESTEROL,
        NutrientKey.CAFFEINE,
        NutrientKey.WATER,
    )
}

@Composable
internal fun TargetScreen(
    store: LocalStore,
    region: GuidanceRegion,
    onRegionChanged: (GuidanceRegion) -> Unit,
    onTargetsChanged: () -> Unit,
) {
    var revision by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<NutrientKey?>(null) }
    val settings = remember(revision, region) { store.settings() }
    val targets = remember(settings, region) { NutrientTargets.resolved(region, settings) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Text("Target", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold) }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GuidanceRegion.entries.forEach { value ->
                    FilterChip(
                        selected = region == value,
                        onClick = { onRegionChanged(value) },
                        label = { Text(if (value == GuidanceRegion.SINGAPORE) "Singapore" else value.displayName) },
                    )
                }
            }
        }
        items(targetOrder, key = { it.name }) { key ->
            val target = targets[key]
            val custom = settings[NutrientTargets.settingKey(key)] != null
            Column(
                Modifier.fillMaxWidth().clickable { editing = key }.padding(vertical = 5.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(key.label, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Text(
                        target?.let { "${targetValue(it)} ${key.unit}" } ?: "--",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                LinearProgressIndicator(
                    progress = { if (target != null) 1f else 0f },
                    modifier = Modifier.fillMaxWidth().height(9.dp),
                    color = Sage,
                    trackColor = Color(0xFFE4E9E5),
                )
                Text(
                    if (custom) "Custom" else region.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                )
            }
        }
        item {
            OutlinedButton(
                onClick = {
                    store.settings().keys.filter { it.startsWith("target.") }.forEach(store::removeSetting)
                    revision++
                    onTargetsChanged()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Restore ${region.displayName} defaults") }
        }
    }

    editing?.let { key ->
        TargetEditDialog(
            key = key,
            current = targets[key],
            custom = settings[NutrientTargets.settingKey(key)] != null,
            onDismiss = { editing = null },
            onSave = { value ->
                store.setSetting(NutrientTargets.settingKey(key), value.toString())
                revision++
                editing = null
                onTargetsChanged()
            },
            onUseDefault = {
                store.removeSetting(NutrientTargets.settingKey(key))
                revision++
                editing = null
                onTargetsChanged()
            },
        )
    }
}

@Composable
private fun TargetEditDialog(
    key: NutrientKey,
    current: Double?,
    custom: Boolean,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit,
    onUseDefault: () -> Unit,
) {
    var value by remember(key) { mutableStateOf(current?.toString().orEmpty()) }
    var error by remember(key) { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(key.label) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it; error = false },
                label = { Text(key.unit) },
                isError = error,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(onClick = {
                val number = value.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 && it <= 100_000.0 }
                if (number == null) error = true else onSave(number)
            }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (custom) TextButton(onClick = onUseDefault) { Text("Use default") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

private fun targetValue(value: Double): String =
    if (value >= 100.0 || value % 1.0 == 0.0) String.format(Locale.US, "%.0f", value)
    else String.format(Locale.US, "%.1f", value)
