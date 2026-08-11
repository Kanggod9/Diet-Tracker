package io.github.kanggod9.diettracker.integration

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.MealType as HealthMealType
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.grams
import androidx.health.connect.client.units.kilocalories
import androidx.health.connect.client.units.micrograms
import androidx.health.connect.client.units.milligrams
import androidx.health.connect.client.units.milliliters
import io.github.kanggod9.diettracker.domain.AmountUnit
import io.github.kanggod9.diettracker.domain.DataSet
import io.github.kanggod9.diettracker.domain.EntryAmount
import io.github.kanggod9.diettracker.domain.EntryKind
import io.github.kanggod9.diettracker.domain.JournalEntry
import io.github.kanggod9.diettracker.domain.MealType
import io.github.kanggod9.diettracker.domain.NutrientKey
import io.github.kanggod9.diettracker.domain.NutrientProvenance
import io.github.kanggod9.diettracker.domain.Nutrients
import java.time.Instant
import java.time.ZoneId

enum class HealthConnectAvailability { AVAILABLE, UPDATE_REQUIRED, NOT_SUPPORTED }

data class HealthNutritionDraft(
    val name: String?,
    val mealType: MealType,
    val values: Map<NutrientKey, Double>,
    val sourceRecordId: String? = null,
)

data class HealthWriteResult(
    val nutritionRecordId: String? = null,
    val hydrationRecordId: String? = null,
)

object HealthNutritionMapper {
    fun toDraft(name: String?, mealType: MealType, nutrients: Nutrients): HealthNutritionDraft =
        HealthNutritionDraft(name, mealType, nutrients.values)

    fun fromDraft(draft: HealthNutritionDraft): Nutrients = Nutrients(draft.values)

    fun toNutritionRecord(
        entry: JournalEntry,
        clientRecordVersion: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): NutritionRecord {
        val start = entry.loggedAt
        val end = start.plusSeconds(60)
        val offset = start.atZone(zoneId).offset
        val nutrients = entry.nutrients
        return NutritionRecord(
            startTime = start,
            startZoneOffset = offset,
            endTime = end,
            endZoneOffset = offset,
            metadata = Metadata.manualEntry(entry.id, clientRecordVersion),
            biotin = nutrients.mass(NutrientKey.BIOTIN),
            caffeine = nutrients.mass(NutrientKey.CAFFEINE),
            calcium = nutrients.mass(NutrientKey.CALCIUM),
            energy = nutrients[NutrientKey.ENERGY]?.kilocalories,
            energyFromFat = nutrients[NutrientKey.ENERGY_FROM_FAT]?.kilocalories,
            chloride = nutrients.mass(NutrientKey.CHLORIDE),
            cholesterol = nutrients.mass(NutrientKey.CHOLESTEROL),
            chromium = nutrients.mass(NutrientKey.CHROMIUM),
            copper = nutrients.mass(NutrientKey.COPPER),
            dietaryFiber = nutrients.mass(NutrientKey.DIETARY_FIBER),
            folate = nutrients.mass(NutrientKey.FOLATE),
            folicAcid = nutrients.mass(NutrientKey.FOLIC_ACID),
            iodine = nutrients.mass(NutrientKey.IODINE),
            iron = nutrients.mass(NutrientKey.IRON),
            magnesium = nutrients.mass(NutrientKey.MAGNESIUM),
            manganese = nutrients.mass(NutrientKey.MANGANESE),
            molybdenum = nutrients.mass(NutrientKey.MOLYBDENUM),
            monounsaturatedFat = nutrients.mass(NutrientKey.MONOUNSATURATED_FAT),
            niacin = nutrients.mass(NutrientKey.NIACIN),
            pantothenicAcid = nutrients.mass(NutrientKey.PANTOTHENIC_ACID),
            phosphorus = nutrients.mass(NutrientKey.PHOSPHORUS),
            polyunsaturatedFat = nutrients.mass(NutrientKey.POLYUNSATURATED_FAT),
            potassium = nutrients.mass(NutrientKey.POTASSIUM),
            protein = nutrients.mass(NutrientKey.PROTEIN),
            riboflavin = nutrients.mass(NutrientKey.RIBOFLAVIN),
            saturatedFat = nutrients.mass(NutrientKey.SATURATED_FAT),
            selenium = nutrients.mass(NutrientKey.SELENIUM),
            sodium = nutrients.mass(NutrientKey.SODIUM),
            sugar = nutrients.mass(NutrientKey.TOTAL_SUGAR),
            thiamin = nutrients.mass(NutrientKey.THIAMIN),
            totalCarbohydrate = nutrients.mass(NutrientKey.TOTAL_CARBOHYDRATE),
            totalFat = nutrients.mass(NutrientKey.TOTAL_FAT),
            transFat = nutrients.mass(NutrientKey.TRANS_FAT),
            unsaturatedFat = nutrients.mass(NutrientKey.UNSATURATED_FAT),
            vitaminA = nutrients.mass(NutrientKey.VITAMIN_A),
            vitaminB12 = nutrients.mass(NutrientKey.VITAMIN_B12),
            vitaminB6 = nutrients.mass(NutrientKey.VITAMIN_B6),
            vitaminC = nutrients.mass(NutrientKey.VITAMIN_C),
            vitaminD = nutrients.mass(NutrientKey.VITAMIN_D),
            vitaminE = nutrients.mass(NutrientKey.VITAMIN_E),
            vitaminK = nutrients.mass(NutrientKey.VITAMIN_K),
            zinc = nutrients.mass(NutrientKey.ZINC),
            name = entry.name,
            mealType = entry.mealType.toHealthMealType(),
        )
    }

    fun fromNutritionRecord(record: NutritionRecord): JournalEntry {
        val values = buildMap {
            record.energy?.inKilocalories?.let { put(NutrientKey.ENERGY, it) }
            record.energyFromFat?.inKilocalories?.let { put(NutrientKey.ENERGY_FROM_FAT, it) }
            putMass(NutrientKey.BIOTIN, record.biotin)
            putMass(NutrientKey.CAFFEINE, record.caffeine)
            putMass(NutrientKey.CALCIUM, record.calcium)
            putMass(NutrientKey.CHLORIDE, record.chloride)
            putMass(NutrientKey.CHOLESTEROL, record.cholesterol)
            putMass(NutrientKey.CHROMIUM, record.chromium)
            putMass(NutrientKey.COPPER, record.copper)
            putMass(NutrientKey.DIETARY_FIBER, record.dietaryFiber)
            putMass(NutrientKey.FOLATE, record.folate)
            putMass(NutrientKey.FOLIC_ACID, record.folicAcid)
            putMass(NutrientKey.IODINE, record.iodine)
            putMass(NutrientKey.IRON, record.iron)
            putMass(NutrientKey.MAGNESIUM, record.magnesium)
            putMass(NutrientKey.MANGANESE, record.manganese)
            putMass(NutrientKey.MOLYBDENUM, record.molybdenum)
            putMass(NutrientKey.MONOUNSATURATED_FAT, record.monounsaturatedFat)
            putMass(NutrientKey.NIACIN, record.niacin)
            putMass(NutrientKey.PANTOTHENIC_ACID, record.pantothenicAcid)
            putMass(NutrientKey.PHOSPHORUS, record.phosphorus)
            putMass(NutrientKey.POLYUNSATURATED_FAT, record.polyunsaturatedFat)
            putMass(NutrientKey.POTASSIUM, record.potassium)
            putMass(NutrientKey.PROTEIN, record.protein)
            putMass(NutrientKey.RIBOFLAVIN, record.riboflavin)
            putMass(NutrientKey.SATURATED_FAT, record.saturatedFat)
            putMass(NutrientKey.SELENIUM, record.selenium)
            putMass(NutrientKey.SODIUM, record.sodium)
            putMass(NutrientKey.TOTAL_SUGAR, record.sugar)
            putMass(NutrientKey.THIAMIN, record.thiamin)
            putMass(NutrientKey.TOTAL_CARBOHYDRATE, record.totalCarbohydrate)
            putMass(NutrientKey.TOTAL_FAT, record.totalFat)
            putMass(NutrientKey.TRANS_FAT, record.transFat)
            putMass(NutrientKey.UNSATURATED_FAT, record.unsaturatedFat)
            putMass(NutrientKey.VITAMIN_A, record.vitaminA)
            putMass(NutrientKey.VITAMIN_B12, record.vitaminB12)
            putMass(NutrientKey.VITAMIN_B6, record.vitaminB6)
            putMass(NutrientKey.VITAMIN_C, record.vitaminC)
            putMass(NutrientKey.VITAMIN_D, record.vitaminD)
            putMass(NutrientKey.VITAMIN_E, record.vitaminE)
            putMass(NutrientKey.VITAMIN_K, record.vitaminK)
            putMass(NutrientKey.ZINC, record.zinc)
        }
        val provenance = values.keys.associateWith {
            NutrientProvenance(
                dataSet = DataSet.HEALTH_CONNECT,
                sourceId = record.metadata.id,
                sourceLabel = "Imported from Health Connect",
                retrievedAt = Instant.now(),
                verified = true,
            )
        }
        return JournalEntry(
            id = "health-nutrition-${record.metadata.id}",
            name = record.name?.takeIf { it.isNotBlank() } ?: "Health Connect nutrition",
            kind = EntryKind.FOOD,
            mealType = record.mealType.fromHealthMealType(),
            servingDescription = "Imported nutrition record",
            servingGrams = null,
            amount = EntryAmount(1.0, AmountUnit.SERVING),
            loggedAt = record.startTime,
            nutrients = Nutrients(values, provenance),
            note = "Imported after explicit Health Connect read.",
        )
    }

    fun toHydrationRecord(
        entry: JournalEntry,
        clientRecordVersion: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): HydrationRecord? {
        val waterGrams = entry.nutrients[NutrientKey.WATER] ?: return null
        val milliliters = when (entry.amount.unit) {
            AmountUnit.MILLILITRE -> entry.amount.value
            else -> waterGrams
        }
        if (milliliters <= 0.0) return null
        val start = entry.loggedAt
        val offset = start.atZone(zoneId).offset
        return HydrationRecord(
            startTime = start,
            startZoneOffset = offset,
            endTime = start.plusSeconds(60),
            endZoneOffset = offset,
            volume = milliliters.milliliters,
            metadata = Metadata.manualEntry("${entry.id}-hydration", clientRecordVersion),
        )
    }

    fun fromHydrationRecord(record: HydrationRecord): JournalEntry {
        val milliliters = record.volume.inMilliliters
        val source = NutrientProvenance(
            dataSet = DataSet.HEALTH_CONNECT,
            sourceId = record.metadata.id,
            sourceLabel = "Imported from Health Connect hydration",
            retrievedAt = Instant.now(),
            verified = true,
        )
        return JournalEntry(
            id = "health-hydration-${record.metadata.id}",
            name = "Water",
            kind = EntryKind.DRINK,
            mealType = MealType.SNACK,
            servingDescription = "$milliliters mL",
            servingGrams = null,
            amount = EntryAmount(milliliters, AmountUnit.MILLILITRE),
            loggedAt = record.startTime,
            nutrients = Nutrients(
                values = mapOf(NutrientKey.WATER to milliliters),
                provenance = mapOf(NutrientKey.WATER to source),
            ),
            note = "Imported after explicit Health Connect read.",
        )
    }

    private fun Nutrients.mass(key: NutrientKey): Mass? = this[key]?.let { value ->
        when (key.unit) {
            "g" -> value.grams
            "mg" -> value.milligrams
            "mcg" -> value.micrograms
            else -> error("Unsupported Health Connect mass unit for $key")
        }
    }

    private fun MutableMap<NutrientKey, Double>.putMass(key: NutrientKey, mass: Mass?) {
        mass ?: return
        put(
            key,
            when (key.unit) {
                "g" -> mass.inGrams
                "mg" -> mass.inMilligrams
                "mcg" -> mass.inMicrograms
                else -> error("Unsupported Health Connect mass unit for $key")
            },
        )
    }

    private fun MealType.toHealthMealType(): Int = when (this) {
        MealType.BREAKFAST -> HealthMealType.MEAL_TYPE_BREAKFAST
        MealType.LUNCH -> HealthMealType.MEAL_TYPE_LUNCH
        MealType.DINNER -> HealthMealType.MEAL_TYPE_DINNER
        MealType.SNACK, MealType.LATE_NIGHT, MealType.COOKING_OIL -> HealthMealType.MEAL_TYPE_SNACK
        MealType.UNKNOWN -> HealthMealType.MEAL_TYPE_UNKNOWN
    }

    private fun Int.fromHealthMealType(): MealType = when (this) {
        HealthMealType.MEAL_TYPE_BREAKFAST -> MealType.BREAKFAST
        HealthMealType.MEAL_TYPE_LUNCH -> MealType.LUNCH
        HealthMealType.MEAL_TYPE_DINNER -> MealType.DINNER
        HealthMealType.MEAL_TYPE_SNACK -> MealType.SNACK
        else -> MealType.UNKNOWN
    }
}

class HealthConnectGateway(private val context: Context) {
    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(NutritionRecord::class),
        HealthPermission.getWritePermission(NutritionRecord::class),
        HealthPermission.getReadPermission(HydrationRecord::class),
        HealthPermission.getWritePermission(HydrationRecord::class),
    )

    fun availability(): HealthConnectAvailability = when (HealthConnectClient.getSdkStatus(context)) {
        HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.AVAILABLE
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
            HealthConnectAvailability.UPDATE_REQUIRED
        else -> HealthConnectAvailability.NOT_SUPPORTED
    }

    fun clientOrNull(): HealthConnectClient? =
        if (availability() == HealthConnectAvailability.AVAILABLE) {
            HealthConnectClient.getOrCreate(context)
        } else {
            null
        }

    suspend fun grantedPermissions(): Set<String> =
        clientOrNull()?.permissionController?.getGrantedPermissions().orEmpty()

    fun canReadAndWrite(granted: Set<String>): Boolean = permissions.all { it in granted }

    suspend fun writeEntry(entry: JournalEntry): HealthWriteResult {
        val client = clientOrNull() ?: error("Health Connect is unavailable")
        check(canReadAndWrite(client.permissionController.getGrantedPermissions())) {
            "Health Connect nutrition and hydration permissions are required"
        }
        val version = Instant.now().toEpochMilli()
        val records = mutableListOf<Record>()
        val recordKinds = mutableListOf<String>()

        val nutritionKeys = entry.nutrients.values.keys - NutrientKey.WATER - NutrientKey.ADDED_SUGAR
        if (nutritionKeys.isNotEmpty()) {
            records += HealthNutritionMapper.toNutritionRecord(entry, version)
            recordKinds += NUTRITION_RECORD
        }
        HealthNutritionMapper.toHydrationRecord(entry, version)?.let {
            records += it
            recordKinds += HYDRATION_RECORD
        }
        require(records.isNotEmpty()) { "This entry has no Health Connect-compatible values" }

        val response = client.insertRecords(records)
        var nutritionId: String? = null
        var hydrationId: String? = null
        response.recordIdsList.forEachIndexed { index, id ->
            when (recordKinds[index]) {
                NUTRITION_RECORD -> nutritionId = id
                HYDRATION_RECORD -> hydrationId = id
            }
        }
        return HealthWriteResult(nutritionId, hydrationId)
    }

    suspend fun readEntries(start: Instant, end: Instant): List<JournalEntry> {
        require(start.isBefore(end))
        val client = clientOrNull() ?: error("Health Connect is unavailable")
        check(canReadAndWrite(client.permissionController.getGrantedPermissions())) {
            "Health Connect nutrition and hydration permissions are required"
        }
        val filter = TimeRangeFilter.between(start, end)
        val nutrition = readAllNutrition(client, filter).map(HealthNutritionMapper::fromNutritionRecord)
        val hydration = readAllHydration(client, filter).map(HealthNutritionMapper::fromHydrationRecord)
        return (nutrition + hydration).sortedByDescending { it.loggedAt }
    }

    private suspend fun readAllNutrition(
        client: HealthConnectClient,
        filter: TimeRangeFilter,
    ): List<NutritionRecord> {
        val records = mutableListOf<NutritionRecord>()
        var token: String? = null
        do {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = NutritionRecord::class,
                    timeRangeFilter = filter,
                    pageToken = token,
                ),
            )
            records += response.records
            token = response.pageToken
        } while (token != null)
        return records
    }

    private suspend fun readAllHydration(
        client: HealthConnectClient,
        filter: TimeRangeFilter,
    ): List<HydrationRecord> {
        val records = mutableListOf<HydrationRecord>()
        var token: String? = null
        do {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = HydrationRecord::class,
                    timeRangeFilter = filter,
                    pageToken = token,
                ),
            )
            records += response.records
            token = response.pageToken
        } while (token != null)
        return records
    }

    companion object {
        const val NUTRITION_RECORD = "nutrition"
        const val HYDRATION_RECORD = "hydration"
    }
}
