package com.noten.app.model

data class TuningString(
    val name: String,       // Display name e.g. "E2"
    val noteName: String,   // e.g. "E"
    val octave: Int,
    val frequency: Double   // Target frequency in Hz
)

data class GuitarTuning(
    val name: String,
    val strings: List<TuningString>
) {
    fun closestString(frequency: Double): TuningString? {
        if (strings.isEmpty()) return null
        return strings.minByOrNull {
            kotlin.math.abs(frequencyToCents(frequency, it.frequency))
        }
    }

    companion object {
        fun frequencyToCents(detected: Double, target: Double): Double {
            return 1200.0 * kotlin.math.log2(detected / target)
        }
    }
}

object GuitarTunings {
    val STANDARD = GuitarTuning(
        "Standard", listOf(
            TuningString("E2", "E", 2, 82.41),
            TuningString("A2", "A", 2, 110.00),
            TuningString("D3", "D", 3, 146.83),
            TuningString("G3", "G", 3, 196.00),
            TuningString("B3", "B", 3, 246.94),
            TuningString("E4", "E", 4, 329.63),
        )
    )

    val DROP_D = GuitarTuning(
        "Drop D", listOf(
            TuningString("D2", "D", 2, 73.42),
            TuningString("A2", "A", 2, 110.00),
            TuningString("D3", "D", 3, 146.83),
            TuningString("G3", "G", 3, 196.00),
            TuningString("B3", "B", 3, 246.94),
            TuningString("E4", "E", 4, 329.63),
        )
    )

    val OPEN_G = GuitarTuning(
        "Open G", listOf(
            TuningString("D2", "D", 2, 73.42),
            TuningString("G2", "G", 2, 98.00),
            TuningString("D3", "D", 3, 146.83),
            TuningString("G3", "G", 3, 196.00),
            TuningString("B3", "B", 3, 246.94),
            TuningString("D4", "D", 4, 293.66),
        )
    )

    val DADGAD = GuitarTuning(
        "DADGAD", listOf(
            TuningString("D2", "D", 2, 73.42),
            TuningString("A2", "A", 2, 110.00),
            TuningString("D3", "D", 3, 146.83),
            TuningString("G3", "G", 3, 196.00),
            TuningString("A3", "A", 3, 220.00),
            TuningString("D4", "D", 4, 293.66),
        )
    )

    val HALF_STEP_DOWN = GuitarTuning(
        "Eb Standard", listOf(
            TuningString("Eb2", "D#", 2, 77.78),
            TuningString("Ab2", "G#", 2, 103.83),
            TuningString("Db3", "C#", 3, 138.59),
            TuningString("Gb3", "F#", 3, 185.00),
            TuningString("Bb3", "A#", 3, 233.08),
            TuningString("Eb4", "D#", 4, 311.13),
        )
    )

    val ALL = listOf(STANDARD, DROP_D, HALF_STEP_DOWN, OPEN_G, DADGAD)
}
