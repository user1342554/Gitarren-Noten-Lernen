package com.noten.app.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.math.abs

class NoteUtilsTest {

    @Test
    fun `A4 at 440 Hz returns A4 with 0 cents`() {
        val note = NoteUtils.frequencyToNote(440.0)
        assertEquals("A", note.name)
        assertEquals(4, note.octave)
        assertEquals(0.0, note.cents, 0.1)
    }

    @Test
    fun `E2 at 82_41 Hz returns E2`() {
        val note = NoteUtils.frequencyToNote(82.41)
        assertEquals("E", note.name)
        assertEquals(2, note.octave)
        assert(abs(note.cents) < 1.0)
    }

    @Test
    fun `E4 at 329_63 Hz returns E4`() {
        val note = NoteUtils.frequencyToNote(329.63)
        assertEquals("E", note.name)
        assertEquals(4, note.octave)
        assert(abs(note.cents) < 1.0)
    }

    @Test
    fun `C4 middle C at 261_63 Hz returns C4`() {
        val note = NoteUtils.frequencyToNote(261.63)
        assertEquals("C", note.name)
        assertEquals(4, note.octave)
        assert(abs(note.cents) < 1.0)
    }

    @Test
    fun `B3 at 246_94 Hz returns B3`() {
        val note = NoteUtils.frequencyToNote(246.94)
        assertEquals("B", note.name)
        assertEquals(3, note.octave)
        assert(abs(note.cents) < 1.0)
    }

    @Test
    fun `sharp note returns positive cents`() {
        val note = NoteUtils.frequencyToNote(445.0)
        assertEquals("A", note.name)
        assertEquals(4, note.octave)
        assert(note.cents > 0) { "Expected positive cents for sharp note, got ${note.cents}" }
    }

    @Test
    fun `flat note returns negative cents`() {
        val note = NoteUtils.frequencyToNote(435.0)
        assertEquals("A", note.name)
        assertEquals(4, note.octave)
        assert(note.cents < 0) { "Expected negative cents for flat note, got ${note.cents}" }
    }

    @Test
    fun `all guitar open strings detected correctly`() {
        val strings = listOf(
            82.41 to ("E" to 2),
            110.0 to ("A" to 2),
            146.83 to ("D" to 3),
            196.0 to ("G" to 3),
            246.94 to ("B" to 3),
            329.63 to ("E" to 4),
        )
        for ((freq, expected) in strings) {
            val note = NoteUtils.frequencyToNote(freq)
            assertEquals(expected.first, note.name, "Wrong name for $freq Hz")
            assertEquals(expected.second, note.octave, "Wrong octave for $freq Hz")
        }
    }
}
