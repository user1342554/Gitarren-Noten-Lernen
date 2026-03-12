package com.noten.app.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noten.app.ui.theme.*

@Composable
fun ResultScreen(
    score: Int,
    total: Int,
    onPlayAgain: () -> Unit,
    onGoHome: () -> Unit
) {
    val percentage = if (total > 0) (score * 100) / total else 0
    val scoreColor = when {
        percentage >= 80 -> InTuneGreen
        percentage >= 50 -> CloseYellow
        else -> OffRed
    }
    val message = when {
        percentage == 100 -> "Perfekt!"
        percentage >= 80 -> "Super!"
        percentage >= 50 -> "Gut gemacht!"
        else -> "Weiter \u00fcben!"
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Score circle
        Box(
            modifier = Modifier
                .size(150.dp)
                .border(6.dp, scoreColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "$score",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = scoreColor
                )
                Text(
                    "von $total",
                    fontSize = 16.sp,
                    color = TextGray
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            message,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = scoreColor
        )

        // Percentage
        Text(
            "$percentage% Genauigkeit",
            color = TextGray,
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(64.dp))

        // Buttons
        Button(
            onClick = onPlayAgain,
            colors = ButtonDefaults.buttonColors(containerColor = InTuneGreen),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Nochmal", fontSize = 20.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onGoHome,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Zur\u00fcck", fontSize = 20.sp, color = TextWhite)
        }
    }
}
