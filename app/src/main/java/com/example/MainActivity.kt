package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.ScannerViewModel
import com.example.ui.screens.GeneratorScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ResultScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge-to-edge safe area drawing
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val viewModel: ScannerViewModel = viewModel()

                // Register native toast receiver for state notifications
                LaunchedEffect(Unit) {
                    viewModel.notification.collect { message ->
                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "scanner",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        composable("scanner") {
                            ScannerScreen(
                                viewModel = viewModel,
                                onNavigateToHistory = { navController.navigate("history") },
                                onNavigateToGenerator = { navController.navigate("generator") },
                                onNavigateToDetail = { navController.navigate("detail") }
                            )
                        }
                        composable("detail") {
                            ResultScreen(
                                viewModel = viewModel,
                                onNavigateBack = {
                                    viewModel.resetScannerState()
                                    navController.popBackStack()
                                }
                            )
                        }
                        composable("history") {
                            HistoryScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToDetail = { navController.navigate("detail") }
                            )
                        }
                        composable("generator") {
                            GeneratorScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
