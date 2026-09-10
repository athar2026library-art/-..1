package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ProgressRepository
import com.example.data.SettingsRepository
import com.example.data.dataStore
import com.example.ui.AppNavGraph
import com.example.ui.AppViewModel
import com.example.ui.AppViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val progressRepository = ProgressRepository.getInstance(applicationContext)
        val settingsRepository = SettingsRepository(applicationContext.dataStore)
        
        setContent {
            val viewModel: AppViewModel = viewModel(
                factory = AppViewModelFactory(progressRepository, settingsRepository)
            )
            
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            
            val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            val isFajrTime = currentHour in 3..6
            val finalDarkMode = isDarkMode || isFajrTime
            
            MyApplicationTheme(darkTheme = finalDarkMode, isFajrMode = isFajrTime) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavGraph(viewModel = viewModel)
                }
            }
        }
    }
}
