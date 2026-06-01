package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.BotViewModel

@Composable
fun ConnectScreen(viewModel: BotViewModel) {
    val phone by viewModel.phone.collectAsState()
    val botName by viewModel.botName.collectAsState()
    val prefix by viewModel.prefix.collectAsState()
    val geminiKey by viewModel.geminiKey.collectAsState()
    
    val pairingCode by viewModel.pairingCode.collectAsState()
    val serverOnline by viewModel.serverOnline.collectAsState()
    val botStatus by viewModel.botStatus.collectAsState()

    var isConnecting by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Conectar con WhatsApp", style = MaterialTheme.typography.titleLarge)
                
                OutlinedTextField(
                    value = phone,
                    onValueChange = { viewModel.phone.value = it },
                    label = { Text("Número de WhatsApp (+52...)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = botName,
                    onValueChange = { viewModel.botName.value = it },
                    label = { Text("Nombre del bot") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = prefix,
                    onValueChange = { viewModel.prefix.value = it },
                    label = { Text("Prefijo (!)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = geminiKey,
                    onValueChange = { viewModel.geminiKey.value = it },
                    label = { Text("API Key de Gemini") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        isConnecting = true
                        viewModel.connectWhatsApp { success, message ->
                            isConnecting = false
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isConnecting && serverOnline
                ) {
                    Text(if (isConnecting) "Conectando..." else "🔗 Conectar WhatsApp")
                }
            }
        }

        if (pairingCode != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tu código de emparejamiento:", fontWeight = FontWeight.Bold)
                    Text(
                        text = pairingCode!!.chunked(4).joinToString(" - "),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 24.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("1. Abre WhatsApp\n2. Dispositivos vinculados\n3. Vincular con número de teléfono", fontSize = 14.sp)
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Estado Actual", fontWeight = FontWeight.Bold)
                Text(
                    text = if (botStatus == "connected") "✅ Conectado" else "● $botStatus",
                    color = if (botStatus == "connected") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
