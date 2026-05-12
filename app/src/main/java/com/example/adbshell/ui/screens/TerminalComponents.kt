package com.example.adbshell.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adbshell.ui.theme.TerminalGreen
import com.example.adbshell.ui.theme.TerminalRed
import com.example.adbshell.ui.theme.TextMuted

data class ShellLine(
    val command: String,
    val output: String,
    val isError: Boolean = false,
    val isSystem: Boolean = false
)

@Composable
fun TerminalOutput(lines: List<ShellLine>, listState: LazyListState, modifier: Modifier = Modifier, emptyHint: String = "$ Type a command below...") {
    Card(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117))) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (lines.isEmpty()) {
                item { Text(emptyHint, fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = TextMuted) }
            }
            items(lines) { line ->
                Column {
                    if (line.command.isNotEmpty() && !line.isSystem) {
                        Text("$ ${line.command}", fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = TerminalGreen)
                    }
                    Text(line.output, fontFamily = FontFamily.Monospace, fontSize = 12.sp,
                        color = when { line.isError -> TerminalRed; line.isSystem -> Color(0xFFFFD93D); else -> Color(0xFFE6EDF3) })
                }
            }
        }
    }
}

@Composable
fun CommandInput(value: String, onValueChange: (String) -> Unit, onSend: () -> Unit, onClear: () -> Unit, enabled: Boolean = true, placeholder: String = "Enter command...") {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = value, onValueChange = onValueChange, modifier = Modifier.weight(1f),
            placeholder = { Text(placeholder, fontFamily = FontFamily.Monospace, fontSize = 13.sp) },
            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
            singleLine = true, enabled = enabled,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() })
        )
        IconButton(onClick = onSend, enabled = enabled && value.isNotBlank()) {
            Icon(Icons.Filled.Send, contentDescription = "Run",
                tint = if (enabled && value.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
        }
        IconButton(onClick = onClear) {
            Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
    }
}
