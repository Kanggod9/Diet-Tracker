package io.github.kanggod9.diettracker.integration

import io.github.kanggod9.diettracker.domain.AmountUnit
import io.github.kanggod9.diettracker.domain.DataSet
import io.github.kanggod9.diettracker.domain.EntryAmount
import io.github.kanggod9.diettracker.domain.EntryKind
import io.github.kanggod9.diettracker.domain.JournalEntry
import io.github.kanggod9.diettracker.domain.MealType
import io.github.kanggod9.diettracker.domain.NutrientKey
import io.github.kanggod9.diettracker.domain.NutrientProvenance
import io.github.kanggod9.diettracker.domain.Nutrients
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Base64
import java.util.UUID

enum class PhotoSourceType { INGREDIENT, PACKAGE }
enum class AiFieldSource { PACKAGE_LABEL, AI_ESTIMATE }

@Serializable
data class AiNutrientValue(
    val value: Double? = null,
    val unit: String,
    val basis: String? = null,
    val source: String,
)

@Serializable
data class AiPhotoResponse(
    @SerialName("schema_version") val schemaVersion: Int,
    @SerialName("source_type") val sourceType: String,
    val name: String,
    @SerialName("generic_name") val genericName: String,
    @SerialName("usda_query") val usdaQuery: String,
    val kind: String,
    @SerialName("amount_value") val amountValue: Double,
    @SerialName("amount_unit") val amountUnit: String,
    @SerialName("meal_type") val mealType: String = "UNKNOWN",
    val confidence: Double,
    val nutrients: Map<String, AiNutrientValue>,
    val warnings: List<String> = emptyList(),
)

@Serializable
private data class PhotoAnalysisRequest(
    @SerialName("schema_version") val schemaVersion: Int = 1,
    @SerialName("mime_type") val mimeType: String,
    @SerialName("image_base64") val imageBase64: String,
)

data class PhotoDraft(
    val name: String,
    val genericName: String,
    val usdaQuery: String,
    val sourceType: PhotoSourceType,
    val kind: EntryKind,
    val mealType: MealType,
    val amount: EntryAmount,
    val confidence: Double,
    val nutrients: Nutrients,
    val warnings: List<String>,
) {
    /** Adds verified USDA values only where the photo draft is missing a field. */
    fun withMissingUsda(food: UsdaFood, grams: Double): PhotoDraft {
        val verified = food.nutrientsForGrams(grams)
        val missingValues = verified.values.filterKeys { it !in nutrients.values }
        val missingProvenance = verified.provenance.filterKeys { it in missingValues }
        return copy(
            nutrients = Nutrients(
                values = nutrients.values + missingValues,
                provenance = nutrients.provenance + missingProvenance,
            ),
        )
    }

    /** Package-label observations are marked verified after review; AI estimates retain their provenance. */
    fun toConfirmedEntry(note: String = ""): JournalEntry {
        val confirmedProvenance = nutrients.provenance.mapValues { (_, source) ->
            if (source.dataSet == DataSet.PACKAGE_LABEL) {
                source.copy(
                    sourceLabel = "Package label reviewed by user",
                    verified = true,
                )
            } else {
                source
            }
        }
        return JournalEntry(
            id = UUID.randomUUID().toString(),
            name = name,
            kind = kind,
            mealType = mealType,
            servingDescription = "${amount.value} ${amount.unit.symbol}",
            servingGrams = amount.value.takeIf { amount.unit == AmountUnit.GRAM },
            amount = amount,
            nutrients = Nutrients(nutrients.values, confirmedProvenance),
            note = note.take(2_000),
        )
    }
}

interface PhotoLoggingProvider {
    suspend fun analyze(
        imageBytes: ByteArray,
        consentGranted: Boolean,
        mimeType: String = "image/jpeg",
    ): PhotoDraft
}

object AiContractParser {
    private val json = Json {
        ignoreUnknownKeys = false
        explicitNulls = true
    }

    fun parse(payload: String): PhotoDraft {
        val response = json.decodeFromString<AiPhotoResponse>(payload)
        require(response.schemaVersion == 1) { "Unsupported AI response schema" }
        require(response.name.isNotBlank() && response.name.length <= 120)
        require(response.genericName.length <= 120 && response.usdaQuery.length <= 160)
        require(response.confidence in 0.0..1.0) { "Confidence outside 0..1" }
        require(response.amountValue.isFinite() && response.amountValue in 0.01..100_000.0)

        val amountUnit = AmountUnit.valueOf(response.amountUnit)
        val values = mutableMapOf<NutrientKey, Double>()
        val provenance = mutableMapOf<NutrientKey, NutrientProvenance>()
        response.nutrients.forEach { (rawKey, item) ->
            val key = NutrientKey.valueOf(rawKey)
            require(item.unit == key.unit) { "Unexpected unit for $rawKey" }
            val value = item.value ?: return@forEach
            require(value.isFinite() && value in 0.0..100_000.0) { "Invalid nutrient value" }
            val fieldSource = AiFieldSource.valueOf(item.source)
            values[key] = value
            provenance[key] = NutrientProvenance(
                dataSet =
                    if (fieldSource == AiFieldSource.PACKAGE_LABEL) DataSet.PACKAGE_LABEL
                    else DataSet.AI_DRAFT,
                sourceLabel =
                    if (fieldSource == AiFieldSource.PACKAGE_LABEL) "AI-extracted visible package label; review required"
                    else "AI photo estimate; review required",
                sourceVersion = "ai-photo-schema-1",
                verified = false,
            )
        }

        return PhotoDraft(
            name = response.name.trim(),
            genericName = response.genericName.trim(),
            usdaQuery = response.usdaQuery.trim(),
            sourceType = PhotoSourceType.valueOf(response.sourceType),
            kind = EntryKind.valueOf(response.kind),
            mealType = runCatching { MealType.valueOf(response.mealType) }.getOrDefault(MealType.UNKNOWN),
            amount = EntryAmount(response.amountValue, amountUnit),
            confidence = response.confidence,
            nutrients = Nutrients(values, provenance),
            warnings = response.warnings.map { it.take(240) }.take(12),
        )
    }
}

class DeterministicFakePhotoProvider : PhotoLoggingProvider {
    override suspend fun analyze(
        imageBytes: ByteArray,
        consentGranted: Boolean,
        mimeType: String,
    ): PhotoDraft {
        require(consentGranted) { "Explicit photo-processing consent is required" }
        require(imageBytes.isNotEmpty()) { "Image is empty" }
        return AiContractParser.parse(DEMO_RESPONSE)
    }

    companion object {
        const val DEMO_RESPONSE =
            """{"schema_version":1,"source_type":"INGREDIENT","name":"Demo meal estimate","generic_name":"mixed meal","usda_query":"mixed dish","kind":"FOOD","amount_value":1.0,"amount_unit":"SERVING","meal_type":"UNKNOWN","confidence":0.61,"nutrients":{"ENERGY":{"value":420.0,"unit":"kcal","basis":"estimated photographed portion","source":"AI_ESTIMATE"},"PROTEIN":{"value":18.0,"unit":"g","basis":"estimated photographed portion","source":"AI_ESTIMATE"},"SODIUM":{"value":null,"unit":"mg","basis":"not visually determinable","source":"AI_ESTIMATE"}},"warnings":["AI estimate only; confirm every field before saving"]}"""
    }
}

class GatewayPhotoProvider(
    private val client: GatewayHttpClient,
) : PhotoLoggingProvider {
    private val json = Json {
        encodeDefaults = true
        explicitNulls = true
    }

    override suspend fun analyze(
        imageBytes: ByteArray,
        consentGranted: Boolean,
        mimeType: String,
    ): PhotoDraft {
        require(consentGranted) { "Explicit photo-processing consent is required" }
        require(imageBytes.isNotEmpty() && imageBytes.size <= MAX_IMAGE_BYTES) {
            "Choose an image no larger than 8 MB"
        }
        require(mimeType in SUPPORTED_MIME_TYPES) { "Unsupported image type" }
        val request = PhotoAnalysisRequest(
            mimeType = mimeType,
            imageBase64 = Base64.getEncoder().encodeToString(imageBytes),
        )
        return AiContractParser.parse(client.postJson("v1/photo/analyze", json.encodeToString(request)))
    }

    companion object {
        private const val MAX_IMAGE_BYTES = 8 * 1024 * 1024
        private val SUPPORTED_MIME_TYPES = setOf(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/heic",
            "image/heif",
        )
    }
}
