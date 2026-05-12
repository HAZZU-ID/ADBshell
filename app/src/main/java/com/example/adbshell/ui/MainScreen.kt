package com.example.adbshell.ui

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.adbshell.ui.screens.LocalShellScreen
import com.example.adbshell.ui.screens.UsbScreen
import com.example.adbshell.ui.screens.WirelessScreen

data class TabItem(val label: String, val icon: ImageVector)

@Composable
fun MainScreen(activity: Activity) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        TabItem("Local", Icons.Filled.Terminal),
        TabItem("Wireless", Icons.Filled.Wifi),
        TabItem("USB", Icons.Filled.Usb),
    )
    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (selectedTab) {
                0 -> LocalShellScreen()
                1 -> WirelessScreen()
                2 -> UsbScreen(activity = activity)
            }
        }
    }
}
