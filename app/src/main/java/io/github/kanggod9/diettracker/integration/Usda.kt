package io.github.kanggod9.diettracker.integration

import io.github.kanggod9.diettracker.domain.DataSet
import io.github.kanggod9.diettracker.domain.NutrientKey
import io.github.kanggod9.diettracker.domain.NutrientProvenance
import io.github.kanggod9.diettracker.domain.Nutrients
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Duration
import java.time.Instant

enum class UsdaDataType(val wireValue: String, val dataSet: DataSet) {
    FOUNDATION("Foundation", DataSet.USDA_FOUNDATION),
    SR_LEGACY("SR Legacy", DataSet.USDA_SR_LEGACY),
}

data class UsdaFood(
    val fdcId: Long,
    val description: String,
    val dataType: UsdaDataType,
    val nutrientsPer100g: Nutrients,
) {
    init {
        require(fdcId > 0)
        require(description.isNotBlank() && description.length <= 240)
    }

    fun nutrientsForGrams(grams: Double): Nutrients {
        require(grams.isFinite() && grams > 0.0 && grams <= 100_000.0)
        return nutrientsPer100g.scaled(grams / 100.0)
    }
}

data class UsdaCacheEntry(
    val food: UsdaFood,
    val fetchedAt: Instant,
    val expiresAt: Instant,
)

interface UsdaFoodDataSource {
    suspend fun search(
        query: String,
        allowedTypes: Set<UsdaDataType> = UsdaDataType.entries.toSet(),
    ): List<UsdaFood>

    suspend fun food(fdcId: Long): UsdaFood?
}

interface UsdaFoodCache {
    fun get(fdcId: Long, now: Instant): UsdaCacheEntry?
    fun put(entry: UsdaCacheEntry)
    fun clear()
}

data class UsdaSearchContract(
    val query: String,
    val dataTypes: Set<UsdaDataType> = UsdaDataType.entries.toSet(),
) {
    init {
        require(query.trim().length in 2..120)
        require(dataTypes.isNotEmpty() && dataTypes.all { it in UsdaDataType.entries })
    }

    val dataTypeParameter: String get() = dataTypes.joinToString(",") { it.wireValue }
}

@Serializable
private data class UsdaSearchRequest(
    @SerialName("schema_version") val schemaVersion: Int = 1,
    val query: String,
    @SerialName("data_types") val dataTypes: List<String>,
)

@Serializable
private data class UsdaFoodRequest(
    @SerialName("schema_version") val schemaVersion: Int = 1,
    @SerialName("fdc_id") val fdcId: Long,
)

@Serializable
private data class GatewayUsdaFood(
    @SerialName("fdc_id") val fdcId: Long,
    val description: String,
    @SerialName("data_type") val dataType: String,
    val nutrients: Map<String, Double?>,
)

@Serializable
private data class UsdaSearchResponse(
    @SerialName("schema_version") val schemaVersion: Int,
    val foods: List<GatewayUsdaFood>,
)

@Serializable
private data class UsdaFoodResponse(
    @SerialName("schema_version") val schemaVersion: Int,
    val food: GatewayUsdaFood? = null,
)

class GatewayUsdaDataSource(
    private val client: GatewayHttpClient,
    private val clock: () -> Instant = Instant::now,
) : UsdaFoodDataSource {
    private val json = Json {
        ignoreUnknownKeys = false
        explicitNulls = true
        encodeDefaults = true
    }

    override suspend fun search(
        query: String,
        allowedTypes: Set<UsdaDataType>,
    ): List<UsdaFood> {
        val contract = UsdaSearchContract(query.trim(), allowedTypes)
        val request = UsdaSearchRequest(
            query = contract.query,
            dataTypes = allowedTypes.sortedBy { it.ordinal }.map { it.wireValue },
        )
        val response = json.decodeFromString<UsdaSearchResponse>(
            client.postJson("v1/usda/search", json.encodeToString(request)),
        )
        require(response.schemaVersion == 1)
        return response.foods.take(MAX_RESULTS).map { it.toDomain(allowedTypes, clock()) }
    }

    override suspend fun food(fdcId: Long): UsdaFood? {
        require(fdcId > 0)
        val response = json.decodeFromString<UsdaFoodResponse>(
            client.postJson("v1/usda/food", json.encodeToString(UsdaFoodRequest(fdcId = fdcId))),
        )
        require(response.schemaVersion == 1)
        return response.food?.toDomain(UsdaDataType.entries.toSet(), clock())
    }

    private fun GatewayUsdaFood.toDomain(
        allowedTypes: Set<UsdaDataType>,
        retrievedAt: Instant,
    ): UsdaFood {
        val type = UsdaDataType.entries.firstOrNull { it.wireValue == dataType }
            ?: error("Unsupported USDA data type")
        require(type in allowedTypes) { "Gateway returned a disallowed USDA data type" }
        require(fdcId > 0 && description.isNotBlank())

        val values = nutrients.mapNotNull { (rawKey, rawValue) ->
            val value = rawValue ?: return@mapNotNull null
            val key = NutrientKey.valueOf(rawKey)
            require(value.isFinite() && value in 0.0..100_000.0)
            key to value
        }.toMap()
        require(values[NutrientKey.ENERGY]?.let { it <= 1_000.0 } != false)
        val sourceUrl = "https://fdc.nal.usda.gov/fdc-app.html#/food-details/$fdcId/nutrients"
        val provenance = values.keys.associateWith {
            NutrientProvenance(
                dataSet = type.dataSet,
                sourceId = fdcId.toString(),
                sourceLabel = "USDA FoodData Central · $description · per 100 g",
                sourceUrl = sourceUrl,
                sourceVersion = type.wireValue,
                retrievedAt = retrievedAt,
                verified = true,
            )
        }
        return UsdaFood(
            fdcId = fdcId,
            description = description.take(240),
            dataType = type,
            nutrientsPer100g = Nutrients(values, provenance),
        )
    }

    companion object {
        private const val MAX_RESULTS = 25
    }
}

class CachingUsdaDataSource(
    private val upstream: UsdaFoodDataSource,
    private val cache: UsdaFoodCache,
    private val clock: () -> Instant = Instant::now,
    private val timeToLive: Duration = Duration.ofDays(30),
) : UsdaFoodDataSource {
    override suspend fun search(
        query: String,
        allowedTypes: Set<UsdaDataType>,
    ): List<UsdaFood> {
        val results = upstream.search(query, allowedTypes)
        val now = clock()
        results.forEach { food ->
            cache.put(UsdaCacheEntry(food, now, now.plus(timeToLive)))
        }
        return results
    }

    override suspend fun food(fdcId: Long): UsdaFood? {
        val now = clock()
        cache.get(fdcId, now)?.let { return it.food }
        return upstream.food(fdcId)?.also { food ->
            cache.put(UsdaCacheEntry(food, now, now.plus(timeToLive)))
        }
    }
}

class InMemoryUsdaFoodCache : UsdaFoodCache {
    private val entries = mutableMapOf<Long, UsdaCacheEntry>()

    override fun get(fdcId: Long, now: Instant): UsdaCacheEntry? =
        entries[fdcId]?.takeIf { now.isBefore(it.expiresAt) } ?: entries.remove(fdcId).let { null }

    override fun put(entry: UsdaCacheEntry) {
        entries[entry.food.fdcId] = entry
    }

    override fun clear() {
        entries.clear()
    }
}

class DemoUsdaDataSource(
    private val samples: List<UsdaFood>,
) : UsdaFoodDataSource {
    override suspend fun search(query: String, allowedTypes: Set<UsdaDataType>): List<UsdaFood> =
        samples.filter {
            it.dataType in allowedTypes && it.description.contains(query, ignoreCase = true)
        }

    override suspend fun food(fdcId: Long): UsdaFood? = samples.firstOrNull { it.fdcId == fdcId }
}
