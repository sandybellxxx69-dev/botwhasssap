package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.viewmodel.BotViewModel

@Composable
fun DesignScreen(viewModel: BotViewModel) {
    val quickReplies by viewModel.quickReplies.collectAsState()
    val chatHistory by viewModel.chatHistory.collectAsState()

    var showReplyDialog by remember { mutableStateOf(false) }
    var triggerText by remember { mutableStateOf("") }
    var replyText by remember { mutableStateOf("") }

    var chatMessage by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    val context = LocalContext.current

    if (showReplyDialog) {
        AlertDialog(
            onDismissRequest = { showReplyDialog = false },
            title = { Text("Nueva Respuesta Rápida") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = triggerText, onValueChange = { triggerText = it }, label = { Text("Palabra clave") })
                    OutlinedTextField(value = replyText, onValueChange = { replyText = it }, label = { Text("Respuesta") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (triggerText.isNotBlank() && replyText.isNotBlank()) {
                        viewModel.addQuickReply(triggerText, replyText)
                        triggerText = ""
                        replyText = ""
                        showReplyDialog = false
                    }
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { showReplyDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Respuestas Rápidas", style = MaterialTheme.typography.titleMedium)
        Button(onClick = { showReplyDialog = true }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text("+ Agregar")
        }

        LazyColumn(modifier = Modifier.weight(0.3f)) {
            itemsIndexed(quickReplies) { index, reply ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Si dicen: \${reply.trigger}", style = MaterialTheme.typography.bodyMedium)
                            Text("Bot responde: \${reply.reply}", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { viewModel.removeQuickReply(index) }) {
                            Icon(Icons.Default.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        Divider(Modifier.padding(vertical = 8.dp))

        Text("Asistente IA (Generador de Código)", style = MaterialTheme.typography.titleMedium)
        
        LazyColumn(modifier = Modifier.weight(0.6f), reverseLayout = true) {
            items(chatHistory.reversed().size) { idx ->
                val item = chatHistory.reversed()[idx]
                val isUser = item.role == "user"
                Card(
                    modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(0.9f)
                        .let { if (isUser) it.padding(start = 32.dp) else it.padding(end = 32.dp) },
                    colors = CardDefaults.cardColors(containerColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(8.dp)) {
                        Text(item.parts.firstOrNull()?.text ?: "")
                        if (!isUser && (item.parts.firstOrNull()?.text?.contains("```javascript") == true)) {
                            Button(onClick = {
                                val code = item.parts.first().text.substringAfter("```javascript").substringBefore("```").trim()
                                viewModel.applyCode(code) { success, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }) { Text("✨ Aplicar Código") }
                        }
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = chatMessage,
                onValueChange = { chatMessage = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ej. El bot debe decir hola...") }
            )
            IconButton(
                onClick = {
                    if (chatMessage.isNotBlank()) {
                        isSending = true
                        val msg = chatMessage
                        chatMessage = ""
                        viewModel.sendChat(msg) { _, errorMsg ->
                            isSending = false
                            if (errorMsg.isNotEmpty()) Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.size(56.dp),
                enabled = !isSending
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, "Enviar")
            }
        }
    }
}
