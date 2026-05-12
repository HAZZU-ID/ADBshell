package com.example.adbshell.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.adbshell.ui.theme.TerminalGreen
import dadb.AdbKeyPair
import dadb.Dadb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun WirelessScreen() {
    var ipAddress by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("5555") }
    var command by remember { mutableStateOf("") }
    var isConnected by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }
    val lines = remember { mutableStateListOf<ShellLine>() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val adbRef = remember { mutableStateOf<Dadb?>(null) }

    fun addSystem(msg: String, error: Boolean = false) { lines.add(ShellLine("", msg, isError = error, isSystem = true)) }

    fun connect() {
        val ip = ipAddress.trim(); val p = port.trim().toIntOrNull() ?: 5555
        if (ip.isEmpty()) return
        isConnecting = true
        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val dadb = Dadb.create(ip, p, AdbKeyPair.generate())
                    adbRef.value = dadb
                    withContext(Dispatchers.Main) { isConnected = true; isConnecting = false; addSystem("✓ Connected to $ip:$p") }
                } catch (e: Exception) { withContext(Dispatchers.Main) { isConnecting = false; addSystem("✗ Failed: ${e.message}", error = true) } }
            }
        }
    }

    fun disconnect() { scope.launch(Dispatchers.IO) { adbRef.value?.close(); adbRef.value = null; withContext(Dispatchers.Main) { isConnected = false; addSystem("Disconnected") } } }

    fun execute(cmd: String) {
        val dadb = adbRef.value ?: return; if (cmd.isBlank()) return; command = ""
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                try { val r = dadb.shell(cmd); ShellLine(cmd, r.output.trimEnd().ifEmpty { "(exit ${r.exitCode})" }, r.exitCode != 0) }
                catch (e: Exception) { ShellLine(cmd, "Error: ${e.message}", isError = true) }
            }
            lines.add(result); if (lines.isNotEmpty()) listState.animateScrollToItem(lines.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column {
            Text("Wireless ADB", style = MaterialTheme.typography.headlineSmall)
            Text("Connect over WiFi / TCP", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
        }
        if (!isConnected) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("💡"); Text("Enable: adb tcpip 5555  or  Settings → Developer → Wireless debugging", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = if (isConnected) Color(0xFF0A2A1A) else MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (isConnected) Icons.Filled.Wifi else Icons.Filled.WifiOff, null, tint = if (isConnected) TerminalGreen else MaterialTheme.colorScheme.outline)
                    Text(if (isConnected) "Connected to ${ipAddress.trim()}:${port.trim()}" else "Not connected", color = if (isConnected) TerminalGreen else MaterialTheme.colorScheme.onSurface)
                }
                if (!isConnected) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = ipAddress, onValueChange = { ipAddress = it }, label = { Text("IP Address") }, placeholder = { Text("192.168.1.x") }, modifier = Modifier.weight(2f), singleLine = true)
                        OutlinedTextField(value = port, onValueChange = { port = it }, label = { Text("Port") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                }
                Button(onClick = { if (isConnected) disconnect() else connect() }, modifier = Modifier.fillMaxWidth(), enabled = !isConnecting && (isConnected || ipAddress.isNotBlank()),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isConnected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)) {
                    if (isConnecting) { CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)); Text("Connecting...") }
                    else Text(if (isConnected) "Disconnect" else "Connect")
                }
            }
        }
        TerminalOutput(lines, listState, Modifier.weight(1f), "$ Connect to a device first...")
        CommandInput(command, { command = it }, { execute(command) }, { lines.clear() }, isConnected, "adb shell command...")
    }
}
