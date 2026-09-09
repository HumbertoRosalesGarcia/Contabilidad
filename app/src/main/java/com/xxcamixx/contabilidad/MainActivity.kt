package com.xxcamixx.contabilidad

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xxcamixx.contabilidad.ui.screens.FinanceScreen
import com.xxcamixx.contabilidad.ui.screens.LoginScreen
import com.xxcamixx.contabilidad.ui.screens.TechSplashScreen
import com.xxcamixx.contabilidad.util.scheduleNextChatSync
import com.xxcamixx.contabilidad.viewmodel.FinanceViewModel
import com.xxcamixx.contabilidad.viewmodel.FinanceViewModelFactory

val CustomDarkColorScheme = darkColorScheme(
    primary = Color(0xFFBB86FC),
    onPrimary = Color.Black,
    secondary = Color(0xFF03DAC6),
    onSecondary = Color.Black,
    background = Color(0xFF121212),
    onBackground = Color.White,
    surface = Color(0xFF1E1E1E),
    onSurface = Color.White,
    error = Color(0xFFCF6679),
    onError = Color.Black
)

class MainActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        scheduleNextChatSync(this)

        setContent {
            val systemTheme = isSystemInDarkTheme()
            var isDarkTheme by remember { mutableStateOf(systemTheme) }
            var showSplash by remember { mutableStateOf(true) }

            val authPrefs = getSharedPreferences("GlobalAuthPrefs", Context.MODE_PRIVATE)
            var loggedInUser by remember { mutableStateOf(authPrefs.getString("userName", null)) }
            var loggedInUserId by remember { mutableStateOf(authPrefs.getString("userId", null)) }
            var userRole by remember { mutableStateOf(authPrefs.getString("userRole", "INVITADO") ?: "INVITADO") }
            var consumedSeconds by remember { mutableStateOf(authPrefs.getLong("consumedSeconds", 0L)) }
            var planDuration by remember { mutableStateOf(authPrefs.getLong("planDuration", 2592000L)) }

            val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
            LaunchedEffect(Unit) { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }

            MaterialTheme(colorScheme = if (isDarkTheme) CustomDarkColorScheme else lightColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (showSplash) {
                        TechSplashScreen(onTimeout = { showSplash = false })
                    } else if (loggedInUserId == null) {
                        LoginScreen(onLoginSuccess = { name, id, role, secs, duration ->
                            authPrefs.edit().putString("userName", name).putString("userId", id).putString("userRole", role).putLong("consumedSeconds", secs).putLong("planDuration", duration).putString("lastKnownUserId", id).putString("lastKnownRole", role).apply()
                            loggedInUser = name; loggedInUserId = id; userRole = role; consumedSeconds = secs; planDuration = duration
                        })
                    } else {
                        val viewModel: FinanceViewModel = viewModel(key = loggedInUserId, factory = FinanceViewModelFactory(application, loggedInUserId!!))
                        FinanceScreen(
                            viewModel = viewModel, userName = loggedInUser ?: "Usuario", initialRole = userRole, initialConsumedSeconds = consumedSeconds, initialPlanDuration = planDuration,
                            onLogout = { authPrefs.edit().remove("userName").remove("userId").remove("userRole").remove("consumedSeconds").remove("planDuration").apply(); loggedInUser = null; loggedInUserId = null; userRole = "INVITADO" },
                            isDarkTheme = isDarkTheme, onThemeToggle = { isDarkTheme = !isDarkTheme }
                        )
                    }
                }
            }
        }
    }
}
