package com.noten.app.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noten.app.audio.AudioProcessor
import com.noten.app.model.NoteUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class QuizViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private var audioJob: Job? = null
    private var audioProcessor: AudioProcessor? = null
    private var noteStartTimeMs: Long = 0L
    private var pool: List<QuizNote> = emptyList()

    private var consecutiveDetections = 0
    private var lastDetectedName = ""
    private val requiredConsecutive = 3
    private var answerLocked = false

    fun setDifficulty(difficulty: Difficulty) {
        _uiState.update { it.copy(difficulty = difficulty) }
    }

    fun onPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(hasPermission = granted) }
        if (granted) startSession()
    }

    fun startSession() {
        pool = NotePool.poolForDifficulty(_uiState.value.difficulty)
        val firstNote = NotePool.randomNote(pool)
        _uiState.update {
            it.copy(
                currentNote = firstNote,
                score = 0,
                attempts = 0,
                streak = 0,
                bestStreak = 0,
                feedback = null,
                detectedNoteName = "--"
            )
        }
        noteStartTimeMs = System.currentTimeMillis()
        answerLocked = false
        consecutiveDetections = 0
        lastDetectedName = ""
        startListening()
    }

    private fun startListening() {
        val processor = AudioProcessor(
            onPitchDetected = { frequency ->
                val note = NoteUtils.frequencyToNote(frequency.toDouble())
                _uiState.update { it.copy(detectedNoteName = note.name) }

                if (answerLocked) return@AudioProcessor

                if (note.name == lastDetectedName) {
                    consecutiveDetections++
                } else {
                    lastDetectedName = note.name
                    consecutiveDetections = 1
                }

                if (consecutiveDetections >= requiredConsecutive) {
                    val target = _uiState.value.currentNote ?: return@AudioProcessor
                    val correct = note.name == target.name

                    answerLocked = true

                    if (correct) {
                        val newStreak = _uiState.value.streak + 1
                        _uiState.update {
                            it.copy(
                                feedback = Feedback.Correct,
                                score = it.score + 1,
                                attempts = it.attempts + 1,
                                streak = newStreak,
                                bestStreak = maxOf(it.bestStreak, newStreak)
                            )
                        }
                        // Correct: brief green flash, then next note
                        viewModelScope.launch {
                            delay(800L)
                            nextNote()
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                feedback = Feedback.Wrong(note.name),
                                attempts = it.attempts + 1,
                                streak = 0
                            )
                        }
                        // Wrong: show what was played, then let them retry the SAME note
                        viewModelScope.launch {
                            delay(1200L)
                            retryCurrentNote()
                        }
                    }
                }
            },
            onSilence = {
                _uiState.update { it.copy(detectedNoteName = "--") }
            }
        )
        audioProcessor = processor
        audioJob = viewModelScope.launch {
            _uiState.update { it.copy(isListening = true) }
            processor.start()
        }
    }

    private fun nextNote() {
        val newNote = NotePool.randomNote(pool)
        _uiState.update {
            it.copy(
                currentNote = newNote,
                feedback = null,
                detectedNoteName = "--"
            )
        }
        noteStartTimeMs = System.currentTimeMillis()
        resetDetection()
    }

    private fun retryCurrentNote() {
        // Keep the same note, just clear feedback so they can try again
        _uiState.update {
            it.copy(
                feedback = null,
                detectedNoteName = "--"
            )
        }
        resetDetection()
    }

    private fun resetDetection() {
        answerLocked = false
        consecutiveDetections = 0
        lastDetectedName = ""
    }

    fun stopAndGetResults(): Pair<Int, Int> {
        stopListening()
        val state = _uiState.value
        return Pair(state.score, state.attempts)
    }

    private fun stopListening() {
        audioJob?.cancel()
        audioProcessor?.stop()
        audioProcessor = null
        _uiState.update { it.copy(isListening = false) }
    }

    fun onPause() {
        stopListening()
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }
}
