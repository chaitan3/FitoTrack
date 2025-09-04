package de.tadris.fitness.recording.indoor.exercise

data class RepetitionRecognizedEvent(
    val type: String,
    val timestamp: Long,
    val intensity: Double = 0.0
)