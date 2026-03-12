package com.noten.app.quiz

data class QuizNote(
    val name: String,
    val octave: Int,
    val staffPosition: Int
)

data class QuizNoteResult(
    val target: QuizNote,
    val played: String?,
    val correct: Boolean,
    val timeMs: Long
)

sealed class Feedback {
    object Correct : Feedback()
    data class Wrong(val playedNote: String) : Feedback()
}

enum class Difficulty(val label: String) {
    OPEN_STRINGS("Leere Saiten"),
    FIRST_POSITION("Erste Lage"),
    ALL_NOTES("Alle Noten")
}

data class QuizUiState(
    val currentNote: QuizNote? = null,
    val detectedNoteName: String = "--",
    val feedback: Feedback? = null,
    val isListening: Boolean = false,
    val hasPermission: Boolean = false,
    val score: Int = 0,
    val attempts: Int = 0,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val difficulty: Difficulty = Difficulty.OPEN_STRINGS
)

object NotePool {
    val OPEN_STRINGS = listOf(
        QuizNote("E", 2, staffPosition = -8),
        QuizNote("A", 2, staffPosition = -4),
        QuizNote("D", 3, staffPosition = -1),
        QuizNote("G", 3, staffPosition = 2),
        QuizNote("B", 3, staffPosition = 4),
        QuizNote("E", 4, staffPosition = 7),
    )

    val FIRST_POSITION = listOf(
        // E string: E2, F2, G2
        QuizNote("E", 2, staffPosition = -8),
        QuizNote("F", 2, staffPosition = -7),
        QuizNote("G", 2, staffPosition = -6),
        // A string: A2, B2, C3
        QuizNote("A", 2, staffPosition = -4),
        QuizNote("B", 2, staffPosition = -3),
        QuizNote("C", 3, staffPosition = -2),
        // D string: D3, E3, F3
        QuizNote("D", 3, staffPosition = -1),
        QuizNote("E", 3, staffPosition = 0),
        QuizNote("F", 3, staffPosition = 1),
        // G string: G3, A3
        QuizNote("G", 3, staffPosition = 2),
        QuizNote("A", 3, staffPosition = 3),
        // B string: B3, C4, D4
        QuizNote("B", 3, staffPosition = 4),
        QuizNote("C", 4, staffPosition = 5),
        QuizNote("D", 4, staffPosition = 6),
        // High E string: E4, F4, G4
        QuizNote("E", 4, staffPosition = 7),
        QuizNote("F", 4, staffPosition = 8),
        QuizNote("G", 4, staffPosition = 9),
    )

    val ALL_NOTES = FIRST_POSITION + listOf(
        // Sharps/flats from first position
        QuizNote("F#", 2, staffPosition = -7), // between F and G
        QuizNote("G#", 2, staffPosition = -6),
        QuizNote("C#", 3, staffPosition = -2),
        QuizNote("F#", 3, staffPosition = 1),
        QuizNote("G#", 3, staffPosition = 2),
        QuizNote("C#", 4, staffPosition = 5),
        QuizNote("D#", 4, staffPosition = 6),
        QuizNote("F#", 4, staffPosition = 8),
    )

    fun poolForDifficulty(difficulty: Difficulty): List<QuizNote> = when (difficulty) {
        Difficulty.OPEN_STRINGS -> OPEN_STRINGS
        Difficulty.FIRST_POSITION -> FIRST_POSITION
        Difficulty.ALL_NOTES -> ALL_NOTES
    }

    fun randomNote(pool: List<QuizNote>): QuizNote = pool.random()

    fun generateRound(pool: List<QuizNote>, count: Int = 10): List<QuizNote> {
        return List(count) { pool.random() }
    }
}
