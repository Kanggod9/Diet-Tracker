package io.github.kanggod9.diettracker.data

import io.github.kanggod9.diettracker.domain.DataSet
import io.github.kanggod9.diettracker.domain.NutrientKey
import io.github.kanggod9.diettracker.domain.NutrientProvenance
import io.github.kanggod9.diettracker.domain.Nutrients
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64

/**
 * Stable, line-oriented local format. A missing key stays absent and an explicit zero round-trips as zero.
 * P2 added URL/version metadata while the decoder continues to accept the original P record.
 */
object NutrientCodec {
    fun encode(nutrients: Nutrients): String = buildString {
        nutrients.values.toSortedMap(compareBy { it.name }).forEach { (key, value) ->
            append("V|").append(key.name).append('|').append(value).append(10.toChar())
        }
        nutrients.provenance.toSortedMap(compareBy { it.name }).forEach { (key, source) ->
            append("P2|").append(key.name).append('|').append(source.dataSet.name).append('|')
                .append(source.verified).append('|').append(b64(source.sourceId.orEmpty())).append('|')
                .append(b64(source.sourceLabel)).append('|').append(b64(source.sourceUrl.orEmpty())).append('|')
                .append(b64(source.sourceVersion.orEmpty())).append('|')
                .append(source.retrievedAt.orEmpty())
                .append(10.toChar())
        }
    }

    fun decode(encoded: String): Nutrients {
        val values = mutableMapOf<NutrientKey, Double>()
        val sources = mutableMapOf<NutrientKey, NutrientProvenance>()
        encoded.lineSequence().filter { it.isNotBlank() }.forEach { line ->
            val parts = line.split('|')
            runCatching {
                when (parts.firstOrNull()) {
                    "V" -> if (parts.size == 3) {
                        val value = parts[2].toDouble()
                        if (value.isFinite() && value >= 0.0) {
                            values[NutrientKey.valueOf(parts[1])] = value
                        }
                    }
                    "P" -> if (parts.size == 7) {
                        sources[NutrientKey.valueOf(parts[1])] = NutrientProvenance(
                            dataSet = DataSet.valueOf(parts[2]),
                            verified = parts[3].toBoolean(),
                            sourceId = unb64(parts[4]).ifBlank { null },
                            sourceLabel = unb64(parts[5]),
                            retrievedAt = parts[6].toLongOrNull()?.let(Instant::ofEpochMilli),
                        )
                    }
                    "P2" -> if (parts.size == 9) {
                        sources[NutrientKey.valueOf(parts[1])] = NutrientProvenance(
                            dataSet = DataSet.valueOf(parts[2]),
                            verified = parts[3].toBoolean(),
                            sourceId = unb64(parts[4]).ifBlank { null },
                            sourceLabel = unb64(parts[5]),
                            sourceUrl = unb64(parts[6]).ifBlank { null },
                            sourceVersion = unb64(parts[7]).ifBlank { null },
                            retrievedAt = parts[8].toLongOrNull()?.let(Instant::ofEpochMilli),
                        )
                    }
                }
            }
        }
        return Nutrients(values, sources.filterKeys { it in values })
    }

    private fun b64(value: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun unb64(value: String): String =
        String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)

    private fun Instant?.orEmpty(): String = this?.toEpochMilli()?.toString().orEmpty()
}
