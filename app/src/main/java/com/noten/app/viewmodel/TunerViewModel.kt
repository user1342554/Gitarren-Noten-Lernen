package com.noten.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noten.app.audio.AudioProcessor
import com.noten.app.model.GuitarTuning
import com.noten.app.model.GuitarTunings
import com.noten.app.model.NoteUtils
import com.noten.app.model.TunerUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TunerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TunerUiState())
    val uiState: StateFlow<TunerUiState> = _uiState.asStateFlow()

    private var audioJob: Job? = null
    private var audioProcessor: AudioProcessor? = null

    private var lastNoteName: String = ""
    private var noteHoldCount: Int = 0
    private val noteHoldThreshold = 3

    fun setTuning(tuning: GuitarTuning) {
        _uiState.update { it.copy(tuning = tuning, closestString = null, centsFromTarget = 0.0) }
    }

    fun onPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(hasPermission = granted) }
        if (granted && !_uiState.value.isListening) {
            startListening()
        }
    }

    fun toggleListening() {
        if (_uiState.value.isListening) {
            stopListening()
        } else if (_uiState.value.hasPermission) {
            startListening()
        }
    }

    fun onPause() {
        if (_uiState.value.isListening) {
            stopListening()
            _uiState.update { it.copy(isListening = false) }
        }
    }

    private fun startListening() {
        val processor = AudioProcessor(
            onPitchDetected = { frequency ->
                val note = NoteUtils.frequencyToNote(frequency.toDouble())

                if (note.name != lastNoteName) {
                    noteHoldCount++
                    if (noteHoldCount < noteHoldThreshold) return@AudioProcessor
                    lastNoteName = note.name
                    noteHoldCount = 0
                } else {
                    noteHoldCount = 0
                }

                val tuning = _uiState.value.tuning
                val closest = tuning.closestString(frequency.toDouble())
                val centsFromTarget = if (closest != null) {
                    GuitarTuning.frequencyToCents(frequency.toDouble(), closest.frequency)
                } else 0.0

                _uiState.update {
                    it.copy(
                        noteName = note.name,
                        octave = note.octave,
                        cents = note.cents,
                        frequency = note.frequency,
                        closestString = closest,
                        centsFromTarget = centsFromTarget
                    )
                }
            },
            onSilence = {
                _uiState.update {
                    it.copy(
                        noteName = "--",
                        octave = 0,
                        cents = 0.0,
                        frequency = 0.0,
                        closestString = null,
                        centsFromTarget = 0.0
                    )
                }
            }
        )
        audioProcessor = processor

        audioJob = viewModelScope.launch {
            _uiState.update { it.copy(isListening = true) }
            processor.start()
        }
    }

    private fun stopListening() {
        audioJob?.cancel()
        audioProcessor?.stop()
        audioProcessor = null
        _uiState.update { it.copy(isListening = false) }
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }
}
