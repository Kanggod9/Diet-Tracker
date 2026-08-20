package io.github.kanggod9.diettracker.ui

import android.app.TimePickerDialog
import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.github.kanggod9.diettracker.data.LocalSnapshot
import io.github.kanggod9.diettracker.data.LocalStore
import io.github.kanggod9.diettracker.data.SecureConfigStore
import io.github.kanggod9.diettracker.integration.HealthConnectAvailability
import io.github.kanggod9.diettracker.integration.HealthConnectGateway
import io.github.kanggod9.diettracker.reminder.MealReminderPreferences
import io.github.kanggod9.diettracker.reminder.ReminderMeal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun SettingsScreen(
    store: LocalStore,
    secureConfig: SecureConfigStore,
    healthGateway: HealthConnectGateway,
    gatewayRevision: Int,
    healthPermissionRevision: Int,
    reminderRevision: Int,
    notificationPermissionGranted: Boolean,
    onGatewayChanged: () -> Unit,
    onReminderMasterChanged: (Boolean) -> Unit,
    onReminderMealChanged: (ReminderMeal, Boolean) -> Unit,
    onReminderTimeChanged: (ReminderMeal, LocalTime) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestHealthPermissions: () -> Unit,
    onImportHealth: () -> Unit,
    onExportHealth: () -> Unit,
    onExportJson: () -> Unit,
    onDeleteAll: () -> Unit,
) {
    var endpoint by remember(gatewayRevision) { mutableStateOf(secureConfig.configuredEndpoint().orEmpty()) }
    var token by remember(gatewayRevision) { mutableStateOf("") }
    var gatewayError by remember { mutableStateOf<String?>(null) }
    var granted by remember { mutableStateOf<Set<String>>(emptySet()) }
    val reminderSettings = remember(reminderRevision) { MealReminderPreferences.load(store) }
    val context = LocalContext.current
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()) }
    LaunchedEffect(gatewayRevision, healthPermissionRevision) {
        granted = runCatching { healthGateway.grantedPermissions() }.getOrDefault(emptySet())
    }
    val healthAvailable = healthGateway.availability() == HealthConnectAvailability.AVAILABLE
    val allHealthPermissions = healthGateway.canReadAndWrite(granted)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Text("Settings", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold) }
        item {
            SectionCard("Private AI and USDA gateway") {
                OutlinedTextField(
                    endpoint,
                    { endpoint = it; gatewayError = null },
                    label = { Text("Gateway URL") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    token,
                    { token = it; gatewayError = null },
                    label = { Text("Access token") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                gatewayError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            try {
                                secureConfig.saveGateway(endpoint, token)
                                token = ""
                                gatewayError = null
                                onGatewayChanged()
                            } catch (error: Exception) {
                                gatewayError = error.message ?: "Invalid gateway settings."
                            }
                        },
                        enabled = endpoint.isNotBlank() && token.isNotBlank(),
                        modifier = Modifier.weight(1f),
                    ) { Text("Save") }
                    OutlinedButton(
                        onClick = {
                            secureConfig.clearGateway()
                            endpoint = ""
                            token = ""
                            onGatewayChanged()
                        },
                        enabled = secureConfig.configuredEndpoint() != null,
                        modifier = Modifier.weight(1f),
                    ) { Text("Clear") }
                }
                Text(if (secureConfig.isConfigured()) "Configured" else "Not configured")
            }
        }
        item {
            SectionCard("Health Connect") {
                Text(
                    when (healthGateway.availability()) {
                        HealthConnectAvailability.AVAILABLE -> if (allHealthPermissions) "Connected" else "Permissions needed"
                        HealthConnectAvailability.UPDATE_REQUIRED -> "Update required"
                        HealthConnectAvailability.NOT_SUPPORTED -> "Not supported"
                    },
                )
                Button(
                    onClick = onRequestHealthPermissions,
                    enabled = healthAvailable,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Permissions") }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onImportHealth,
                        enabled = healthAvailable && allHealthPermissions,
                        modifier = Modifier.weight(1f),
                    ) { Text("Import") }
                    OutlinedButton(
                        onClick = onExportHealth,
                        enabled = healthAvailable && allHealthPermissions,
                        modifier = Modifier.weight(1f),
                    ) { Text("Write") }
                }
            }
        }
        item {
            SectionCard("Meal reminders") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("Enable reminders", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Notifications are skipped when that meal is already logged.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Switch(
                        checked = reminderSettings.enabled,
                        onCheckedChange = onReminderMasterChanged,
                    )
                }
                if (reminderSettings.enabled && !notificationPermissionGranted) {
                    Text("Notification permission is required.", color = MaterialTheme.colorScheme.error)
                    Button(onClick = onRequestNotificationPermission, modifier = Modifier.fillMaxWidth()) {
                        Text("Grant notification permission")
                    }
                }
                ReminderMeal.entries.forEach { meal ->
                    val option = reminderSettings.option(meal)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(meal.displayName, fontWeight = FontWeight.SemiBold)
                            OutlinedButton(
                                onClick = {
                                    TimePickerDialog(
                                        context,
                                        { _, hour, minute ->
                                            onReminderTimeChanged(meal, LocalTime.of(hour, minute))
                                        },
                                        option.time.hour,
                                        option.time.minute,
                                        false,
                                    ).show()
                                },
                                enabled = option.enabled,
                            ) { Text(option.time.format(timeFormatter)) }
                        }
                        Switch(
                            checked = option.enabled,
                            onCheckedChange = { onReminderMealChanged(meal, it) },
                        )
                    }
                }
            }
        }
        item {
            SectionCard("Local data") {
                OutlinedButton(onClick = onExportJson, modifier = Modifier.fillMaxWidth()) {
                    Text("Export JSON")
                }
                OutlinedButton(onClick = onDeleteAll, modifier = Modifier.fillMaxWidth()) {
                    Text("Delete all")
                }
            }
        }
        item {
            SectionCard("Privacy") {
                Text("Photos require consent.")
                Text("Gateway keys stay outside the app.")
                Text("USDA searches never include your journal.")
            }
        }
    }
}

internal object ExportJson {
    fun encode(snapshot: LocalSnapshot): String = buildJsonObject {
        put("schema_version", 1)
        put("exported_at", java.time.Instant.now().toString())
        put("privacy_note", "Local user-requested export; gateway credentials are excluded.")
        put("settings", buildJsonObject {
            snapshot.settings.toSortedMap().forEach { (key, value) -> put(key, value) }
        })
        put("entries", JsonArray(snapshot.entries.map { entry ->
            buildJsonObject {
                put("id", entry.id)
                put("name", entry.name)
                put("kind", entry.kind.name)
                put("meal_type", entry.mealType.name)
                put("logged_at", entry.loggedAt.toString())
                put("serving_description", entry.servingDescription)
                put("serving_grams", entry.servingGrams?.let(::JsonPrimitive) ?: JsonNull)
                put("amount_value", entry.amount.value)
                put("amount_unit", entry.amount.unit.name)
                put("note", entry.note)
                put("nutrients", buildJsonObject {
                    entry.nutrients.values.toSortedMap(compareBy { it.name }).forEach { (key, value) ->
                        put(key.name, value)
                    }
                })
                put("provenance", buildJsonObject {
                    entry.nutrients.provenance.toSortedMap(compareBy { it.name }).forEach { (key, source) ->
                        put(key.name, buildJsonObject {
                            put("data_set", source.dataSet.name)
                            put("source_id", source.sourceId?.let(::JsonPrimitive) ?: JsonNull)
                            put("source_label", source.sourceLabel)
                            put("source_url", source.sourceUrl?.let(::JsonPrimitive) ?: JsonNull)
                            put("source_version", source.sourceVersion?.let(::JsonPrimitive) ?: JsonNull)
                            put("retrieved_at", source.retrievedAt?.toString()?.let(::JsonPrimitive) ?: JsonNull)
                            put("verified", source.verified)
                        })
                    }
                })
            }
        }))
        put("suggestions", JsonArray(snapshot.suggestions.map { suggestion ->
            buildJsonObject {
                put("id", suggestion.id)
                put("message", suggestion.message)
                put("source_title", suggestion.sourceTitle)
                put("source_url", suggestion.sourceUrl)
            }
        }))
        put("quick_foods", JsonArray(snapshot.quickFoods.map { food ->
            buildJsonObject {
                put("id", food.id)
                put("name", food.name)
                put("kind", food.kind.name)
                put("meal_type", food.mealType.name)
                put("serving_description", food.servingDescription)
                put("amount_value", food.amount.value)
                put("amount_unit", food.amount.unit.name)
                put("nutrients", buildJsonObject {
                    food.nutrients.values.toSortedMap(compareBy { it.name }).forEach { (key, value) -> put(key.name, value) }
                })
            }
        }))
    }.toString()
}

internal suspend fun writeExportDocument(context: Context, uri: Uri, payload: String) = withContext(Dispatchers.IO) {
    context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8)?.use {
        it.write(payload)
    } ?: error("The export destination could not be opened.")
}