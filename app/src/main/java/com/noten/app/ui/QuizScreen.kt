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
import com.noten.app.quiz.Difficulty
import com.noten.app.quiz.Feedback
import com.noten.app.quiz.QuizViewModel
import com.noten.app.quiz.StaffNotation
import com.noten.app.ui.theme.*

@Composable
fun QuizScreen(
    difficulty: Difficulty,
    onStop: (score: Int, total: Int) -> Unit,
    onBack: () -> Unit
) {
    val vm: QuizViewModel = viewModel()
    val uiState by vm.uiState.collectAsState()

    // Set difficulty before starting
    LaunchedEffect(difficulty) {
        vm.setDifficulty(difficulty)
    }

    // Permission handling
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        vm.onPermissionResult(granted)
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                val (score, total) = vm.stopAndGetResults()
                if (total > 0) {
                    onStop(score, total)
                } else {
                    onBack()
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zur\u00fcck", tint = TextWhite)
            }
            Spacer(modifier = Modifier.weight(1f))

            // Running score
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${uiState.score} richtig",
                    color = InTuneGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                if (uiState.streak > 1) {
                    Text(
                        "${uiState.streak}x Serie",
                        color = CloseYellow,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Difficulty label
        Text(
            uiState.difficulty.label,
            color = TextGray,
            fontSize = 12.sp,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

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
                modifier = Modifier.fillMaxWidth().height(220.dp),
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
                Text(
                    "Das war ${fb.playedNote} \u2013 nochmal!",
                    color = OffRed,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }
            null -> {
                if (uiState.isListening) {
                    Text("\uD83C\uDFB5", fontSize = 48.sp)
                    Text(
                        "Lausche...",
                        color = InTuneGreen,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
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

        Spacer(modifier = Modifier.weight(1f))

        // Stop button
        OutlinedButton(
            onClick = {
                val (score, total) = vm.stopAndGetResults()
                if (total > 0) {
                    onStop(score, total)
                } else {
                    onBack()
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Beenden", fontSize = 16.sp, color = TextWhite)
        }
    }
}
