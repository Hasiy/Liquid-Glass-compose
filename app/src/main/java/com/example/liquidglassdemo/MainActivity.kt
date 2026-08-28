package com.example.liquidglassdemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.liquidglassdemo.ui.DynamicLightTabBarDemoScreen
import com.example.liquidglassdemo.ui.theme.LiquidGlassDemoTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LiquidGlassDemoTheme {
                DynamicLightTabBarDemoScreen()
            }
        }
    }
}