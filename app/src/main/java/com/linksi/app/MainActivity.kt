package com.linksi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.linksi.app.ui.screens.HomeScreen
import com.linksi.app.ui.screens.OnboardingScreen
import com.linksi.app.ui.theme.LinksTheme
import com.linksi.app.utils.isOnboardingComplete
import com.linksi.app.utils.setOnboardingComplete
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LinksTheme {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()

                // Check if onboarding is complete
                val onboardingComplete by isOnboardingComplete(context)
                    .collectAsState(initial = null)

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (onboardingComplete) {
                        null -> {
                            // Loading — show nothing or splash
                            Box(Modifier.fillMaxSize())
                        }
                        false -> {
                            OnboardingScreen(
                                onFinish = {
                                    scope.launch {
                                        setOnboardingComplete(context)
                                    }
                                }
                            )
                        }
                        true -> {
                            HomeScreen()
                        }
                    }
                }
            }
        }
    }
}
