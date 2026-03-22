package de.tadris.fitness.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import de.tadris.fitness.Instance
import de.tadris.fitness.ui.dashboard.DashboardScreen
import de.tadris.fitness.ui.theme.FitoTrackTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dao = Instance.getInstance(this).db.workoutDao()
        enableEdgeToEdge()
        setContent {
            FitoTrackTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // TODO consume innerPadding
                    DashboardScreen(dao)
                }
            }
        }
    }
}