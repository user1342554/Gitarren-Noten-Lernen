# Guitar Note Recognition Android App – Design Spec

## Overview

A minimalist Android app that listens to a guitar through the device microphone and displays the detected note in real time, including how sharp or flat the note is (like a tuner).

## Goals

- Accurately detect the fundamental pitch of a single guitar note (monophonic)
- Display note name, octave, frequency, and cents deviation in real time
- Provide clear visual feedback: in tune vs. sharp vs. flat
- Low latency (<100ms perceived delay)
- No external native dependencies – pure Kotlin

## Non-Goals

- Chord recognition
- Polyphonic pitch detection
- Recording or playback
- Custom tuning presets (can be added later)

## Tech Stack

| Component | Choice | Rationale |
|-----------|--------|-----------|
| Language | Kotlin | Modern Android standard |
| UI | Jetpack Compose + Material 3 | Declarative, modern |
| Audio Capture | `AudioRecord` API | Standard Android, sufficient latency for tuner |
| Pitch Detection | YIN algorithm (custom Kotlin) | Proven for monophonic instruments, ~150 lines, no dependency |
| Architecture | MVVM | ViewModel + StateFlow + Compose |
| Min SDK | 26 (Android 8.0) | Covers >95% of devices |
| Build | Gradle (Kotlin DSL) | Standard |

## Architecture

```
┌─────────────────────────────────────┐
│           UI Layer (Compose)        │
│  TunerScreen                        │
│  - Note name (large, centered)      │
│  - Tuner gauge (cents deviation)    │
│  - Frequency display                │
│  - Start/Stop button                │
└──────────────┬──────────────────────┘
               │ observes StateFlow<TunerUiState>
┌──────────────┴──────────────────────┐
│         TunerViewModel              │
│  - Manages TunerUiState             │
│  - Starts/stops AudioProcessor      │
│  - Maps pitch -> note + cents       │
└──────────────┬──────────────────────┘
               │ callback: onPitchDetected(Hz)
┌──────────────┴──────────────────────┐
│        AudioProcessor               │
│  - AudioRecord (44100Hz, mono, 16b) │
│  - Reads PCM in coroutine (IO)      │
│  - Sliding window buffer management │
│  - RMS silence gate                 │
│  - Median-of-3 smoothing            │
│  - Feeds buffer to YIN detector     │
│  - Reports detected frequency       │
└──────────────┬──────────────────────┘
               │ calls
┌──────────────┴──────────────────────┐
│        YinPitchDetector             │
│  - Implements YIN algorithm         │
│  - Input: FloatArray (audio buffer) │
│  - Output: Float (frequency in Hz)  │
│  - Pure function, no state          │
└─────────────────────────────────────┘

Utility:
┌─────────────────────────────────────┐
│        NoteUtils                    │
│  - frequencyToNote(hz) -> Note      │
│  - Note: name, octave, cents, hz    │
│  - A4 = 440 Hz reference            │
└─────────────────────────────────────┘
```

## File Structure

```
app/src/main/java/com/noten/app/
├── MainActivity.kt              # Entry point, permission handling
├── ui/
│   ├── theme/
│   │   ├── Theme.kt             # Dark theme setup
│   │   ├── Color.kt             # Color definitions
│   │   └── Type.kt              # Typography
│   └── TunerScreen.kt           # Main Compose UI
├── viewmodel/
│   └── TunerViewModel.kt        # UI state management
├── audio/
│   ├── AudioProcessor.kt        # AudioRecord capture + coroutine loop
│   └── YinPitchDetector.kt      # YIN algorithm implementation
└── model/
    ├── TunerUiState.kt          # Data class for UI state
    └── NoteUtils.kt             # Frequency-to-note conversion
```

## Audio Pipeline

1. `AudioRecord` captures mono 16-bit PCM at 44100 Hz
2. Buffer size: 4096 samples (~93ms window)
3. Sliding window with 75% overlap: maintain a rolling buffer of 4096 samples, shift out the oldest 1024 and read 1024 new samples per iteration → new result every ~23ms
4. PCM bytes → FloatArray (normalized -1.0 to 1.0)
5. **Silence gate**: compute RMS of buffer; skip pitch detection if RMS < 0.01 (below noise floor)
6. YIN algorithm processes buffer → frequency in Hz (or -1 if no pitch)
7. **Smoothing**: median-of-3 filter on recent frequency results to reduce jitter; note hold timer requires 3 consecutive frames of a new note before switching display
8. Frequency passed to ViewModel via callback
9. ViewModel converts to note name + cents, updates UI state

## YIN Algorithm

The YIN algorithm (de Cheveigne & Kawahara, 2002) detects fundamental frequency through:

1. **Difference function**: For each lag τ, compute the squared difference between signal and shifted signal
2. **Cumulative mean normalized difference**: Normalize to make threshold-based detection reliable
3. **Absolute threshold**: Find first dip below threshold (default 0.2) in the normalized function
4. **Parabolic interpolation**: Refine the lag estimate for sub-sample accuracy
5. **Convert lag to frequency**: `frequency = sampleRate / lag`

Parameters:
- Threshold: 0.2 (standard default for guitar; defined as a constant, may need empirical tuning — lower values like 0.10-0.15 improve accuracy but increase missed detections)
- Buffer: 4096 samples (captures ~7.5 periods of lowest guitar note E2 at 82 Hz)

## Frequency to Note Conversion

```
semitonesFromA4 = 12 * log2(frequency / 440.0)
nearestSemitone = round(semitonesFromA4)
centsDeviation = (semitonesFromA4 - nearestSemitone) * 100

noteNames = ["C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"]
noteIndex = (nearestSemitone + 9 + 1200) % 12   // A is index 9
octave = 4 + floor((nearestSemitone + 9).toDouble() / 12.0).toInt()  // floor division for correct octave below A4
```

- Positive cents = sharp (too high)
- Negative cents = flat (too low)
- ±5 cents = considered "in tune"

## UI Design

**Theme**: Dark, minimal. Black background, white/green/red accents.

**Layout** (single screen, portrait):

```
┌─────────────────────────────┐
│                             │
│    ◄━━━━━━━━|━━━━━━━━►     │  ← Tuner gauge (-50 to +50 cents)
│          -23 cents          │
│                             │
│            E4               │  ← Note name (very large)
│         329.6 Hz            │  ← Frequency (smaller)
│                             │
│         [ STOP ]            │  ← Start/Stop toggle
│                             │
└─────────────────────────────┘
```

**Color coding**:
- Gauge indicator: green when ±5 cents, yellow ±15, red beyond
- Note name text: same color scheme
- Smooth animation on gauge movement

## Permissions

- `android.permission.RECORD_AUDIO` (runtime request required)
- No other permissions needed (foreground-only operation)

## Lifecycle & Threading

- Audio capture runs on `Dispatchers.IO` (dedicated coroutine)
- Coroutine is scoped to `viewModelScope` — survives config changes, stops on ViewModel clear
- When app goes to background: stop `AudioRecord` and release microphone (save battery, avoid conflicts)
- When app returns to foreground: restart capture if it was running before
- All UI state updates dispatched to main thread via `StateFlow`

## Error Handling

- No microphone permission → show explanation + re-request button
- No pitch detected (silence/noise) → show "--" as note, gauge centered
- Device doesn't support required audio config → show error message

## Testing Strategy

- **YinPitchDetector**: Unit tests with known sine wave buffers (generate 440Hz → expect A4, generate 82Hz → expect E2)
- **NoteUtils**: Unit tests for frequency-to-note mapping (edge cases at note boundaries)
- **AudioProcessor**: Integration test (manual, on device)
- **UI**: Manual testing with real guitar
