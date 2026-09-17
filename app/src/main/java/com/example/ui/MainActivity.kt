package com.example.ui

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AiRepository
import com.example.data.AuthRepository
import com.example.data.FirestoreRepository
import com.example.data.ProgressRepository
import com.example.data.SettingsRepository
import com.example.ui.theme.MyApplicationTheme
import com.example.data.AppDatabase
import androidx.datastore.preferences.preferencesDataStore
import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.core.DataStore

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class MainActivity : ComponentActivity() {
    @androidx.compose.material3.ExperimentalMaterial3Api
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val progressDao = AppDatabase.getDatabase(this).progressDao()
        val progressRepository = ProgressRepository(progressDao)
        val settingsRepository = SettingsRepository(this.dataStore)
        val aiRepository = AiRepository()
        val authRepository = AuthRepository(this)
        val firestoreRepository = FirestoreRepository()

        setContent {
            val viewModel: AppViewModel = viewModel(
                factory = AppViewModelFactory(progressRepository, settingsRepository, aiRepository, authRepository, firestoreRepository)
            )
            
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            MyApplicationTheme(
                darkTheme = isDarkMode
            ) {
                AppNavGraph(viewModel = viewModel)
            }
        }
    }
}
