package io.github.kanggod9.diettracker.domain

object NutrientTargets {
    private const val PREFIX = "target."

    fun settingKey(key: NutrientKey): String = PREFIX + key.name.lowercase()

    fun defaults(region: GuidanceRegion): Map<NutrientKey, Double> =
        GuidanceProfiles.all.first { it.region == region }.targets.associate { it.key to it.amount }

    fun resolved(region: GuidanceRegion, settings: Map<String, String>): Map<NutrientKey, Double> {
        val defaults = defaults(region)
        return NutrientKey.entries.mapNotNull { key ->
            val custom = settings[settingKey(key)]?.toDoubleOrNull()
                ?.takeIf { it.isFinite() && it > 0.0 && it <= 100_000.0 }
            (custom ?: defaults[key])?.let { key to it }
        }.toMap()
    }
}