package io.github.kanggod9.diettracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.kanggod9.diettracker.data.LocalStore
import io.github.kanggod9.diettracker.ui.DietTrackerApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = LocalStore(applicationContext)
        setContent { DietTrackerApp(store) }
    }
}
