package io.github.kanggod9.diettracker.ui

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.github.kanggod9.diettracker.data.LocalSnapshot
import io.github.kanggod9.diettracker.data.LocalStore
import io.github.kanggod9.diettracker.data.SecureConfigStore
import io.github.kanggod9.diettracker.domain.GuidanceRegion
import io.github.kanggod9.diettracker.integration.HealthConnectAvailability
import io.github.kanggod9.diettracker.integration.HealthConnectGateway
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Composable
internal fun SettingsScreen(
    store: LocalStore,
    secureConfig: SecureConfigStore,
    healthGateway: HealthConnectGateway,
    gatewayRevision: Int,
    healthPermissionRevision: Int,
    onGatewayChanged: () -> Unit,
    onGuidanceChanged: () -> Unit,
    onRequestHealthPermissions: () -> Unit,
    onImportHealth: () -> Unit,
    onExportHealth: () -> Unit,
    onExportJson: () -> Unit,
    onDeleteAll: () -> Unit,
) {
    var region by remember { mutableStateOf(store.setting("guidance_region") ?: GuidanceRegion.SINGAPORE.name) }
    var endpoint by remember(gatewayRevision) { mutableStateOf(secureConfig.configuredEndpoint().orEmpty()) }
    var token by remember(gatewayRevision) { mutableStateOf("") }
    var gatewayError by remember { mutableStateOf<String?>(null) }
    var granted by remember { mutableStateOf<Set<String>>(emptySet()) }
    LaunchedEffect(gatewayRevision, healthPermissionRevision) {
        granted = runCatching { healthGateway.grantedPermissions() }.getOrDefault(emptySet())
    }
    val healthAvailable = healthGateway.availability() == HealthConnectAvailability.AVAILABLE
    val allHealthPermissions = healthGateway.canReadAndWrite(granted)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("Local-first controls and explicit online actions")
        }
        item {
            SectionCard("Guidance profile") {
                GuidanceRegion.entries.forEach { value ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = region == value.name,
                            onClick = {
                                region = value.name
                                store.setSetting("guidance_region", value.name)
                                onGuidanceChanged()
                            },
                        )
                        Text(value.displayName)
                    }
                }
                Text("All three regional profiles remain visible in Analysis. This choice controls the Today card.", style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            SectionCard("Private AI and USDA gateway") {
                Text("The Android app talks only to your HTTPS gateway. OpenAI and USDA provider keys belong in gateway secrets, never here, in the APK, or in Git.")
                OutlinedTextField(
                    endpoint,
                    { endpoint = it; gatewayError = null },
                    label = { Text("Gateway URL (HTTPS)") },
                    placeholder = { Text("https://diet-api.example.com/") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    token,
                    { token = it; gatewayError = null },
                    label = { Text("App-to-gateway access token") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("Use a random 16+ character gateway token. Do not paste an OpenAI API key.") },
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
                                gatewayError = error.message ?: "Gateway settings are invalid."
                            }
                        },
                        enabled = endpoint.isNotBlank() && token.isNotBlank(),
                        modifier = Modifier.weight(1f),
                    ) { Text("Save securely") }
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
                Text(
                    if (secureConfig.configuredEndpoint() != null) "Configured on this device with Android Keystore encryption." else "Not configured; online actions are disabled.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text("ChatGPT Plus does not include OpenAI API usage. Live AI requires separately billed API access configured only on the gateway.", style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            SectionCard("Health Connect") {
                Text(
                    when (healthGateway.availability()) {
                        HealthConnectAvailability.AVAILABLE -> if (allHealthPermissions) "Nutrition and hydration read/write permissions granted." else "Available; permissions are not fully granted."
                        HealthConnectAvailability.UPDATE_REQUIRED -> "The Health Connect provider must be updated."
                        HealthConnectAvailability.NOT_SUPPORTED -> "Health Connect is not supported on this device."
                    },
                )
                Text("Reads and writes happen only when you tap an action and confirm selected records. There is no silent or background sync.", style = MaterialTheme.typography.bodySmall)
                Button(
                    onClick = onRequestHealthPermissions,
                    enabled = healthAvailable,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Choose nutrition and hydration permissions") }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onImportHealth,
                        enabled = healthAvailable && allHealthPermissions,
                        modifier = Modifier.weight(1f),
                    ) { Text("Review import") }
                    OutlinedButton(
                        onClick = onExportHealth,
                        enabled = healthAvailable && allHealthPermissions,
                        modifier = Modifier.weight(1f),
                    ) { Text("Review write") }
                }
            }
        }
        item {
            SectionCard("Your local data") {
                Text("Journal entries, quick foods, settings, cached USDA references, suggestions, and sync receipts stay in the app database. Android backup is disabled.")
                OutlinedButton(onClick = onExportJson, modifier = Modifier.fillMaxWidth()) {
                    Text("Export local JSON")
                }
                OutlinedButton(onClick = onDeleteAll, modifier = Modifier.fillMaxWidth()) {
                    Text("Delete all local data")
                }
            }
        }
        item {
            SectionCard("Privacy boundaries") {
                Text("Manual entries, trends, and Health Connect drafts stay local unless you explicitly export or write them.")
                HorizontalDivider()
                Text("A selected photo leaves the device only after a per-photo consent screen. Dismissing a draft saves nothing.")
                HorizontalDivider()
                Text("USDA requests contain search text or an FDC id; they do not include your journal or photos.")
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