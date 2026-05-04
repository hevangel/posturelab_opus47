package com.posturelab.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.posturelab.app.ui.AppViewModel
import com.posturelab.app.ui.CaptureScreen
import com.posturelab.app.ui.PostureLabTheme
import com.posturelab.app.ui.ReportScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PostureLabTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot()
                }
            }
        }
    }
}

@Composable
private fun AppRoot() {
    val vm: AppViewModel = viewModel()
    var screen by remember { mutableStateOf(Screen.Capture) }

    when (screen) {
        Screen.Capture -> CaptureScreen(vm) { screen = Screen.Report }
        Screen.Report -> ReportScreen(vm) { screen = Screen.Capture }
    }
}

enum class Screen { Capture, Report }
