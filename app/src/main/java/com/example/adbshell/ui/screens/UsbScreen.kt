package com.example.adbshell.ui.screens

import android.app.Activity
import android.hardware.usb.UsbDevice
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.adbshell.adb.UsbAdbManager
import com.example.adbshell.ui.theme.TerminalGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun UsbScreen(activity: Activity) {
    val manager = remember { UsbAdbManager(activity) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var devices by remember { mutableStateOf<List<UsbDevice>>(emptyList()) }
    var selected by remember { mutableStateOf<UsbDevice?>(null) }
    var isConnected by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf("Scan for USB devices") }
    var command by remember { mutableStateOf("") }
    val lines = remember { mutableStateListOf<ShellLine>() }

    fun addSystem(msg: String, error: Boolean = false) { lines.add(ShellLine("", msg, isError = error, isSystem = true)) }
    fun scan() { devices = manager.findAdbDevices(); statusMsg = if (devices.isEmpty()) "No ADB devices found" else "${devices.size} device(s) found" }
    fun connect(device: UsbDevice) {
        scope.launch {
            addSystem("Requesting USB permission...")
            if (!manager.requestPermission(device)) { addSystem("Permission denied", true); return@launch }
            addSystem("Opening ADB connection...")
            val ok = withContext(Dispatchers.IO) { manager.open(device) }
            if (ok) { isConnected = true; selected = device; addSystem("✓ Connected to ${device.productName ?: device.deviceName}") }
            else addSystem("✗ ADB handshake failed. Tap Allow on device.", true)
        }
    }
    fun disconnect() { manager.close(); isConnected = false; selected = null; addSystem("Disconnected") }
    fun execute(cmd: String) {
        if (cmd.isBlank() || !isConnected) return; command = ""
        scope.launch {
            val r = withContext(Dispatchers.IO) { manager.shell(cmd) }
            lines.add(ShellLine(cmd, r.output.ifEmpty { "(no output)" }, r.isError))
            if (lines.isNotEmpty()) listState.animateScrollToItem(lines.size - 1)
        }
    }

    LaunchedEffect(Unit) { scan() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column { Text("USB Debugging", style = MaterialTheme.typography.headlineSmall); Text("Connect via USB cable", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)) }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("USB Devices", style = MaterialTheme.typography.titleSmall)
                    IconButton(onClick = { scan() }, enabled = !isConnected) { Icon(Icons.Filled.Refresh, null) }
                }
                Text(statusMsg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                if (devices.isNotEmpty() && !isConnected) devices.forEach { device ->
                    OutlinedButton(onClick = { connect(device) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Usb, null, Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text(device.productName ?: device.deviceName)
                    }
                }
                if (isConnected) Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.Usb, null, tint = TerminalGreen, modifier = Modifier.size(18.dp))
                        Text(selected?.productName ?: "Device", color = TerminalGreen)
                    }
                    TextButton(onClick = { disconnect() }) { Text("Disconnect", color = MaterialTheme.colorScheme.error) }
                }
                if (devices.isEmpty() && !isConnected) Text("💡 Enable USB Debugging in Developer Options, then plug in USB cable.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }
        TerminalOutput(lines, listState, Modifier.weight(1f), "$ Connect a USB device above...")
        CommandInput(command, { command = it }, { execute(command) }, { lines.clear() }, isConnected, "shell command...")
    }
}
