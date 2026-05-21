package com.example.kt_fife

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.kt_fife.ui.theme.KT_fifeTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.example.kt_fife.data.local.TokenManager
import com.example.kt_fife.navigation.NavGraph

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tokenManager.clearTokens()

        setContent {
            KT_fifeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavGraph(tokenManager = tokenManager)
                }
            }
        }
    }
}