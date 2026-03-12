package com.noten.app.model

import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.roundToInt

object NoteUtils {

    private const val A4_FREQUENCY = 440.0
    private val NOTE_NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    fun frequencyToNote(frequency: Double): NoteInfo {
        val semitonesFromA4 = 12.0 * log2(frequency / A4_FREQUENCY)
        val nearestSemitone = semitonesFromA4.roundToInt()
        val cents = (semitonesFromA4 - nearestSemitone) * 100.0

        val noteIndex = ((nearestSemitone % 12) + 9 + 12) % 12
        val octave = 4 + floor((nearestSemitone + 9).toDouble() / 12.0).toInt()

        return NoteInfo(
            name = NOTE_NAMES[noteIndex],
            octave = octave,
            cents = cents,
            frequency = frequency
        )
    }
}
