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
    private var roundNotes: List<QuizNote> = emptyList()
    private var noteStartTimeMs: Long = 0L

    private var consecutiveDetections = 0
    private var lastDetectedName = ""
    private val requiredConsecutive = 3
    private var answerLocked = false

    fun onPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(hasPermission = granted) }
        if (granted) startRound()
    }

    fun startRound() {
        roundNotes = NotePool.generateRound(NotePool.OPEN_STRINGS, 10)
        _uiState.update {
            QuizUiState(
                currentNote = roundNotes[0],
                currentIndex = 0,
                totalNotes = roundNotes.size,
                hasPermission = it.hasPermission
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
                    val timeMs = System.currentTimeMillis() - noteStartTimeMs
                    val correct = note.name == target.name

                    answerLocked = true
                    recordResult(target, note.name, correct, timeMs)
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

    private fun recordResult(target: QuizNote, playedName: String, correct: Boolean, timeMs: Long) {
        val result = QuizNoteResult(target, playedName, correct, timeMs)
        val newResults = _uiState.value.results + result
        val feedback = if (correct) Feedback.Correct else Feedback.Wrong(playedName)

        _uiState.update { it.copy(feedback = feedback, results = newResults) }

        viewModelScope.launch {
            delay(if (correct) 1000L else 1500L)
            advanceToNextNote()
        }
    }

    private fun advanceToNextNote() {
        val nextIndex = _uiState.value.currentIndex + 1
        if (nextIndex >= roundNotes.size) {
            stopListening()
            _uiState.update { it.copy(isFinished = true, feedback = null) }
        } else {
            _uiState.update {
                it.copy(
                    currentNote = roundNotes[nextIndex],
                    currentIndex = nextIndex,
                    feedback = null,
                    detectedNoteName = "--"
                )
            }
            noteStartTimeMs = System.currentTimeMillis()
            answerLocked = false
            consecutiveDetections = 0
            lastDetectedName = ""
        }
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
