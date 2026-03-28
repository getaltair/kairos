package com.getaltair.kairos.domain.wear

/**
 * Lightweight habit data transferred to the watch via the Data Layer.
 * Contains only the fields needed for watch-side display and interaction.
 *
 * This is a pure Kotlin data class with manual JSON serialization,
 * keeping the domain module free of Android or serialization framework dependencies.
 */
data class WearHabitData(
    val id: String,
    val name: String,
    val anchorBehavior: String,
    val category: String,
    val estimatedSeconds: Int,
    val icon: String?,
    val color: String?,
) {
    fun toJson(): String = buildString {
        append("{")
        append("\"id\":\"${escapeJson(id)}\",")
        append("\"name\":\"${escapeJson(name)}\",")
        append("\"anchorBehavior\":\"${escapeJson(anchorBehavior)}\",")
        append("\"category\":\"${escapeJson(category)}\",")
        append("\"estimatedSeconds\":$estimatedSeconds,")
        append("\"icon\":${if (icon != null) "\"${escapeJson(icon)}\"" else "null"},")
        append("\"color\":${if (color != null) "\"${escapeJson(color)}\"" else "null"}")
        append("}")
    }

    companion object {
        fun fromJson(json: String): WearHabitData? {
            fun extract(key: String): String? {
                val pattern = "\"$key\":\"([^\"]*)\""
                val match = Regex(pattern).find(json)
                return match?.groupValues?.get(1)
            }

            fun extractInt(key: String): Int {
                val pattern = "\"$key\":(\\d+)"
                return Regex(pattern).find(json)?.groupValues?.get(1)?.toInt() ?: 0
            }

            val id = extract("id")
            val name = extract("name")
            if (id.isNullOrBlank() || name.isNullOrBlank()) return null

            return WearHabitData(
                id = id,
                name = name,
                anchorBehavior = extract("anchorBehavior") ?: "",
                category = extract("category") ?: "",
                estimatedSeconds = extractInt("estimatedSeconds"),
                icon = extract("icon"),
                color = extract("color"),
            )
        }

        fun listFromJson(json: String): List<WearHabitData> {
            if (json.isBlank() || json == "[]") return emptyList()
            val trimmed = json.trim().removePrefix("[").removeSuffix("]").trim()
            if (trimmed.isEmpty()) return emptyList()
            return splitJsonObjects(trimmed).mapNotNull { fromJson(it) }
        }

        fun listToJson(list: List<WearHabitData>): String = "[${list.joinToString(",") { it.toJson() }}]"
    }
}
