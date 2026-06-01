package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class BotViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("botforge_prefs", Context.MODE_PRIVATE)

    private val _serverOnline = MutableStateFlow(false)
    val serverOnline: StateFlow<Boolean> = _serverOnline.asStateFlow()

    private val _botStatus = MutableStateFlow("Desconocido")
    val botStatus: StateFlow<String> = _botStatus.asStateFlow()

    private val _pairingCode = MutableStateFlow<String?>(null)
    val pairingCode: StateFlow<String?> = _pairingCode.asStateFlow()

    // Config state
    val phone = MutableStateFlow(prefs.getString("bf_phone", "") ?: "")
    val botName = MutableStateFlow(prefs.getString("bf_botname", "Mi Bot") ?: "Mi Bot")
    val prefix = MutableStateFlow(prefs.getString("bf_prefix", "!") ?: "!")
    val geminiKey = MutableStateFlow(prefs.getString("bf_gemini", "") ?: "")

    private val _quickReplies = MutableStateFlow<List<QuickReply>>(emptyList())
    val quickReplies: StateFlow<List<QuickReply>> = _quickReplies.asStateFlow()

    private val _chatHistory = MutableStateFlow<List<Content>>(emptyList())
    val chatHistory: StateFlow<List<Content>> = _chatHistory.asStateFlow()

    private var pollingJob: Job? = null

    init {
        startPolling()
    }

    fun saveConfigLocally() {
        prefs.edit()
            .putString("bf_phone", phone.value)
            .putString("bf_botname", botName.value)
            .putString("bf_prefix", prefix.value)
            .putString("bf_gemini", geminiKey.value)
            .apply()
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                checkServerStatus()
                delay(3000)
            }
        }
    }

    suspend fun checkServerStatus() {
        try {
            val response = ApiClient.termuxApi.getStatus()
            _serverOnline.value = true
            _botStatus.value = response.status
            _pairingCode.value = response.pairingCode
        } catch (e: Exception) {
            _serverOnline.value = false
            _botStatus.value = "Offline"
            _pairingCode.value = null
        }
    }

    fun connectWhatsApp(onResult: (Boolean, String) -> Unit) {
        if (!_serverOnline.value) {
            onResult(false, "Servidor no activo, revisa Termux")
            return
        }
        saveConfigLocally()
        viewModelScope.launch {
            try {
                // Update config
                ApiClient.termuxApi.updateConfig(
                    ConfigUpdate(botName.value, prefix.value, geminiKey.value, _quickReplies.value)
                )

                // Request connect
                val response = ApiClient.termuxApi.connect(ConnectRequest(phone.value))
                if (response.success) {
                    if (response.code != null) {
                        _pairingCode.value = response.code
                        onResult(true, "Código listo")
                    } else if (response.status == "connected") {
                        onResult(true, "Bot conectado exitosamente")
                    } else {
                        onResult(true, "Iniciando...")
                    }
                } else {
                    onResult(false, response.error ?: "Error desconocido")
                }
            } catch (e: Exception) {
                onResult(false, "Error de red: \${e.message}")
            }
        }
    }

    fun addQuickReply(trigger: String, reply: String) {
        val current = _quickReplies.value.toMutableList()
        current.add(QuickReply(trigger, reply))
        _quickReplies.value = current
        syncConfig()
    }

    fun removeQuickReply(index: Int) {
        val current = _quickReplies.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _quickReplies.value = current
            syncConfig()
        }
    }

    private fun syncConfig() {
        viewModelScope.launch {
            try {
                ApiClient.termuxApi.updateConfig(
                    ConfigUpdate(botName.value, prefix.value, geminiKey.value, _quickReplies.value)
                )
            } catch (e: Exception) {
                // Ignore sync errors
            }
        }
    }

    fun sendChat(message: String, onResult: (Boolean, String) -> Unit) {
        val currentHistory = _chatHistory.value.toMutableList()
        currentHistory.add(Content(role = "user", parts = listOf(Part(text = message))))
        _chatHistory.value = currentHistory

        if (geminiKey.value.isEmpty()) {
            onResult(false, "Falta API Key de Gemini")
            return
        }

        viewModelScope.launch {
            try {
                val systemInstruction = PartWrapper(
                    parts = listOf(Part("Eres un experto en bots de WhatsApp con Baileys (Node.js). Ayudas al usuario a construir su bot paso a paso. Responde SIEMPRE en español. Cuando generes código, usa bloques ```javascript. El servidor ya está corriendo en Express en puerto 3000. La lógica del bot va dentro del evento messages.upsert. Estructura base del handler de mensajes (ASUME VARIABLES: sock, msg, from, text, botConfig). Genera la logica para el handler. Al final del codigo di: ✅ Haz clic en 'Aplicar código' para activar esto en tu bot."))
                )

                val request = GeminiRequest(
                    systemInstruction = systemInstruction,
                    contents = _chatHistory.value
                )

                val response = ApiClient.geminiApi.generateContent(geminiKey.value, request)

                if (response.error != null) {
                    onResult(false, response.error.message)
                } else {
                    val reply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    if (reply != null) {
                        val newHistory = _chatHistory.value.toMutableList()
                        newHistory.add(Content(role = "model", parts = listOf(Part(text = reply))))
                        _chatHistory.value = newHistory
                        onResult(true, "")
                    } else {
                        onResult(false, "Respuesta vacía de la IA")
                    }
                }

            } catch (e: Exception) {
                // pop user message on failure
                val reverted = _chatHistory.value.toMutableList()
                reverted.removeLastOrNull()
                _chatHistory.value = reverted
                onResult(false, e.message ?: "Error de red")
            }
        }
    }

    fun applyCode(code: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = ApiClient.termuxApi.updateBot(UpdateBotRequest(code))
                if (response.success) {
                    onResult(true, response.message ?: "Código aplicado")
                } else {
                    onResult(false, response.error ?: "Error al aplicar")
                }
            } catch (e: Exception) {
                onResult(false, "Error: \${e.message}")
            }
        }
    }
}
