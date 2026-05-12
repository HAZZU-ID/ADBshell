package com.example.adbshell.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LocalShellScreen() {
    var command by remember { mutableStateOf("") }
    val lines = remember { mutableStateListOf<ShellLine>() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val quickCommands = listOf("id","whoami","uname -a","ls /sdcard","df -h","getprop ro.build.version.release","ip addr show wlan0")

    fun execute(cmd: String) {
        if (cmd.isBlank()) return
        command = ""
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val process = ProcessBuilder("/bin/sh", "-c", cmd).redirectErrorStream(true).start()
                    val output = process.inputStream.bufferedReader().readText().trimEnd()
                    val exitCode = process.waitFor()
                    ShellLine(cmd, output.ifEmpty { "(exit code $exitCode)" }, exitCode != 0)
                } catch (e: Exception) { ShellLine(cmd, "Error: ${e.message}", isError = true) }
            }
            lines.add(result)
            if (lines.isNotEmpty()) listState.animateScrollToItem(lines.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column {
            Text("Local Shell", style = MaterialTheme.typography.headlineSmall)
            Text("Commands run directly on this device", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            quickCommands.forEach { qc -> SuggestionChip(onClick = { execute(qc) }, label = { Text(qc, style = MaterialTheme.typography.labelSmall) }) }
        }
        TerminalOutput(lines = lines, listState = listState, modifier = Modifier.weight(1f), emptyHint = "$ Tap a chip or type a command...")
        CommandInput(value = command, onValueChange = { command = it }, onSend = { execute(command) }, onClear = { lines.clear() }, placeholder = "e.g. getprop ro.product.model")
    }
}
