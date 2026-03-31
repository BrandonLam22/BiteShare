package org.example.biteshare.view

fun etaLabel(rawEta: String): String? {
    val trimmed = rawEta.trim()
    if (trimmed.isBlank()) return null
    if (trimmed.equals("ETA unavailable", ignoreCase = true)) return null
    return trimmed
}

fun joinInfo(
    separator: String = " • ",
    vararg parts: String?,
): String =
    parts.mapNotNull { part ->
        part?.trim()?.takeIf { it.isNotBlank() }
    }.joinToString(separator)
