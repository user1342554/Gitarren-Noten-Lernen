package com.noten.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.noten.app.model.TunerUiState
import com.noten.app.ui.theme.*
import kotlin.math.abs

@Composable
fun TunerScreen(
    uiState: TunerUiState,
    onToggleListening: () -> Unit,
    onRequestPermission: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextWhite
                )
            }
        }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (!uiState.hasPermission) {
            Text(
                text = "Mikrofon-Zugriff wird ben\u00f6tigt",
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

        Text(
            text = if (uiState.noteName == "--") "" else "%+.0f cents".format(uiState.cents),
            color = animatedColor,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = if (uiState.noteName == "--") "--" else "${uiState.noteName}${uiState.octave}",
            color = if (uiState.noteName == "--") TextGray else animatedColor,
            fontSize = 96.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = if (uiState.frequency > 0) "%.1f Hz".format(uiState.frequency) else "",
            color = TextGray,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(64.dp))

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

        drawLine(
            color = Color(0xFF333333),
            start = Offset(gaugeLeft, centerY),
            end = Offset(gaugeRight, centerY),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )

        drawLine(
            color = Color(0xFF666666),
            start = Offset(centerX, centerY - 20f),
            end = Offset(centerX, centerY + 20f),
            strokeWidth = 2f
        )

        val clampedCents = cents.coerceIn(-50f, 50f)
        val indicatorX = centerX + (clampedCents / 50f) * (gaugeWidth / 2)

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
