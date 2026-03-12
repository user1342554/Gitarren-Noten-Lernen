package com.noten.app.audio

object YinPitchDetector {

    const val DEFAULT_THRESHOLD = 0.20f

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
            yinBuffer[tau] = if (runningSum != 0f) delta * tau / runningSum else 1f
        }

        // Step 3: Absolute threshold — find first dip below threshold
        var tauEstimate = -1
        for (tau in 2 until halfSize) {
            if (yinBuffer[tau] < threshold) {
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
