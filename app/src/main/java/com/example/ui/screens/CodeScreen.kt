package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.api.ApiClient
import com.example.viewmodel.BotViewModel
import kotlinx.coroutines.launch

@Composable
fun CodeScreen(viewModel: BotViewModel) {
    var sourceCode by remember { mutableStateOf("// Cargando código...") }
    val blockScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        try {
            sourceCode = ApiClient.termuxApi.getCode().string()
        } catch (e: Exception) {
            sourceCode = "// No se pudo cargar el código: \${e.message}"
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Archivo: bot-logic.js", style = MaterialTheme.typography.titleMedium)
            Button(onClick = { copyToClipboard(context, sourceCode, "Código base") }) {
                Text("📋 Copiar")
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        Box(
            modifier = Modifier.fillMaxSize()
                .background(Color(0xFF1E1E1E))
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = sourceCode,
                color = Color(0xFF4EC9B0),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
        }
    }
}
