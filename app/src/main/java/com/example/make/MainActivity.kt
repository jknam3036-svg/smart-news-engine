package com.example.make

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.make.ui.MakeApp
import com.example.make.ui.theme.MakeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as MakeApplication
        setContent {
            MakeTheme {
                MakeApp(app.database, app.mailRepository)
            }
        }
    }
}