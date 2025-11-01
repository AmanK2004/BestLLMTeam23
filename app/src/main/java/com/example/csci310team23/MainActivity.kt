package com.example.csci310team23

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.csci310team23.ui.theme.CSCI310Team23Theme
import com.example.csci310team23.ui.AppRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CSCI310Team23Theme {
                AppRoot()
            }
        }
    }
}
