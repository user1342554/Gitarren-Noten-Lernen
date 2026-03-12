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
        private const val STEP_SIZE = 1024
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

        val initialShorts = ShortArray(BUFFER_SIZE)
        var totalRead = 0
        while (totalRead < BUFFER_SIZE && isActive) {
            val read = record.read(initialShorts, totalRead, BUFFER_SIZE - totalRead)
            if (read > 0) totalRead += read
        }
        for (i in 0 until BUFFER_SIZE) {
            slidingBuffer[i] = initialShorts[i] / 32768f
        }

        while (isActive) {
            val read = record.read(readBuffer, 0, STEP_SIZE)
            if (read <= 0) continue

            System.arraycopy(slidingBuffer, STEP_SIZE, slidingBuffer, 0, BUFFER_SIZE - STEP_SIZE)
            for (i in 0 until read) {
                slidingBuffer[BUFFER_SIZE - STEP_SIZE + i] = readBuffer[i] / 32768f
            }

            val rms = computeRms(slidingBuffer)
            if (rms < RMS_THRESHOLD) {
                recentPitches.clear()
                onSilence()
                continue
            }

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
