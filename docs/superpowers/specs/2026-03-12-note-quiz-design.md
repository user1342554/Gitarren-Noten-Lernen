# Note Quiz Feature – Design Spec

## Overview

Add a Duolingo-style note quiz to the existing guitar tuner app. The app displays a musical note on a staff, the user plays it on their guitar, and gets immediate right/wrong feedback. A round consists of 10 notes with a score screen at the end.

## Goals

- Teach users to read sheet music by associating staff notation with guitar notes
- Provide immediate audio-based feedback (reuse existing pitch detection)
- Simple round-based gameplay: 10 notes per round, score at the end
- Progressive difficulty: start with open strings, then add fretted notes

## Non-Goals

- Persistent progress/scores (future)
- XP, streaks, gamification beyond the round score (future)
- Multiple game modes (time-based, endless, etc.) (future)
- Custom note selection by user (future)

## New Screens

### 1. HomeScreen

Entry point of the app. Two options:
- **Noten Quiz** – starts a quiz round
- **Stimmgerät** – opens the existing tuner

Minimal layout: app title + two large buttons.

### 2. QuizScreen

**Layout (top to bottom):**
- Progress bar with "Runde 1" and "3/10" counter
- Staff notation area: treble clef + 5 lines + note rendered at correct vertical position
- Listening indicator (pulsing green dot + "Lausche...")
- Detected note display (large text showing what the mic heard, or "--" if silent)
- Feedback flash: green checkmark or red X after each attempt

**Behavior:**
1. On screen load, pick next note from the round's note list
2. Render note on staff
3. Start listening via AudioProcessor (same as tuner)
4. When a stable pitch is detected (held for ~0.5 seconds), compare to target note
5. If note name matches (octave-insensitive for now): correct. Show green flash, increment score, advance after 1s
6. If note name doesn't match: wrong. Show red flash with what was played, advance after 1.5s
7. After 10 notes: navigate to ResultScreen

**Octave handling:** For simplicity, only check the note name (e.g., "E" matches "E" regardless of octave). This avoids confusion for beginners who might play the right note in a different octave.

**Timeout:** If no note is detected within 10 seconds, show a skip option.

### 3. ResultScreen

**Layout (top to bottom):**
- Large score circle: "8 von 10"
- Motivational text based on score (0-4: "Weiter üben!", 5-7: "Gut gemacht!", 8-9: "Super!", 10: "Perfekt!")
- Stats row: Genauigkeit %, Durchschnittszeit pro Note, Beste Serie
- Detail list: each note with checkmark/X and what was played if wrong
- Two buttons: "Zurück" (home) and "Nochmal" (replay same difficulty)

## Data Model

### QuizNote

```
data class QuizNote(
    val name: String,        // "E", "A", "D", etc.
    val octave: Int,         // for staff rendering position
    val staffPosition: Int   // vertical position on staff (0 = bottom line E4, upward)
)
```

### NotePool

Predefined pools of notes by difficulty:

**Level 1 – Open Strings:**
E2, A2, D3, G3, B3, E4

**Level 2 – First Position naturals (Bund 0-3, no sharps/flats):**
E2, F2, G2, A2, B2, C3, D3, E3, F3, G3, A3, B3, C4, D4, E4, F4, G4

**For Phase 1:** Only Level 1 (open strings). Level 2 can be added trivially since the quiz logic is note-pool-agnostic.

### QuizRound

```
data class QuizRound(
    val notes: List<QuizNote>,           // 10 notes, randomly selected from pool
    val results: List<QuizNoteResult>,   // filled as user plays
    val currentIndex: Int
)

data class QuizNoteResult(
    val target: QuizNote,
    val played: String?,       // note name that was detected, or null if skipped
    val correct: Boolean,
    val timeMs: Long           // how long it took to play
)
```

### QuizUiState

```
data class QuizUiState(
    val currentNote: QuizNote?,
    val currentIndex: Int = 0,
    val totalNotes: Int = 10,
    val detectedNoteName: String = "--",
    val feedback: Feedback? = null,     // null = waiting, Correct, Wrong(played)
    val isListening: Boolean = false,
    val results: List<QuizNoteResult> = emptyList()
)

sealed class Feedback {
    object Correct : Feedback()
    data class Wrong(val playedNote: String) : Feedback()
}
```

## Staff Rendering

Render a treble clef staff using Compose Canvas:

- 5 horizontal lines, evenly spaced
- Treble clef symbol (Unicode &#119070; or custom drawing)
- Note head (filled ellipse) positioned vertically based on pitch
- Stem line from note head
- Ledger lines for notes above/below the staff (e.g., E2 needs multiple ledger lines below)

**Staff position mapping (treble clef):**
- Each line/space = one diatonic step
- Bottom line (line 1) = E4
- Space below line 1 = D4
- Line below staff = C4 (first ledger line below)
- Continue downward for lower notes

For guitar notes spanning E2–E4, most notes will need ledger lines below the staff. This is normal for guitar music in treble clef (guitar sounds an octave lower than written).

**Simplified approach for Phase 1:** Since guitar is a transposing instrument (written an octave higher than it sounds), display notes at their written pitch (one octave up). This means:
- E2 guitar → displays as E3 on staff (first ledger line below)
- A2 guitar → displays as A3 on staff (second space below)
- D3 guitar → displays as D4 on staff (space below line 1)
- G3 guitar → displays as G4 on staff (line 2)
- B3 guitar → displays as B4 on staff (line 3)
- E4 guitar → displays as E5 on staff (space above line 4)

This matches how guitar music is actually written in sheet music.

## Architecture Changes

### New Files

```
app/src/main/java/com/noten/app/
├── navigation/
│   └── NotenNavigation.kt       # NavHost with 3 routes: home, quiz, result
├── quiz/
│   ├── QuizNote.kt              # QuizNote, NotePool, QuizNoteResult data classes
│   ├── QuizUiState.kt           # QuizUiState + Feedback sealed class
│   ├── QuizViewModel.kt         # Quiz game logic
│   └── StaffRenderer.kt         # Compose Canvas function to draw staff + note
├── ui/
│   ├── HomeScreen.kt            # New entry screen
│   ├── QuizScreen.kt            # Quiz gameplay screen
│   └── ResultScreen.kt          # Score/results screen
```

### Modified Files

- `MainActivity.kt` – replace direct TunerScreen with NavHost
- `TunerScreen.kt` – add back-navigation button

### Reused Components

- `AudioProcessor` – exact same mic capture + YIN detection
- `NoteUtils` – frequency-to-note conversion
- `YinPitchDetector` – unchanged
- Theme (colors, typography) – reused across all screens

## Quiz Logic Flow

```
HomeScreen → [Start Quiz] → QuizScreen
  → QuizViewModel picks 10 random notes from pool
  → For each note:
      1. Display on staff
      2. Start AudioProcessor
      3. Wait for stable detection (same note for 0.5s)
      4. Compare detected note name to target note name
      5. Record result (correct/wrong, time taken)
      6. Show feedback animation (1-1.5s)
      7. Advance to next note
  → After note 10: navigate to ResultScreen with results

ResultScreen → [Nochmal] → QuizScreen (new round)
ResultScreen → [Zurück] → HomeScreen
```

## Detection Logic for Quiz

Reuse AudioProcessor but with quiz-specific matching:

1. AudioProcessor reports detected frequency
2. QuizViewModel converts to NoteInfo via NoteUtils
3. Compare `detectedNote.name` to `targetNote.name` (octave-insensitive)
4. Require the same note name detected for 3 consecutive callbacks (~0.5s) to avoid false triggers
5. On match: mark correct. On stable mismatch: mark wrong.

## Testing Strategy

- **QuizNote/NotePool**: Unit test that pool contains expected notes, random selection works
- **QuizViewModel**: Unit test the game logic (correct detection advances, wrong detection records, round completes after 10)
- **StaffRenderer**: Manual visual testing (hard to unit test Canvas)
- **Integration**: Manual testing with real guitar
