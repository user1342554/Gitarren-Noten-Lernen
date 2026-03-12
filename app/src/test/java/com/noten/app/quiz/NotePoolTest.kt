package com.noten.app.quiz

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class NotePoolTest {

    @Test
    fun `open strings pool has 6 notes`() {
        assertEquals(6, NotePool.OPEN_STRINGS.size)
    }

    @Test
    fun `open strings contains all guitar open strings`() {
        val names = NotePool.OPEN_STRINGS.map { it.name }
        assertTrue(names.containsAll(listOf("E", "A", "D", "G", "B")))
    }

    @Test
    fun `generateRound returns requested number of notes`() {
        val round = NotePool.generateRound(NotePool.OPEN_STRINGS, count = 10)
        assertEquals(10, round.size)
    }

    @Test
    fun `generateRound only contains notes from pool`() {
        val pool = NotePool.OPEN_STRINGS
        val round = NotePool.generateRound(pool, count = 10)
        for (note in round) {
            assertTrue(pool.contains(note), "Note $note not in pool")
        }
    }

    @Test
    fun `staff position is set for all open strings`() {
        for (note in NotePool.OPEN_STRINGS) {
            assertNotNull(note.staffPosition, "Staff position missing for ${note.name}${note.octave}")
        }
    }
}
