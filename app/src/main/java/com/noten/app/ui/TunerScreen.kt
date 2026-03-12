package com.noten.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noten.app.model.GuitarTuning
import com.noten.app.model.GuitarTunings
import com.noten.app.model.TunerUiState
import com.noten.app.model.TuningString
import com.noten.app.ui.theme.*
import kotlin.math.abs

@Composable
fun TunerScreen(
    uiState: TunerUiState,
    onToggleListening: () -> Unit,
    onRequestPermission: () -> Unit,
    onTuningChanged: (GuitarTuning) -> Unit = {},
    onBack: (() -> Unit)? = null
) {
    var showTuningPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zur\u00fcck", tint = TextWhite)
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.weight(1f))

            // Tuning picker button
            Row(
                modifier = Modifier
                    .clickable { showTuningPicker = true }
                    .background(DarkSurface, RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(uiState.tuning.name, color = TextWhite, fontSize = 16.sp)
                Icon(Icons.Filled.KeyboardArrowDown, null, tint = TextGray, modifier = Modifier.size(20.dp))
            }
        }

        // Tuning picker dropdown
        DropdownMenu(
            expanded = showTuningPicker,
            onDismissRequest = { showTuningPicker = false }
        ) {
            GuitarTunings.ALL.forEach { tuning ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(tuning.name, fontWeight = FontWeight.Bold)
                            Text(
                                tuning.strings.joinToString(" ") { it.name },
                                fontSize = 12.sp,
                                color = TextGray
                            )
                        }
                    },
                    onClick = {
                        onTuningChanged(tuning)
                        showTuningPicker = false
                    }
                )
            }
        }

        if (!uiState.hasPermission) {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                "Mikrofon-Zugriff wird ben\u00f6tigt",
                color = TextWhite,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRequestPermission) { Text("Erlauben") }
            Spacer(modifier = Modifier.weight(1f))
            return
        }

        Spacer(modifier = Modifier.height(16.dp))

        // String indicators
        StringIndicators(
            strings = uiState.tuning.strings,
            activeString = uiState.closestString,
            centsFromTarget = uiState.centsFromTarget
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Main gauge
        val centsForGauge = uiState.centsFromTarget
        val animatedCents by animateFloatAsState(
            targetValue = centsForGauge.toFloat(),
            animationSpec = tween(durationMillis = 150),
            label = "cents"
        )
        val tuneColor = centsToColor(centsForGauge)
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
                .height(100.dp)
        )

        // Cents label
        Text(
            text = if (uiState.closestString != null) "%+.0f cents".format(centsForGauge) else "",
            color = animatedColor,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Target string info
        if (uiState.closestString != null) {
            Text(
                "Ziel: ${uiState.closestString!!.name}",
                color = TextGray,
                fontSize = 14.sp
            )
            Text(
                "%.1f Hz".format(uiState.closestString!!.frequency),
                color = TextGray,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Detected note (big)
        Text(
            text = if (uiState.noteName == "--") "--" else "${uiState.noteName}${uiState.octave}",
            color = if (uiState.noteName == "--") TextGray else animatedColor,
            fontSize = 72.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        // Detected frequency
        Text(
            text = if (uiState.frequency > 0) "%.1f Hz".format(uiState.frequency) else "",
            color = TextGray,
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Auto-listen: start automatically, but keep toggle for manual control
        if (!uiState.isListening) {
            Button(
                onClick = onToggleListening,
                colors = ButtonDefaults.buttonColors(containerColor = InTuneGreen),
                modifier = Modifier.width(200.dp).height(52.dp)
            ) {
                Text("START", fontSize = 18.sp)
            }
        } else {
            OutlinedButton(
                onClick = onToggleListening,
                modifier = Modifier.width(200.dp).height(52.dp)
            ) {
                Text("STOP", fontSize = 18.sp, color = TextGray)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun StringIndicators(
    strings: List<TuningString>,
    activeString: TuningString?,
    centsFromTarget: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        strings.forEach { string ->
            val isActive = activeString == string
            val color = when {
                !isActive -> TextGray
                abs(centsFromTarget) <= 5.0 -> InTuneGreen
                abs(centsFromTarget) <= 15.0 -> CloseYellow
                else -> OffRed
            }
            val animatedColor by animateColorAsState(
                targetValue = color,
                animationSpec = tween(200),
                label = "stringColor"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .then(
                            if (isActive) Modifier.border(2.dp, animatedColor, CircleShape)
                            else Modifier
                        )
                        .background(
                            if (isActive) animatedColor.copy(alpha = 0.15f) else Color.Transparent,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        string.noteName,
                        color = animatedColor,
                        fontSize = 18.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                    )
                }
                Text(
                    string.name,
                    color = if (isActive) animatedColor else TextGray.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
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
        val gaugeWidth = size.width * 0.85f
        val gaugeLeft = centerX - gaugeWidth / 2
        val gaugeRight = centerX + gaugeWidth / 2

        // Background track
        drawLine(
            color = Color(0xFF2A2A2A),
            start = Offset(gaugeLeft, centerY),
            end = Offset(gaugeRight, centerY),
            strokeWidth = 6f,
            cap = StrokeCap.Round
        )

        // Tick marks
        val tickCount = 10
        for (i in 0..tickCount) {
            val x = gaugeLeft + (gaugeWidth * i / tickCount)
            val isMajor = i == 0 || i == tickCount / 2 || i == tickCount
            val tickHeight = if (isMajor) 16f else 8f
            drawLine(
                color = if (isMajor) Color(0xFF555555) else Color(0xFF3A3A3A),
                start = Offset(x, centerY - tickHeight),
                end = Offset(x, centerY + tickHeight),
                strokeWidth = if (isMajor) 2f else 1f
            )
        }

        // Center zone highlight (green zone)
        val greenWidth = gaugeWidth * 0.1f // ±5 cents out of ±50
        drawLine(
            color = InTuneGreen.copy(alpha = 0.3f),
            start = Offset(centerX - greenWidth, centerY),
            end = Offset(centerX + greenWidth, centerY),
            strokeWidth = 6f,
            cap = StrokeCap.Round
        )

        // Indicator
        val clampedCents = cents.coerceIn(-50f, 50f)
        val indicatorX = centerX + (clampedCents / 50f) * (gaugeWidth / 2)

        // Indicator line
        drawLine(
            color = color,
            start = Offset(indicatorX, centerY - 24f),
            end = Offset(indicatorX, centerY + 24f),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )

        // Indicator dot
        drawCircle(
            color = color,
            radius = 8f,
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
