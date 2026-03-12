package com.noten.app

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.noten.app.navigation.NotenNavigation
import com.noten.app.ui.theme.DarkBackground
import com.noten.app.ui.theme.NotenTheme
import com.noten.app.viewmodel.TunerViewModel

class MainActivity : ComponentActivity() {

    private lateinit var tunerViewModel: TunerViewModel

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        tunerViewModel.onPermissionResult(granted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            NotenTheme {
                val vm: TunerViewModel = viewModel()
                tunerViewModel = vm

                val tunerUiState by vm.uiState.collectAsState()

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                    color = DarkBackground
                ) {
                    NotenNavigation(
                        tunerUiState = tunerUiState,
                        onToggleListening = vm::toggleListening,
                        onTuningChanged = vm::setTuning,
                        onRequestPermission = {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    )
                }
            }
        }

        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (::tunerViewModel.isInitialized) {
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> tunerViewModel.onPause()
                    else -> {}
                }
            }
        })
    }
}
