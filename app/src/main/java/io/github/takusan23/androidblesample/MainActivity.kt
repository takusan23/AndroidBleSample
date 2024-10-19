package io.github.takusan23.androidblesample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.takusan23.androidblesample.ui.screen.BleScanConnectScreen
import io.github.takusan23.androidblesample.ui.screen.GattServerScreen
import io.github.takusan23.androidblesample.ui.theme.AndroidBleSampleTheme

/** 画面遷移一覧 */
enum class NavigationPaths(val path: String) {
    BleScanConnect("ble_find"),
    GattServer("gatt_server");

    // TODO マテリアルアイコンを入れる
    val icon: ImageVector
        get() = when (this) {
            BleScanConnect -> Icons.Default.Search
            GattServer -> Icons.Default.Share
        }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroidBleSampleTheme {
                MainScreen()
            }
        }
    }
}

@Composable
private fun MainScreen() {
    val navController = rememberNavController()
    val currentPath = navController.currentBackStackEntryAsState()

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationPaths.entries.forEach { nav ->
                    NavigationBarItem(
                        selected = currentPath.value?.destination?.route == nav.path,
                        onClick = { navController.navigate(nav.path) },
                        icon = { Icon(imageVector = nav.icon, contentDescription = null) },
                        label = { Text(text = nav.name) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(WindowInsets.statusBars)
                .consumeWindowInsets(WindowInsets.navigationBars),
            navController = navController,
            startDestination = NavigationPaths.BleScanConnect.path
        ) {
            composable(NavigationPaths.BleScanConnect.path) {
                BleScanConnectScreen()
            }
            composable(NavigationPaths.GattServer.path) {
                GattServerScreen()
            }
        }
    }
}