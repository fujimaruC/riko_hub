package id.riko.hub

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import id.riko.hub.ui.RikoHub
import kotlinx.coroutines.launch
import java.io.IOException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            lifecycleScope.launch {
                try { (application as RikoHubApp).container.settings.recordAppLaunch() }
                catch (error: IOException) { Log.e("MainActivity", "Cannot record app launch", error) }
            }
        }
        enableEdgeToEdge()
        setContent {
            RikoHub { dark ->
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { dark },
                )
            }
        }
    }
}
