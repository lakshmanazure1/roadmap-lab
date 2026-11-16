package com.lakshman.roadmaplab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lakshman.roadmaplab.ui.AppRoot
import com.lakshman.roadmaplab.ui.LabViewModel
import com.lakshman.roadmaplab.ui.rememberBackupHooks
import com.lakshman.roadmaplab.ui.theme.LocalPalette
import com.lakshman.roadmaplab.ui.theme.RoadmapLabTheme

class MainActivity : ComponentActivity() {
    private val vm: LabViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val ui by vm.ui.collectAsStateWithLifecycle()
            RoadmapLabTheme(ui?.s?.settings?.theme ?: "light") {
                val dark = LocalPalette.current.isDark
                DisposableEffect(dark) {
                    val transparent = android.graphics.Color.TRANSPARENT
                    val style = if (dark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent)
                    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                    onDispose { }
                }
                AppRoot(ui, vm, vm.notes, rememberBackupHooks(vm, ui?.s?.settings?.freshStartDay))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Keeps "today" right if the app was left in the background overnight.
        vm.refreshDay()
    }
}
