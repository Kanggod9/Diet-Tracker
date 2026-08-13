package io.github.kanggod9.diettracker.ui
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.kanggod9.diettracker.data.LocalStore
import io.github.kanggod9.diettracker.domain.GuidanceRegion
import io.github.kanggod9.diettracker.domain.NutrientKey
import io.github.kanggod9.diettracker.domain.NutrientTargets
import java.util.Locale

@Composable
internal fun TargetScreen(
    store: LocalStore,
    region: GuidanceRegion,
    onRegionChanged: (GuidanceRegion) -> Unit,
    onTargetsChanged: () -> Unit,
    onNutrientSelected: (NutrientKey) -> Unit,
) {
    var revision by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<NutrientKey?>(null) }
    val settings = remember(revision, region) { store.settings() }
    val targets = remember(settings, region) { NutrientTargets.resolved(region, settings) }
    val visibleKeys = dashboardNutrientOrder

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
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
        items(visibleKeys, key = { it.name }) { key ->
            val target = targets[key]
            val custom = settings[NutrientTargets.settingKey(key)] != null
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Target card ${key.name}" }
                    .clickable { onNutrientSelected(key) },
                border = BorderStroke(1.dp, Turquoise.copy(alpha = 0.72f)),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(key.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (custom) "Custom" else region.displayName,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Text(
                        target?.let { "${targetValue(it)} ${key.unit}" } ?: "--",
                        color = DarkTurquoise,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    IconButton(onClick = { editing = key }) {
                        Icon(Icons.Outlined.Edit, "Edit ${key.label} target")
                    }
                }
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
