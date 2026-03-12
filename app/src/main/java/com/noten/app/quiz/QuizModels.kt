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

data class QuizUiState(
    val currentNote: QuizNote? = null,
    val currentIndex: Int = 0,
    val totalNotes: Int = 10,
    val detectedNoteName: String = "--",
    val feedback: Feedback? = null,
    val isListening: Boolean = false,
    val isFinished: Boolean = false,
    val results: List<QuizNoteResult> = emptyList(),
    val hasPermission: Boolean = false
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

    fun generateRound(pool: List<QuizNote>, count: Int = 10): List<QuizNote> {
        return List(count) { pool.random() }
    }
}
