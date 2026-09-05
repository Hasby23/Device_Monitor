package com.example.devicemonitor

data class SurfaceFlingerLayerStats(
    val layerName: String,
    val totalFrames: Long,
    val droppedFrames: Long = 0,
    val jankyFrames: Long = 0,
    val averageFPS: Double = 0.0
)

object SurfaceFlingerParser {

    fun parseTotalFrames(rawOutput: String, targetLayerQuery: String): Double? {
        return parseLayerStats(rawOutput, targetLayerQuery)?.averageFPS
    }

    fun parseLayerStats(rawOutput: String, targetLayerQuery: String): SurfaceFlingerLayerStats? {
        return parseAllLayers(rawOutput).firstOrNull {
            it.layerName.contains(targetLayerQuery, ignoreCase = true)
        }
    }

    fun parseAllLayers(rawOutput: String): List<SurfaceFlingerLayerStats> {
        val results = mutableListOf<SurfaceFlingerLayerStats>()
        var currentLayerName: String? = null
        var currentTotalFrames: Long? = null
        var currentDroppedFrames: Long = 0
        var currentJankyFrames: Long = 0
        var currentAverageFPS = 0.0

        for (line in rawOutput.lineSequence()) {
            val trimmed = line.trim()

            if (trimmed.startsWith("displayRefreshRate =") || trimmed.startsWith("layerName =")) {
                val layer = currentLayerName
                val frames = currentTotalFrames
                if (!layer.isNullOrBlank() && layer != "none" && frames != null) {
                    results.add(
                        SurfaceFlingerLayerStats(
                            layerName = layer,
                            totalFrames = frames,
                            droppedFrames = currentDroppedFrames,
                            jankyFrames = currentJankyFrames,
                            averageFPS = currentAverageFPS
                        )
                    )
                }

                if (trimmed.startsWith("layerName =")) {
                    currentLayerName = trimmed.substringAfter("=").trim()
                } else {
                    currentLayerName = null
                }
                currentTotalFrames = null
                currentDroppedFrames = 0
                currentJankyFrames = 0
                currentAverageFPS = 0.0
            } else if (trimmed.startsWith("layerName =")) {
                currentLayerName = trimmed.substringAfter("=").trim()
            } else if (trimmed.startsWith("totalFrames =")) {
                currentTotalFrames = trimmed.substringAfter("=").trim().toLongOrNull()
            } else if (trimmed.startsWith("droppedFrames =")) {
                currentDroppedFrames = trimmed.substringAfter("=").trim().toLongOrNull() ?: 0
            } else if (trimmed.startsWith("jankyFrames =")) {
                currentJankyFrames = trimmed.substringAfter("=").trim().toLongOrNull() ?: 0
            } else if (trimmed.startsWith("averageFPS =")) {
                currentAverageFPS = trimmed.substringAfter("=").trim().toDoubleOrNull() ?: 0.0
            }
        }

        val layer = currentLayerName
        val frames = currentTotalFrames
        if (!layer.isNullOrBlank() && layer != "none" && frames != null) {
            results.add(
                SurfaceFlingerLayerStats(
                    layerName = layer,
                    totalFrames = frames,
                    droppedFrames = currentDroppedFrames,
                    jankyFrames = currentJankyFrames,
                    averageFPS = currentAverageFPS
                )
            )
        }

        return results
    }
}
