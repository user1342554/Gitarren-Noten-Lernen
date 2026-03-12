package com.noten.app.audio

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.sin

class YinPitchDetectorTest {

    private val sampleRate = 44100

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
