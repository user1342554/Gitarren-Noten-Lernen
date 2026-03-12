package com.noten.app.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.noten.app.quiz.Feedback
import com.noten.app.quiz.QuizViewModel
import com.noten.app.quiz.StaffNotation
import com.noten.app.ui.theme.*

@Composable
fun QuizScreen(
    onFinished: (score: Int, total: Int) -> Unit,
    onBack: () -> Unit
) {
    val vm: QuizViewModel = viewModel()
    val uiState by vm.uiState.collectAsState()

    // Permission handling
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        vm.onPermissionResult(granted)
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    // Navigate to results when finished
    LaunchedEffect(uiState.isFinished) {
        if (uiState.isFinished) {
            val score = uiState.results.count { it.correct }
            onFinished(score, uiState.totalNotes)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top bar with back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zur\u00fcck", tint = TextWhite)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                "${uiState.currentIndex + 1} / ${uiState.totalNotes}",
                color = TextGray,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        // Progress bar
        LinearProgressIndicator(
            progress = { (uiState.currentIndex.toFloat()) / uiState.totalNotes },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            color = InTuneGreen,
            trackColor = DarkSurface,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Staff notation
        val noteColor = when (uiState.feedback) {
            is Feedback.Correct -> InTuneGreen
            is Feedback.Wrong -> OffRed
            null -> Color.White
        }
        val animatedNoteColor by animateColorAsState(
            targetValue = noteColor,
            animationSpec = tween(200),
            label = "noteColor"
        )

        if (uiState.currentNote != null) {
            Card(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1a1a2e))
            ) {
                StaffNotation(
                    staffPosition = uiState.currentNote!!.staffPosition,
                    noteColor = animatedNoteColor,
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Feedback display
        when (val fb = uiState.feedback) {
            is Feedback.Correct -> {
                Text("\u2713", fontSize = 64.sp, color = InTuneGreen)
                Text("Richtig!", color = InTuneGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            is Feedback.Wrong -> {
                Text("\u2717", fontSize = 64.sp, color = OffRed)
                Text("Falsch \u2013 du hast ${fb.playedNote} gespielt", color = OffRed, fontSize = 16.sp)
            }
            null -> {
                // Listening indicator
                if (uiState.isListening) {
                    Text("\uD83C\uDFB5", fontSize = 48.sp)
                    Text("Lausche...", color = InTuneGreen, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Detected note
        Text(
            text = uiState.detectedNoteName,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = if (uiState.detectedNoteName == "--") TextGray else TextWhite,
            textAlign = TextAlign.Center
        )
        Text("Erkannt", color = TextGray, fontSize = 12.sp)
    }
}
