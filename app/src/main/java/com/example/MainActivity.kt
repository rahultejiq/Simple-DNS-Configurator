package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.data.database.AppDatabase
import com.example.data.repository.DnsRepository
import com.example.ui.DnsAppScreen
import com.example.ui.DnsViewModel
import com.example.ui.DnsViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge-to-edge full screeen setup
        enableEdgeToEdge()

        // 1. Instantiate the Room Database
        val database = AppDatabase.getInstance(applicationContext)

        // 2. Instantiate core Repository
        val repository = DnsRepository(
            serverDao = database.dnsServerDao(),
            logDao = database.queryLogDao()
        )

        // 3. Obtain ViewModel scoped to this Activity
        val viewModel: DnsViewModel by viewModels {
            DnsViewModelFactory(repository)
        }

        setContent {
            MyApplicationTheme {
                DnsAppScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@androidx.compose.runtime.Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    androidx.compose.material3.Text(text = "Hello $name!", modifier = modifier)
}
