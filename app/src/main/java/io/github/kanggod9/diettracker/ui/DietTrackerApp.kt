package io.github.kanggod9.diettracker.ui
import io.github.kanggod9.diettracker.domain.NutrientKey
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.Typography
import androidx.activity.compose.BackHandler

import android.Manifest
import android.content.Context
import android.os.Build
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.health.connect.client.PermissionController
import io.github.kanggod9.diettracker.data.LocalStore
import io.github.kanggod9.diettracker.data.SecureConfigStore
import io.github.kanggod9.diettracker.domain.GuidanceProfiles
import io.github.kanggod9.diettracker.domain.GuidanceRegion
import io.github.kanggod9.diettracker.domain.JournalEntry
import io.github.kanggod9.diettracker.domain.NutrientAggregator
import io.github.kanggod9.diettracker.domain.NutrientTargets
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
import io.github.kanggod9.diettracker.reminder.MealReminderPreferences
import io.github.kanggod9.diettracker.reminder.MealReminderScheduler
import io.github.kanggod9.diettracker.reminder.notificationPermissionGranted
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

internal val Turquoise = Color(0xFF40E0D0)
internal val DarkTurquoise = Color(0xFF0B6F70)
internal val FlameOrange = Color(0xFFFF8C42)
internal val Sage = Turquoise
internal val DeepSage = DarkTurquoise
internal val Mint = Color(0xFFE8F7F6)
internal val CanvasColor = Color(0xFFF7F9F7)

private val ComfortableTypography = Typography(
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 17.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 15.sp, lineHeight = 22.sp),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 24.sp,
        lineHeight = 31.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Medium,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
    ),
)

private const val PHOTO_CONSENT_SKIP = "photo_consent_skip"

@Composable
fun DietTrackerApp(store: LocalStore) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val secureConfig = remember { SecureConfigStore(context.applicationContext) }
    val gatewayClient = remember { GatewayHttpClient { secureConfig.connection() } }
    val usdaSource = remember { CachingUsdaDataSource(GatewayUsdaDataSource(gatewayClient), store) }
    val healthGateway = remember { HealthConnectGateway(context.applicationContext) }

    var screen by remember { mutableStateOf(Screen.LOGS) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var entries by remember { mutableStateOf(store.entries()) }
    var quickFoods by remember { mutableStateOf(store.quickFoods()) }
    var addOpen by remember { mutableStateOf(false) }
    var photoSourceOpen by remember { mutableStateOf(false) }
    var reviewSeed by remember { mutableStateOf<ReviewSeed?>(null) }
    var usdaRequest by remember { mutableStateOf<UsdaLookupRequest?>(null) }
    var pendingPhoto by remember { mutableStateOf<Uri?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var cameraFile by remember { mutableStateOf<File?>(null) }
    var healthImports by remember { mutableStateOf<List<JournalEntry>?>(null) }
    var healthExportOpen by remember { mutableStateOf(false) }
    var deleteAllOpen by remember { mutableStateOf(false) }
    var busyMessage by remember { mutableStateOf<String?>(null) }
    var pendingExportJson by remember { mutableStateOf<String?>(null) }
    var gatewayRevision by remember { mutableIntStateOf(0) }
    var targetRevision by remember { mutableIntStateOf(0) }
    var healthPermissionRevision by remember { mutableIntStateOf(0) }
    var reminderRevision by remember { mutableIntStateOf(0) }
    var notificationsGranted by remember { mutableStateOf(notificationPermissionGranted(context)) }
    var nutrientHistory by remember { mutableStateOf<NutrientKey?>(null) }
    var foodScoreHistoryOpen by remember { mutableStateOf(false) }
    var guidanceRegion by remember {
        mutableStateOf(
            runCatching {
                GuidanceRegion.valueOf(store.setting("guidance_region") ?: GuidanceRegion.SINGAPORE.name)
            }.getOrDefault(GuidanceRegion.SINGAPORE),
        )
    }

    fun refresh() {
        entries = store.entries()
        quickFoods = store.quickFoods()
    }

    fun message(value: String) {
        scope.launch { snackbar.showSnackbar(value.take(240)) }
    }

    fun analyzePhoto(uri: Uri) {
        scope.launch {
            busyMessage = "Analyzing photo"
            try {
                val photo = readPhotoForAnalysis(context, uri)
                val draft = GatewayPhotoProvider(gatewayClient).analyze(photo.bytes, true, photo.mimeType)
                reviewSeed = ReviewSeed(
                    entry = draft.toConfirmedEntry(""),
                    title = "Review AI photo estimate",
                    sourceNotes = listOf(
                        "${draft.sourceType.name.lowercase().replaceFirstChar { it.titlecase() }} photo · ${(draft.confidence * 100).toInt()}% confidence",
                    ) + draft.warnings,
                    photoDraft = draft,
                    returnDestination = ReviewReturnDestination.PHOTO_SOURCE,
                )
            } catch (error: Exception) {
                message(userFacingError(error))
                photoSourceOpen = true
            } finally {
                cameraFile?.delete()
                cameraFile = null
                busyMessage = null
            }
        }
    }

    fun acceptPhoto(uri: Uri?) {
        if (uri == null) return
        if (store.setting(PHOTO_CONSENT_SKIP) == "true") analyzePhoto(uri) else pendingPhoto = uri
    }

    fun writeToHealth(entry: JournalEntry, automatic: Boolean, replace: Boolean = false) {
        scope.launch {
            if (automatic) {
                val granted = runCatching { healthGateway.grantedPermissions() }.getOrDefault(emptySet())
                if (!healthGateway.canReadAndWrite(granted) || !shouldAutoUpdateHealth(entry)) {
                    message("Saved locally.")
                    return@launch
                }
            }
            try {
                val result = if (replace) healthGateway.replaceEntry(entry) else healthGateway.writeEntry(entry)
                val now = Instant.now()
                result.nutritionRecordId?.let {
                    store.recordHealthExport(entry.id, HealthConnectGateway.NUTRITION_RECORD, it, now)
                }
                result.hydrationRecordId?.let {
                    store.recordHealthExport(entry.id, HealthConnectGateway.HYDRATION_RECORD, it, now)
                }
                if (automatic) {
                    message(if (replace) "Saved locally and updated Health Connect." else "Saved locally and written to Health Connect.")
                }
            } catch (error: Exception) {
                if (automatic) message("Saved locally. Health Connect update failed: ${userFacingError(error)}")
                else message(userFacingError(error))
            }
        }
    }

    fun deleteWithHealth(entry: JournalEntry) {
        store.delete(entry.id)
        refresh()
        scope.launch {
            val granted = runCatching { healthGateway.grantedPermissions() }.getOrDefault(emptySet())
            if (!healthGateway.canReadAndWrite(granted) || !shouldAutoUpdateHealth(entry)) {
                message("Deleted locally.")
                return@launch
            }
            try {
                healthGateway.deleteEntry(entry.id)
                message("Deleted locally and removed from Health Connect.")
            } catch (error: Exception) {
                message("Deleted locally. Health Connect removal failed: ${userFacingError(error)}")
            }
        }
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) photoSourceOpen = true else acceptPhoto(uri)
    }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = cameraUri
        cameraUri = null
        if (success) {
            acceptPhoto(uri)
        } else {
            cameraFile?.delete()
            cameraFile = null
            photoSourceOpen = true
        }
    }
    val exportDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        val payload = pendingExportJson
        pendingExportJson = null
        if (uri != null && payload != null) scope.launch {
            runCatching { writeExportDocument(context, uri, payload) }
                .onSuccess { message("Export created.") }
                .onFailure { message("Export failed.") }
        }
    }
    val healthPermissions = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) { granted ->
        healthPermissionRevision++
        message(if (healthGateway.canReadAndWrite(granted)) "Health Connect permissions granted." else "Health Connect permissions incomplete.")
    }
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationsGranted = granted
        if (granted) {
            MealReminderPreferences.setMasterEnabled(store, true)
            MealReminderScheduler.sync(context.applicationContext, store)
            reminderRevision++
            message("Meal reminders enabled.")
        } else {
            message("Notification permission is required for meal reminders.")
        }
    }
    LaunchedEffect(Unit) {
        MealReminderScheduler.sync(context.applicationContext, store)
    }

    val selectedAggregate = remember(entries, selectedDate) {
        NutrientAggregator.aggregate(entries.filter { it.localDate() == selectedDate }.map { it.nutrients })
    }
    val trend = remember(entries) {
        TrendAnalyzer.summarize(entries.toDailyTotals(), TrendWindow.DAYS_30, LocalDate.now())
    }
    val currentProfile = remember(guidanceRegion) {
        GuidanceProfiles.all.first { it.region == guidanceRegion }
    }
    val targets = remember(guidanceRegion, targetRevision) {
        NutrientTargets.resolved(guidanceRegion, store.settings())
    }
    val allSuggestions = remember(entries, currentProfile) {
        GuidanceProfiles.all.flatMap {
            SuggestionEngine.generate(
                NutrientAggregator.aggregate(entries.filter { entry -> entry.localDate() == LocalDate.now() }.map { entry -> entry.nutrients }),
                trend,
                it,
            )
        }
    }
    androidx.compose.runtime.LaunchedEffect(allSuggestions) { store.replaceSuggestions(allSuggestions) }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Turquoise,
            onPrimary = Color(0xFF003C3A),
            secondary = DarkTurquoise,
            background = CanvasColor,
            surface = Color.White,
            surfaceVariant = Mint,
        ),
        typography = ComfortableTypography,
    ) {
        BackHandler(enabled = nutrientHistory != null || foodScoreHistoryOpen) {
            nutrientHistory = null
            foodScoreHistoryOpen = false
        }
        Scaffold(
            containerColor = CanvasColor,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                if (nutrientHistory == null && !foodScoreHistoryOpen) {
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
                }
            },
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                val historyKey = nutrientHistory
                if (foodScoreHistoryOpen) {
                    FoodScoreHistoryScreen(
                        entries = entries,
                        initialDate = selectedDate,
                        onBack = { foodScoreHistoryOpen = false },
                    )
                } else if (historyKey != null) {
                    NutrientHistoryScreen(
                        key = historyKey,
                        entries = entries,
                        initialDate = selectedDate,
                        target = targets[historyKey],
                        onBack = { nutrientHistory = null },
                    )
                } else {
                    when (screen) {
                        Screen.LOGS -> LogsScreen(
                            entries = entries,
                            quickFoods = quickFoods,
                            selectedDate = selectedDate,
                            targets = targets,
                            suggestions = SuggestionEngine.generate(selectedAggregate, trend, currentProfile),
                            onDateSelected = { selectedDate = it },
                            onLog = { addOpen = true },
                            onNutrientSelected = { nutrientHistory = it },
                            onEdit = { reviewSeed = ReviewSeed(it, "Edit entry") },
                            onFoodScoreSelected = { foodScoreHistoryOpen = true },
                            onDelete = ::deleteWithHealth,
                        )
                        Screen.TARGET -> TargetScreen(
                            store = store,
                            region = guidanceRegion,
                            onRegionChanged = {
                                guidanceRegion = it
                                store.setSetting("guidance_region", it.name)
                                targetRevision++
                            },
                            onTargetsChanged = { targetRevision++ },
                            onNutrientSelected = { nutrientHistory = it },
                        )
                        Screen.SETTINGS -> SettingsScreen(
                            store = store,
                            secureConfig = secureConfig,
                            healthGateway = healthGateway,
                            gatewayRevision = gatewayRevision,
                            healthPermissionRevision = healthPermissionRevision,
                            reminderRevision = reminderRevision,
                            notificationPermissionGranted = notificationsGranted,
                            onGatewayChanged = { gatewayRevision++; message("Gateway settings updated.") },
                            onReminderMasterChanged = { enabled ->
                                if (!enabled) {
                                    MealReminderPreferences.setMasterEnabled(store, false)
                                    MealReminderScheduler.sync(context.applicationContext, store)
                                    reminderRevision++
                                } else if (notificationPermissionGranted(context)) {
                                    notificationsGranted = true
                                    MealReminderPreferences.setMasterEnabled(store, true)
                                    MealReminderScheduler.sync(context.applicationContext, store)
                                    reminderRevision++
                                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            onReminderMealChanged = { meal, enabled ->
                                MealReminderPreferences.setMealEnabled(store, meal, enabled)
                                MealReminderScheduler.sync(context.applicationContext, store)
                                reminderRevision++
                            },
                            onReminderTimeChanged = { meal, time ->
                                MealReminderPreferences.setMealTime(store, meal, time)
                                MealReminderScheduler.sync(context.applicationContext, store)
                                reminderRevision++
                            },
                            onRequestNotificationPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            onRequestHealthPermissions = { healthPermissions.launch(healthGateway.permissions) },
                            onImportHealth = {
                                scope.launch {
                                    busyMessage = "Reading Health Connect"
                                    try {
                                        val now = Instant.now()
                                        healthImports = healthGateway.readEntries(
                                            now.minus(30, ChronoUnit.DAYS),
                                            now.plusSeconds(1),
                                        )
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
        }

        if (addOpen) LogChooserDialog(
            quickFoods = quickFoods,
            onlineConfigured = secureConfig.isConfigured(),
            onDismiss = { addOpen = false },
            onDetailedManual = {
                addOpen = false
                reviewSeed = ReviewSeed(
                    null,
                    "Detailed manual",
                    returnDestination = ReviewReturnDestination.LOG_CHOOSER,
                )
            },
            onTextParsed = { addOpen = false; usdaRequest = UsdaLookupRequest(it.name, parsed = it) },
            onUsdaSearch = { addOpen = false; usdaRequest = UsdaLookupRequest("") },
            onPhoto = {
                addOpen = false
                if (secureConfig.isConfigured()) photoSourceOpen = true
                else message("Configure the private gateway in Settings.")
            },
            onQuickFood = {
                addOpen = false
                reviewSeed = ReviewSeed(
                    it.toJournalEntry(),
                    "Review quick food",
                    returnDestination = ReviewReturnDestination.LOG_CHOOSER,
                )
            },
            onDeleteQuickFood = { quick ->
                store.deleteQuickFood(quick.id)
                refresh()
                message("Quick food deleted.")
            },
        )

        if (photoSourceOpen) PhotoSourceDialog(
            onDismiss = {
                photoSourceOpen = false
                addOpen = true
            },
            onCamera = {
                photoSourceOpen = false
                runCatching { createCameraCapture(context) }
                    .onSuccess { capture ->
                        cameraUri = capture.first
                        cameraFile = capture.second
                        takePhoto.launch(capture.first)
                    }
                    .onFailure {
                        message("Camera could not be opened.")
                        photoSourceOpen = true
                    }
            },
            onAlbum = {
                photoSourceOpen = false
                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
        )

        reviewSeed?.let { seed ->
            EntryEditorDialog(
                seed = seed,
                nutrientTargets = targets,
                onDismiss = {
                    reviewSeed = null
                    when (seed.returnDestination) {
                        ReviewReturnDestination.CLOSE -> Unit
                        ReviewReturnDestination.LOG_CHOOSER -> addOpen = true
                        ReviewReturnDestination.PHOTO_SOURCE -> photoSourceOpen = true
                    }
                },
                onSave = { entry, saveQuick ->
                    val isNew = entries.none { it.id == entry.id }
                    val datedEntry = if (isNew && selectedDate != LocalDate.now()) {
                        entry.copy(
                            loggedAt = selectedDate.atTime(LocalTime.now())
                                .atZone(ZoneId.systemDefault()).toInstant(),
                        )
                    } else entry
                    store.save(datedEntry)
                    if (saveQuick) store.saveQuickFood(datedEntry.toQuickFood())
                    refresh()
                    reviewSeed = null
                    writeToHealth(datedEntry, automatic = true, replace = !isNew)

                },
                onFindUsda = seed.photoDraft?.let { draft ->
                    {
                        reviewSeed = null
                        usdaRequest = UsdaLookupRequest(draft.usdaQuery, photoDraft = draft, returnSeed = seed)
                    }
                },
            )
        }

        usdaRequest?.let { request ->
            UsdaSearchDialog(
                request = request,
                dataSource = usdaSource,
                onBack = {
                    val returnSeed = request.returnSeed
                    usdaRequest = null
                    if (returnSeed != null) {
                        reviewSeed = returnSeed
                    } else {
                        addOpen = true
                    }
                },
                onDismiss = { usdaRequest = null },
                onReview = { usdaRequest = null; reviewSeed = it },
            )
        }

        pendingPhoto?.let { uri ->
            PhotoConsentDialog(
                endpoint = secureConfig.configuredEndpoint(),
                onDismiss = {
                    pendingPhoto = null
                    cameraFile?.delete()
                    cameraFile = null
                    photoSourceOpen = true
                },
                onAnalyze = { dontShowAgain ->
                    if (dontShowAgain) store.setSetting(PHOTO_CONSENT_SKIP, "true")
                    pendingPhoto = null
                    analyzePhoto(uri)
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
                    message("Imported ${selected.size} record${if (selected.size == 1) "" else "s"}.")
                },
            )
        }

        if (healthExportOpen) HealthExportDialog(
            entries = entries,
            onDismiss = { healthExportOpen = false },
            onConfirm = { selected ->
                healthExportOpen = false
                scope.launch {
                    busyMessage = "Writing Health Connect"
                    var written = 0
                    try {
                        selected.forEach { entry ->
                            val result = healthGateway.replaceEntry(entry)
                            val now = Instant.now()
                            result.nutritionRecordId?.let {
                                store.recordHealthExport(entry.id, HealthConnectGateway.NUTRITION_RECORD, it, now)
                            }
                            result.hydrationRecordId?.let {
                                store.recordHealthExport(entry.id, HealthConnectGateway.HYDRATION_RECORD, it, now)
                            }
                            written++
                        }
                        message("Wrote $written record${if (written == 1) "" else "s"}.")
                    } catch (error: Exception) {
                        message("Wrote $written before stopping. ${userFacingError(error)}")
                    } finally {
                        busyMessage = null
                    }
                }
            },
        )

        if (deleteAllOpen) AlertDialog(
            onDismissRequest = { deleteAllOpen = false },
            title = { Text("Delete all local data?") },
            text = { Text("Journal, targets, settings, caches, and gateway configuration will be deleted.") },
            confirmButton = {
                Button(onClick = {
                    store.clearAllLocalData()
                    secureConfig.clearGateway()
                    deleteAllOpen = false
                    gatewayRevision++
                    targetRevision++
                    guidanceRegion = GuidanceRegion.SINGAPORE
                    refresh()
                    message("All local data deleted.")
                }) { Text("Delete all") }
            },
            dismissButton = { TextButton(onClick = { deleteAllOpen = false }) { Text("Cancel") } },
        )

        busyMessage?.let { BusyOverlay(it) }
    }
}

private fun createCameraCapture(context: Context): Pair<Uri, File> {
    val directory = File(context.cacheDir, "camera").apply { mkdirs() }
    val file = File.createTempFile("diet-photo-", ".jpg", directory)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    return uri to file
}

internal fun shouldAutoUpdateHealth(entry: JournalEntry): Boolean = !entry.id.startsWith("health-")

private fun userFacingError(error: Throwable): String = when (error) {
    is IllegalArgumentException -> error.message ?: "Invalid data."
    else -> error.message?.takeIf { it.length <= 180 } ?: "The operation could not be completed."
}
