package com.noten.app.model

data class TunerUiState(
    val noteName: String = "--",
    val octave: Int = 0,
    val cents: Double = 0.0,
    val frequency: Double = 0.0,
    val isListening: Boolean = false,
    val hasPermission: Boolean = false
)
