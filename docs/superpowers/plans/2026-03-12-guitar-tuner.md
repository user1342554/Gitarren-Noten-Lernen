# Guitar Note Recognition App – Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an Android app that detects the note played on a guitar via the microphone and displays it in real time with tuner-style visual feedback.

**Architecture:** MVVM with Kotlin + Jetpack Compose. AudioRecord captures mic input, a custom YIN algorithm detects pitch, ViewModel maps frequency to note/cents, Compose UI renders a tuner gauge. No external DSP dependencies.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Android AudioRecord, Gradle (Kotlin DSL), JUnit 5

**Spec:** `docs/superpowers/specs/2026-03-12-guitar-tuner-design.md`

---

## Chunk 1: Project Scaffolding + Core Logic

### Task 1: Scaffold Android Project

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts` (root)
- Create: `gradle.properties`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `gradle/libs.versions.toml`

- [ ] **Step 1: Create root `settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Noten"
include(":app")
```

- [ ] **Step 2: Create root `build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
```

- [ ] **Step 3: Create `gradle/libs.versions.toml`**

```toml
[versions]
agp = "8.7.3"
kotlin = "2.1.0"
coreKtx = "1.15.0"
lifecycleRuntime = "2.8.7"
activityCompose = "1.9.3"
composeBom = "2024.12.01"
junit = "5.10.2"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntime" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleRuntime" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-material3 = { group = "androidx.compose.material3", name = "material3" }
junit-jupiter = { group = "org.junit.jupiter", name = "junit-jupiter", version.ref = "junit" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

- [ ] **Step 4: Create `gradle.properties`**

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

- [ ] **Step 5: Create `app/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.noten.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.noten.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)
    testImplementation(libs.junit.jupiter)
}
```

- [ ] **Step 6: Create `app/src/main/AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.RECORD_AUDIO" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="Noten"
        android:supportsRtl="true"
        android:theme="@style/Theme.Material3.DayNight.NoActionBar">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:screenOrientation="portrait">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 7: Install Gradle wrapper**

Run: Download Gradle wrapper files (gradle-wrapper.jar + gradle-wrapper.properties) or run `gradle wrapper --gradle-version 8.11.1` if Gradle is installed globally.

- [ ] **Step 8: Commit**

```bash
git add settings.gradle.kts build.gradle.kts gradle.properties gradle/ app/build.gradle.kts app/src/main/AndroidManifest.xml
git commit -m "feat: scaffold Android project with Gradle, Compose, and JUnit 5"
```

---

### Task 2: NoteUtils – Frequency to Note Conversion (TDD)

**Files:**
- Create: `app/src/main/java/com/noten/app/model/NoteInfo.kt`
- Create: `app/src/main/java/com/noten/app/model/NoteUtils.kt`
- Create: `app/src/test/java/com/noten/app/model/NoteUtilsTest.kt`

- [ ] **Step 1: Create `NoteInfo` data class**

```kotlin
// app/src/main/java/com/noten/app/model/NoteInfo.kt
package com.noten.app.model

data class NoteInfo(
    val name: String,      // e.g. "A", "C#"
    val octave: Int,       // e.g. 4
    val cents: Double,     // -50.0 to +50.0
    val frequency: Double  // Hz
)
```

- [ ] **Step 2: Write the failing tests**

```kotlin
// app/src/test/java/com/noten/app/model/NoteUtilsTest.kt
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
        // 445 Hz is slightly sharp of A4 (440 Hz)
        val note = NoteUtils.frequencyToNote(445.0)
        assertEquals("A", note.name)
        assertEquals(4, note.octave)
        assert(note.cents > 0) { "Expected positive cents for sharp note, got ${note.cents}" }
    }

    @Test
    fun `flat note returns negative cents`() {
        // 435 Hz is slightly flat of A4 (440 Hz)
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
```

- [ ] **Step 3: Run tests to verify they fail**

Run: `./gradlew test --tests "com.noten.app.model.NoteUtilsTest"`
Expected: Compilation failure — `NoteUtils` doesn't exist

- [ ] **Step 4: Implement `NoteUtils`**

```kotlin
// app/src/main/java/com/noten/app/model/NoteUtils.kt
package com.noten.app.model

import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.roundToInt

object NoteUtils {

    private const val A4_FREQUENCY = 440.0
    private val NOTE_NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    fun frequencyToNote(frequency: Double): NoteInfo {
        val semitonesFromA4 = 12.0 * log2(frequency / A4_FREQUENCY)
        val nearestSemitone = semitonesFromA4.roundToInt()
        val cents = (semitonesFromA4 - nearestSemitone) * 100.0

        val noteIndex = ((nearestSemitone % 12) + 9 + 12) % 12
        val octave = 4 + floor((nearestSemitone + 9).toDouble() / 12.0).toInt()

        return NoteInfo(
            name = NOTE_NAMES[noteIndex],
            octave = octave,
            cents = cents,
            frequency = frequency
        )
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew test --tests "com.noten.app.model.NoteUtilsTest"`
Expected: All 8 tests PASS

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/noten/app/model/ app/src/test/java/com/noten/app/model/
git commit -m "feat: add NoteUtils frequency-to-note conversion with tests"
```

---

### Task 3: YIN Pitch Detection Algorithm (TDD)

**Files:**
- Create: `app/src/main/java/com/noten/app/audio/YinPitchDetector.kt`
- Create: `app/src/test/java/com/noten/app/audio/YinPitchDetectorTest.kt`

- [ ] **Step 1: Write the failing tests**

```kotlin
// app/src/test/java/com/noten/app/audio/YinPitchDetectorTest.kt
package com.noten.app.audio

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertEquals as assertClose
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.sin

class YinPitchDetectorTest {

    private val sampleRate = 44100

    /** Generate a sine wave buffer at the given frequency. */
    private fun generateSineWave(frequency: Double, sampleCount: Int): FloatArray {
        return FloatArray(sampleCount) { i ->
            sin(2.0 * PI * frequency * i / sampleRate).toFloat()
        }
    }

    @Test
    fun `detects A4 at 440 Hz`() {
        val buffer = generateSineWave(440.0, 4096)
        val detected = YinPitchDetector.detect(buffer, sampleRate)
        assertEquals(440.0, detected.toDouble(), 2.0)
    }

    @Test
    fun `detects E2 at 82 Hz`() {
        // E2 needs a larger buffer for enough periods
        val buffer = generateSineWave(82.41, 4096)
        val detected = YinPitchDetector.detect(buffer, sampleRate)
        assertEquals(82.41, detected.toDouble(), 2.0)
    }

    @Test
    fun `detects E4 at 330 Hz`() {
        val buffer = generateSineWave(329.63, 4096)
        val detected = YinPitchDetector.detect(buffer, sampleRate)
        assertEquals(329.63, detected.toDouble(), 2.0)
    }

    @Test
    fun `detects D3 at 147 Hz`() {
        val buffer = generateSineWave(146.83, 4096)
        val detected = YinPitchDetector.detect(buffer, sampleRate)
        assertEquals(146.83, detected.toDouble(), 2.0)
    }

    @Test
    fun `returns -1 for silence`() {
        val buffer = FloatArray(4096) { 0f }
        val detected = YinPitchDetector.detect(buffer, sampleRate)
        assertEquals(-1f, detected)
    }

    @Test
    fun `returns -1 for noise below threshold`() {
        val buffer = FloatArray(4096) { ((Math.random() - 0.5) * 0.001).toFloat() }
        val detected = YinPitchDetector.detect(buffer, sampleRate)
        assertEquals(-1f, detected)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests "com.noten.app.audio.YinPitchDetectorTest"`
Expected: Compilation failure — `YinPitchDetector` doesn't exist

- [ ] **Step 3: Implement YIN algorithm**

```kotlin
// app/src/main/java/com/noten/app/audio/YinPitchDetector.kt
package com.noten.app.audio

object YinPitchDetector {

    const val DEFAULT_THRESHOLD = 0.20f

    /**
     * Detect the fundamental frequency in the given audio buffer using the YIN algorithm.
     *
     * @param buffer Audio samples, normalized -1.0 to 1.0
     * @param sampleRate Sample rate in Hz (e.g. 44100)
     * @param threshold YIN confidence threshold (lower = stricter). Default 0.20.
     * @return Detected frequency in Hz, or -1f if no pitch detected.
     */
    fun detect(
        buffer: FloatArray,
        sampleRate: Int,
        threshold: Float = DEFAULT_THRESHOLD
    ): Float {
        val halfSize = buffer.size / 2

        // Step 1 & 2: Difference function + cumulative mean normalized difference
        val yinBuffer = FloatArray(halfSize)
        yinBuffer[0] = 1.0f

        var runningSum = 0f
        for (tau in 1 until halfSize) {
            var delta = 0f
            for (i in 0 until halfSize) {
                val diff = buffer[i] - buffer[i + tau]
                delta += diff * diff
            }
            runningSum += delta
            yinBuffer[tau] = if (runningSum != 0f) delta * tau / runningSum else 0f
        }

        // Step 3: Absolute threshold — find first dip below threshold
        var tauEstimate = -1
        for (tau in 2 until halfSize) {
            if (yinBuffer[tau] < threshold) {
                // Find the local minimum
                while (tau + 1 < halfSize && yinBuffer[tau + 1] < yinBuffer[tau]) {
                    // tau will be incremented by the for loop, but we need the min
                    break
                }
                tauEstimate = tau
                break
            }
        }

        if (tauEstimate == -1) return -1f

        // Walk to the local minimum from tauEstimate
        var bestTau = tauEstimate
        while (bestTau + 1 < halfSize && yinBuffer[bestTau + 1] < yinBuffer[bestTau]) {
            bestTau++
        }

        // Step 4: Parabolic interpolation for sub-sample accuracy
        val betterTau = parabolicInterpolation(yinBuffer, bestTau)

        return sampleRate / betterTau
    }

    private fun parabolicInterpolation(yinBuffer: FloatArray, tau: Int): Float {
        if (tau <= 0 || tau >= yinBuffer.size - 1) return tau.toFloat()

        val s0 = yinBuffer[tau - 1]
        val s1 = yinBuffer[tau]
        val s2 = yinBuffer[tau + 1]

        val adjustment = (s2 - s0) / (2f * (2f * s1 - s2 - s0))
        return tau + adjustment
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew test --tests "com.noten.app.audio.YinPitchDetectorTest"`
Expected: All 6 tests PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/noten/app/audio/YinPitchDetector.kt app/src/test/java/com/noten/app/audio/
git commit -m "feat: implement YIN pitch detection algorithm with tests"
```

---

## Chunk 2: Audio Pipeline + ViewModel

### Task 4: AudioProcessor – Microphone Capture with Sliding Window

**Files:**
- Create: `app/src/main/java/com/noten/app/audio/AudioProcessor.kt`

- [ ] **Step 1: Implement AudioProcessor**

```kotlin
// app/src/main/java/com/noten/app/audio/AudioProcessor.kt
package com.noten.app.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

class AudioProcessor(
    private val onPitchDetected: (Float) -> Unit,
    private val onSilence: () -> Unit
) {
    companion object {
        const val SAMPLE_RATE = 44100
        const val BUFFER_SIZE = 4096
        private const val STEP_SIZE = 1024 // 75% overlap: read 1024 new samples each iteration
        private const val RMS_THRESHOLD = 0.01f
        private const val MEDIAN_WINDOW = 3
    }

    private var audioRecord: AudioRecord? = null
    private val recentPitches = ArrayDeque<Float>(MEDIAN_WINDOW)

    suspend fun start() = withContext(Dispatchers.IO) {
        val minBufSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBufSize, BUFFER_SIZE * 2)
        )
        audioRecord = record
        record.startRecording()

        val slidingBuffer = FloatArray(BUFFER_SIZE)
        val readBuffer = ShortArray(STEP_SIZE)

        // Fill the initial buffer
        val initialShorts = ShortArray(BUFFER_SIZE)
        var totalRead = 0
        while (totalRead < BUFFER_SIZE && isActive) {
            val read = record.read(initialShorts, totalRead, BUFFER_SIZE - totalRead)
            if (read > 0) totalRead += read
        }
        for (i in 0 until BUFFER_SIZE) {
            slidingBuffer[i] = initialShorts[i] / 32768f
        }

        // Main loop: read STEP_SIZE new samples, shift buffer
        while (isActive) {
            val read = record.read(readBuffer, 0, STEP_SIZE)
            if (read <= 0) continue

            // Shift old samples left
            System.arraycopy(slidingBuffer, STEP_SIZE, slidingBuffer, 0, BUFFER_SIZE - STEP_SIZE)
            // Copy new samples to end
            for (i in 0 until read) {
                slidingBuffer[BUFFER_SIZE - STEP_SIZE + i] = readBuffer[i] / 32768f
            }

            // RMS silence gate
            val rms = computeRms(slidingBuffer)
            if (rms < RMS_THRESHOLD) {
                recentPitches.clear()
                onSilence()
                continue
            }

            // Pitch detection
            val pitch = YinPitchDetector.detect(slidingBuffer, SAMPLE_RATE)
            if (pitch > 0) {
                recentPitches.addLast(pitch)
                if (recentPitches.size > MEDIAN_WINDOW) recentPitches.removeFirst()

                if (recentPitches.size == MEDIAN_WINDOW) {
                    val median = medianOf3(recentPitches)
                    onPitchDetected(median)
                }
            } else {
                recentPitches.clear()
                onSilence()
            }
        }

        record.stop()
        record.release()
        audioRecord = null
    }

    fun stop() {
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: IllegalStateException) { }
        audioRecord = null
        recentPitches.clear()
    }

    private fun computeRms(buffer: FloatArray): Float {
        var sum = 0f
        for (sample in buffer) sum += sample * sample
        return sqrt(sum / buffer.size)
    }

    private fun medianOf3(values: ArrayDeque<Float>): Float {
        val sorted = values.toList().sorted()
        return sorted[sorted.size / 2]
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/noten/app/audio/AudioProcessor.kt
git commit -m "feat: add AudioProcessor with sliding window, silence gate, and median smoothing"
```

---

### Task 5: TunerUiState + TunerViewModel

**Files:**
- Create: `app/src/main/java/com/noten/app/model/TunerUiState.kt`
- Create: `app/src/main/java/com/noten/app/viewmodel/TunerViewModel.kt`

- [ ] **Step 1: Create TunerUiState**

```kotlin
// app/src/main/java/com/noten/app/model/TunerUiState.kt
package com.noten.app.model

data class TunerUiState(
    val noteName: String = "--",
    val octave: Int = 0,
    val cents: Double = 0.0,
    val frequency: Double = 0.0,
    val isListening: Boolean = false,
    val hasPermission: Boolean = false
)
```

- [ ] **Step 2: Create TunerViewModel**

```kotlin
// app/src/main/java/com/noten/app/viewmodel/TunerViewModel.kt
package com.noten.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noten.app.audio.AudioProcessor
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
            // Mark that we should resume when coming back
            _uiState.update { it.copy(isListening = false) }
        }
    }

    private fun startListening() {
        val processor = AudioProcessor(
            onPitchDetected = { frequency ->
                val note = NoteUtils.frequencyToNote(frequency.toDouble())

                // Note hold: require consecutive frames before switching
                if (note.name != lastNoteName) {
                    noteHoldCount++
                    if (noteHoldCount < noteHoldThreshold) return@AudioProcessor
                    lastNoteName = note.name
                    noteHoldCount = 0
                } else {
                    noteHoldCount = 0
                }

                _uiState.update {
                    it.copy(
                        noteName = note.name,
                        octave = note.octave,
                        cents = note.cents,
                        frequency = note.frequency
                    )
                }
            },
            onSilence = {
                _uiState.update {
                    it.copy(noteName = "--", octave = 0, cents = 0.0, frequency = 0.0)
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
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/noten/app/model/TunerUiState.kt app/src/main/java/com/noten/app/viewmodel/
git commit -m "feat: add TunerUiState and TunerViewModel with lifecycle management"
```

---

## Chunk 3: UI + MainActivity

### Task 6: Theme Setup

**Files:**
- Create: `app/src/main/java/com/noten/app/ui/theme/Color.kt`
- Create: `app/src/main/java/com/noten/app/ui/theme/Type.kt`
- Create: `app/src/main/java/com/noten/app/ui/theme/Theme.kt`

- [ ] **Step 1: Create Color.kt**

```kotlin
// app/src/main/java/com/noten/app/ui/theme/Color.kt
package com.noten.app.ui.theme

import androidx.compose.ui.graphics.Color

val InTuneGreen = Color(0xFF4CAF50)
val CloseYellow = Color(0xFFFFEB3B)
val OffRed = Color(0xFFF44336)
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val TextWhite = Color(0xFFE0E0E0)
val TextGray = Color(0xFF9E9E9E)
```

- [ ] **Step 2: Create Type.kt**

```kotlin
// app/src/main/java/com/noten/app/ui/theme/Type.kt
package com.noten.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val NotenTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 96.sp,
        letterSpacing = (-1.5).sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.5.sp
    )
)
```

- [ ] **Step 3: Create Theme.kt**

```kotlin
// app/src/main/java/com/noten/app/ui/theme/Theme.kt
package com.noten.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = InTuneGreen,
    secondary = CloseYellow,
    error = OffRed,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = TextWhite,
    onSurface = TextWhite
)

@Composable
fun NotenTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = NotenTypography,
        content = content
    )
}
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/noten/app/ui/theme/
git commit -m "feat: add dark theme with tuner color scheme"
```

---

### Task 7: TunerScreen Compose UI

**Files:**
- Create: `app/src/main/java/com/noten/app/ui/TunerScreen.kt`

- [ ] **Step 1: Implement TunerScreen**

```kotlin
// app/src/main/java/com/noten/app/ui/TunerScreen.kt
package com.noten.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noten.app.model.TunerUiState
import com.noten.app.ui.theme.*
import kotlin.math.abs

@Composable
fun TunerScreen(
    uiState: TunerUiState,
    onToggleListening: () -> Unit,
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (!uiState.hasPermission) {
            Text(
                text = "Mikrofon-Zugriff wird benötigt",
                color = TextWhite,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRequestPermission) {
                Text("Erlauben")
            }
            return
        }

        // Tuner gauge
        val animatedCents by animateFloatAsState(
            targetValue = uiState.cents.toFloat(),
            animationSpec = tween(durationMillis = 150),
            label = "cents"
        )
        val tuneColor = centsToColor(uiState.cents)
        val animatedColor by animateColorAsState(
            targetValue = tuneColor,
            animationSpec = tween(durationMillis = 200),
            label = "color"
        )

        TunerGauge(
            cents = animatedCents,
            color = animatedColor,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        )

        // Cents label
        Text(
            text = if (uiState.noteName == "--") "" else "%+.0f cents".format(uiState.cents),
            color = animatedColor,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Note name
        Text(
            text = if (uiState.noteName == "--") "--" else "${uiState.noteName}${uiState.octave}",
            color = if (uiState.noteName == "--") TextGray else animatedColor,
            fontSize = 96.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        // Frequency
        Text(
            text = if (uiState.frequency > 0) "%.1f Hz".format(uiState.frequency) else "",
            color = TextGray,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(64.dp))

        // Start/Stop button
        Button(
            onClick = onToggleListening,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (uiState.isListening) OffRed else InTuneGreen
            ),
            modifier = Modifier
                .width(200.dp)
                .height(56.dp)
        ) {
            Text(
                text = if (uiState.isListening) "STOP" else "START",
                fontSize = 20.sp
            )
        }
    }
}

@Composable
private fun TunerGauge(
    cents: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val gaugeWidth = size.width * 0.8f
        val gaugeLeft = centerX - gaugeWidth / 2
        val gaugeRight = centerX + gaugeWidth / 2

        // Background track
        drawLine(
            color = Color(0xFF333333),
            start = Offset(gaugeLeft, centerY),
            end = Offset(gaugeRight, centerY),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )

        // Center tick
        drawLine(
            color = Color(0xFF666666),
            start = Offset(centerX, centerY - 20f),
            end = Offset(centerX, centerY + 20f),
            strokeWidth = 2f
        )

        // Indicator position: map -50..+50 cents to gaugeLeft..gaugeRight
        val clampedCents = cents.coerceIn(-50f, 50f)
        val indicatorX = centerX + (clampedCents / 50f) * (gaugeWidth / 2)

        // Indicator
        drawCircle(
            color = color,
            radius = 12f,
            center = Offset(indicatorX, centerY)
        )
    }
}

private fun centsToColor(cents: Double): Color {
    val absCents = abs(cents)
    return when {
        absCents <= 5.0 -> InTuneGreen
        absCents <= 15.0 -> CloseYellow
        else -> OffRed
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/noten/app/ui/TunerScreen.kt
git commit -m "feat: add TunerScreen with animated gauge and color-coded feedback"
```

---

### Task 8: MainActivity – Entry Point + Permission Handling

**Files:**
- Create: `app/src/main/java/com/noten/app/MainActivity.kt`

- [ ] **Step 1: Implement MainActivity**

```kotlin
// app/src/main/java/com/noten/app/MainActivity.kt
package com.noten.app

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.noten.app.ui.TunerScreen
import com.noten.app.ui.theme.DarkBackground
import com.noten.app.ui.theme.NotenTheme
import com.noten.app.viewmodel.TunerViewModel

class MainActivity : ComponentActivity() {

    private lateinit var tunerViewModel: TunerViewModel

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        tunerViewModel.onPermissionResult(granted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            NotenTheme {
                val vm: TunerViewModel = viewModel()
                tunerViewModel = vm

                val uiState by vm.uiState.collectAsState()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    TunerScreen(
                        uiState = uiState,
                        onToggleListening = vm::toggleListening,
                        onRequestPermission = {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    )
                }
            }
        }

        // Stop audio when going to background, restart when returning
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (::tunerViewModel.isInitialized) {
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> tunerViewModel.onPause()
                    else -> {}
                }
            }
        })
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/noten/app/MainActivity.kt
git commit -m "feat: add MainActivity with permission handling and lifecycle management"
```

---

### Task 9: Verify Build

- [ ] **Step 1: Run full build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Run all tests**

Run: `./gradlew test`
Expected: All tests PASS (NoteUtils: 8, YinPitchDetector: 6)

- [ ] **Step 3: Final commit if any fixes needed**

If build or tests required fixes, commit those fixes.

---

## Task Dependency Order

```
Task 1 (scaffold) → Task 2 (NoteUtils) → Task 3 (YIN) → Task 4 (AudioProcessor) → Task 5 (ViewModel) → Task 6 (Theme) → Task 7 (TunerScreen) → Task 8 (MainActivity) → Task 9 (Verify)
```

All tasks are sequential — each builds on the previous.
