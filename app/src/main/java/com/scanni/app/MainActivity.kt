package com.scanni.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scanni.app.domain.model.AppSettings
import com.scanni.app.navigation.ScanniNavHost
import com.scanni.app.ui.theme.ScanniTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val graph = (application as ScanniApplication).graph
        setContent {
            val settings by graph.settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = AppSettings())
            ScanniTheme(
                themeMode = settings.themeMode,
                dynamicColor = settings.dynamicColor,
            ) {
                ScanniNavHost(graph)
            }
        }
    }
}
