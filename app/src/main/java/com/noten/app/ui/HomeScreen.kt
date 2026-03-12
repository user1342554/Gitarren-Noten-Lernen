package com.noten.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noten.app.ui.theme.*

@Composable
fun HomeScreen(
    onStartQuiz: () -> Unit,
    onOpenTuner: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Noten",
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold,
            color = TextWhite
        )
        Text(
            text = "Lerne Noten auf der Gitarre",
            color = TextGray,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 64.dp)
        )

        Button(
            onClick = onStartQuiz,
            colors = ButtonDefaults.buttonColors(containerColor = InTuneGreen),
            modifier = Modifier.fillMaxWidth().height(64.dp)
        ) {
            Text("Noten Quiz", fontSize = 20.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onOpenTuner,
            modifier = Modifier.fillMaxWidth().height(64.dp)
        ) {
            Text("Stimmger\u00e4t", fontSize = 20.sp, color = TextWhite)
        }
    }
}
