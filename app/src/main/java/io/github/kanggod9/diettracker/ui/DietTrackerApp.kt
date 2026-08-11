package io.github.kanggod9.diettracker.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.health.connect.client.PermissionController
import io.github.kanggod9.diettracker.data.LocalStore
import io.github.kanggod9.diettracker.data.SecureConfigStore
import io.github.kanggod9.diettracker.domain.GuidanceProfiles
import io.github.kanggod9.diettracker.domain.GuidanceRegion
import io.github.kanggod9.diettracker.domain.JournalEntry
import io.github.kanggod9.diettracker.domain.NutrientAggregator
import io.github.kanggod9.diettracker.domain.QuickFood
import io.github.kanggod9.diettracker.domain.SuggestionEngine
import io.github.kanggod9.diettracker.domain.TrendAnalyzer
import io.github.kanggod9.diettracker.domain.TrendWindow
import io.github.kanggod9.diettracker.domain.localDate
import io.github.kanggod9.diettracker.integration.CachingUsdaDataSource
import io.github.kanggod9.diettracker.integration.GatewayHttpClient
import io.github.kanggod9.diettracker.integration.GatewayPhotoProvider
import io.github.kanggod9.diettracker.integration.GatewayUsdaDataSource
import io.github.kanggod9.diettracker.integration.HealthConnectGateway
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

internal val Sage = Color(0xFF276749)
internal val DeepSage = Color(0xFF174D38)
internal val Mint = Color(0xFFE2F2E9)
internal val CanvasColor = Color(0xFFF7F9F5)

@Composable
fun DietTrackerApp(store: LocalStore) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val secureConfig = remember { SecureConfigStore(context.applicationContext) }
    val gatewayClient = remember { GatewayHttpClient { secureConfig.connection() } }
    val usdaSource = remember { CachingUsdaDataSource(GatewayUsdaDataSource(gatewayClient), store) }
    val healthGateway = remember { HealthConnectGateway(context.applicationContext) }

    var screen by remember { mutableStateOf(Screen.TODAY) }
    var entries by remember { mutableStateOf(store.entries()) }
    var quickFoods by remember { mutableStateOf(store.quickFoods()) }
    var addOpen by remember { mutableStateOf(false) }
    var reviewSeed by remember { mutableStateOf<ReviewSeed?>(null) }
    var usdaRequest by remember { mutableStateOf<UsdaLookupRequest?>(null) }
    var pendingPhoto by remember { mutableStateOf<Uri?>(null) }
    var healthImports by remember { mutableStateOf<List<JournalEntry>?>(null) }
    var healthExportOpen by remember { mutableStateOf(false) }
    var deleteAllOpen by remember { mutableStateOf(false) }
    var busyMessage by remember { mutableStateOf<String?>(null) }
    var pendingExportJson by remember { mutableStateOf<String?>(null) }
    var gatewayRevision by remember { mutableStateOf(0) }
    var healthPermissionRevision by remember { mutableStateOf(0) }

    fun refresh() {
        entries = store.entries()
        quickFoods = store.quickFoods()
    }
    fun message(value: String) {
        scope.launch { snackbar.showSnackbar(value.take(240)) }
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) {
        pendingPhoto = it
    }
    val exportDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        val payload = pendingExportJson
        pendingExportJson = null
        if (uri != null && payload != null) scope.launch {
            runCatching { writeExportDocument(context, uri, payload) }
                .onSuccess { message("Local export created.") }
                .onFailure { message("Export failed. No journal data was changed.") }
        }
    }
    val healthPermissions = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) { granted ->
        healthPermissionRevision++
        message(if (healthGateway.canReadAndWrite(granted)) "Health Connect permissions granted." else "Health Connect permissions were not fully granted.")
    }

    val todayAggregate = remember(entries) {
        NutrientAggregator.aggregate(entries.filter { it.localDate() == LocalDate.now() }.map { it.nutrients })
    }
    val trend = remember(entries) {
        TrendAnalyzer.summarize(entries.toDailyTotals(), TrendWindow.DAYS_30, LocalDate.now())
    }
    val currentProfile = remember(entries, gatewayRevision) {
        val region = runCatching {
            GuidanceRegion.valueOf(store.setting("guidance_region") ?: GuidanceRegion.SINGAPORE.name)
        }.getOrDefault(GuidanceRegion.SINGAPORE)
        GuidanceProfiles.all.first { it.region == region }
    }
    val allSuggestions = remember(entries, currentProfile) {
        GuidanceProfiles.all.flatMap { SuggestionEngine.generate(todayAggregate, trend, it) }
    }
    LaunchedEffect(allSuggestions) { store.replaceSuggestions(allSuggestions) }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Sage,
            onPrimary = Color.White,
            secondary = DeepSage,
            background = CanvasColor,
            surface = Color.White,
            surfaceVariant = Mint,
        ),
    ) {
        Scaffold(
            containerColor = CanvasColor,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                NavigationBar(containerColor = Color.White) {
                    Screen.entries.forEach { item ->
                        NavigationBarItem(
                            selected = screen == item,
                            onClick = { screen = item },
                            icon = { Icon(item.icon, item.label) },
                            label = { Text(item.label) },
                        )
                    }
                }
            },
            floatingActionButton = {
                if (screen == Screen.TODAY) ExtendedFloatingActionButton(
                    onClick = { addOpen = true },
                    icon = { Icon(Icons.Outlined.Add, null) },
                    text = { Text("Log food or drink") },
                )
            },
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                when (screen) {
                    Screen.TODAY -> TodayScreen(
                        entries = entries,
                        quickFoods = quickFoods,
                        profile = currentProfile.region,
                        suggestions = SuggestionEngine.generate(todayAggregate, trend, currentProfile),
                        onLog = { addOpen = true },
                        onPhoto = {
                            if (secureConfig.isConfigured()) photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            ) else message("Configure the private HTTPS gateway in Settings before photo analysis.")
                        },
                        onEdit = { reviewSeed = ReviewSeed(it, "Edit entry") },
                        onDelete = { store.delete(it.id); refresh() },
                    )
                    Screen.HISTORY -> HistoryScreen(
                        entries,
                        onEdit = { reviewSeed = ReviewSeed(it, "Edit entry") },
                        onDelete = { store.delete(it.id); refresh() },
                    )
                    Screen.ANALYSIS -> AnalysisScreen(entries)
                    Screen.SETTINGS -> SettingsScreen(
                        store = store,
                        secureConfig = secureConfig,
                        healthGateway = healthGateway,
                        gatewayRevision = gatewayRevision,
                        healthPermissionRevision = healthPermissionRevision,
                        onGatewayChanged = { gatewayRevision++; message("Gateway settings updated.") },
                        onGuidanceChanged = { gatewayRevision++ },
                        onRequestHealthPermissions = { healthPermissions.launch(healthGateway.permissions) },
                        onImportHealth = {
                            scope.launch {
                                busyMessage = "Reading the last 30 days from Health Connect"
                                try {
                                    val now = Instant.now()
                                    healthImports = healthGateway.readEntries(now.minus(30, ChronoUnit.DAYS), now.plusSeconds(1))
                                } catch (error: Exception) {
                                    message(userFacingError(error))
                                } finally {
                                    busyMessage = null
                                }
                            }
                        },
                        onExportHealth = { healthExportOpen = true },
                        onExportJson = {
                            pendingExportJson = ExportJson.encode(store.snapshot())
                            exportDocument.launch("diet-tracker-${LocalDate.now()}.json")
                        },
                        onDeleteAll = { deleteAllOpen = true },
                    )
                }
            }
        }
        if (addOpen) LogChooserDialog(
            quickFoods = quickFoods,
            onlineConfigured = secureConfig.isConfigured(),
            onDismiss = { addOpen = false },
            onDetailedManual = { addOpen = false; reviewSeed = ReviewSeed(null, "Manual food or drink") },
            onTextParsed = { addOpen = false; usdaRequest = UsdaLookupRequest(it.name, parsed = it) },
            onUsdaSearch = { addOpen = false; usdaRequest = UsdaLookupRequest("") },
            onPhoto = {
                addOpen = false
                if (secureConfig.isConfigured()) photoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                ) else message("Configure the private HTTPS gateway in Settings first.")
            },
            onQuickFood = { addOpen = false; reviewSeed = ReviewSeed(it.toJournalEntry(), "Review quick food") },
        )

        reviewSeed?.let { seed ->
            EntryEditorDialog(
                seed = seed,
                onDismiss = { reviewSeed = null },
                onSave = { entry, saveQuick ->
                    store.save(entry)
                    if (saveQuick) store.saveQuickFood(entry.toQuickFood())
                    refresh()
                    reviewSeed = null
                    message("Saved locally.")
                },
                onFindUsda = seed.photoDraft?.let { draft ->
                    { reviewSeed = null; usdaRequest = UsdaLookupRequest(draft.usdaQuery, photoDraft = draft) }
                },
            )
        }

        usdaRequest?.let { request ->
            UsdaSearchDialog(
                request = request,
                dataSource = usdaSource,
                onDismiss = { usdaRequest = null },
                onReview = { usdaRequest = null; reviewSeed = it },
            )
        }

        pendingPhoto?.let { uri ->
            PhotoConsentDialog(
                endpoint = secureConfig.configuredEndpoint(),
                onDismiss = { pendingPhoto = null },
                onAnalyze = {
                    scope.launch {
                        busyMessage = "Analyzing photo with OpenAI through your private gateway"
                        try {
                            val photo = readPhotoForAnalysis(context, uri)
                            val draft = GatewayPhotoProvider(gatewayClient).analyze(photo.bytes, true, photo.mimeType)
                            reviewSeed = ReviewSeed(
                                entry = draft.toConfirmedEntry("AI photo draft reviewed before local save."),
                                title = "Review AI photo estimate",
                                sourceNotes = listOf(
                                    "${draft.sourceType.name.lowercase().replaceFirstChar { it.titlecase() }} photo; confidence ${(draft.confidence * 100).toInt()}%.",
                                    "AI estimates stay unverified. Visible package-label fields become confirmed only when you save.",
                                ) + draft.warnings,
                                photoDraft = draft,
                            )
                            pendingPhoto = null
                        } catch (error: Exception) {
                            message(userFacingError(error))
                        } finally {
                            busyMessage = null
                        }
                    }
                },
            )
        }

        healthImports?.let { imported ->
            HealthImportReviewDialog(
                entries = imported,
                onDismiss = { healthImports = null },
                onConfirm = { selected ->
                    selected.forEach(store::save)
                    healthImports = null
                    refresh()
                    message("Imported ${selected.size} reviewed Health Connect record(s) locally.")
                },
            )
        }

        if (healthExportOpen) HealthExportDialog(
            entries = entries,
            onDismiss = { healthExportOpen = false },
            onConfirm = { selected ->
                healthExportOpen = false
                scope.launch {
                    busyMessage = "Writing selected records to Health Connect"
                    var written = 0
                    try {
                        selected.forEach { entry ->
                            val result = healthGateway.writeEntry(entry)
                            val now = Instant.now()
                            result.nutritionRecordId?.let {
                                store.recordHealthExport(entry.id, HealthConnectGateway.NUTRITION_RECORD, it, now)
                            }
                            result.hydrationRecordId?.let {
                                store.recordHealthExport(entry.id, HealthConnectGateway.HYDRATION_RECORD, it, now)
                            }
                            written++
                        }
                        message("Wrote $written selected record(s) to Health Connect.")
                    } catch (error: Exception) {
                        message("Wrote $written record(s) before stopping. ${userFacingError(error)}")
                    } finally {
                        busyMessage = null
                    }
                }
            },
        )

        if (deleteAllOpen) AlertDialog(
            onDismissRequest = { deleteAllOpen = false },
            title = { Text("Delete all local data?") },
            text = {
                Text("This permanently removes the journal, quick foods, settings, suggestions, USDA cache, sync history, and encrypted gateway configuration from this device.")
            },
            confirmButton = {
                Button(onClick = {
                    store.clearAllLocalData()
                    secureConfig.clearGateway()
                    deleteAllOpen = false
                    gatewayRevision++
                    refresh()
                    message("All Diet Tracker local data was deleted.")
                }) { Text("Delete all") }
            },
            dismissButton = { TextButton(onClick = { deleteAllOpen = false }) { Text("Cancel") } },
        )

        busyMessage?.let { BusyOverlay(it) }
    }
}

private fun userFacingError(error: Throwable): String = when (error) {
    is IllegalArgumentException -> error.message ?: "The selected data is invalid."
    else -> error.message?.takeIf { it.length <= 180 } ?: "The operation could not be completed."
}