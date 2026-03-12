package com.noten.app.model

data class NoteInfo(
    val name: String,
    val octave: Int,
    val cents: Double,
    val frequency: Double
)
