package com.noten.app.model

import kotlin.math.log2
import kotlin.math.round

object NoteUtils {

    private val NOTE_NAMES = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    private const val A4_FREQUENCY = 440.0

    fun frequencyToNote(frequency: Double): NoteInfo {
        val semitonesFromA4 = 12.0 * log2(frequency / A4_FREQUENCY)
        val nearestSemitone = round(semitonesFromA4).toInt()
        val cents = (semitonesFromA4 - nearestSemitone) * 100.0

        val midiNote = 69 + nearestSemitone
        val noteIndex = ((midiNote % 12) + 12) % 12
        val octave = (midiNote / 12) - 1

        return NoteInfo(
            name = NOTE_NAMES[noteIndex],
            octave = octave,
            cents = cents,
            frequency = frequency
        )
    }
}
