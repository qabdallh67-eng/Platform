package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.api.SupabaseClient
import com.example.data.repository.EduRepository
import com.example.ui.navigation.AppMainContent
import com.example.ui.theme.AbouQaoudTheme
import com.example.ui.viewmodel.EduViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: EduViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return EduViewModel(application, EduRepository(SupabaseClient(application))) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AbouQaoudTheme(darkTheme = true) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppMainContent(viewModel = viewModel)
                }
            }
        }
    }
}
