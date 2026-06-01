package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.BotViewModel

fun copyToClipboard(context: Context, text: String, label: String = "Text") {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "✅ Copiado al portapapeles", Toast.LENGTH_SHORT).show()
}

@Composable
fun SetupScreen(viewModel: BotViewModel) {
    val context = LocalContext.current
    val serverOnline by viewModel.serverOnline.collectAsState()

    val scriptStr = """pkg update -y && pkg upgrade -y
pkg install -y nodejs git
npm install -g npm
mkdir -p ~/botforge && cd ~/botforge
npm init -y
npm install @whiskeysockets/baileys @hapi/boom express cors qrcode-terminal
cat > server.js << 'SERVEREOF'
const express = require('express');
const cors = require('cors');
const { default: makeWASocket, useMultiFileAuthState, DisconnectReason } = require('@whiskeysockets/baileys');
const { Boom } = require('@hapi/boom');
const app = express();
app.use(cors());
app.use(express.json());

let sock = null;
let botStatus = 'disconnected';
let pairingCode = null;
let botConfig = { name: 'Mi Bot', prefix: '!', geminiKey: '', responses: [] };

async function startBot() {
  const { state, saveCreds } = await useMultiFileAuthState('auth_info');
  sock = makeWASocket({ auth: state, printQRInTerminal: false });
  sock.ev.on('creds.update', saveCreds);
  sock.ev.on('connection.update', ({ connection, lastDisconnect }) => {
    if (connection === 'open') { botStatus = 'connected'; pairingCode = null; }
    if (connection === 'close') {
      botStatus = 'disconnected';
      const shouldReconnect = (lastDisconnect?.error instanceof Boom)
        ? lastDisconnect.error.output?.statusCode !== DisconnectReason.loggedOut : true;
      if (shouldReconnect) setTimeout(startBot, 3000);
    }
  });
  sock.ev.on('messages.upsert', async ({ messages }) => {
    const msg = messages[0];
    if (!msg.message || msg.key.fromMe) return;
    const from = msg.key.remoteJid;
    const text = msg.message.conversation || msg.message.extendedTextMessage?.text || '';
    
    try {
        const fs = require('fs');
        if(fs.existsSync('./bot-logic.js')){
           require('./bot-logic.js')({ sock, msg, from, text, botConfig });
           return;
        }
    } catch(e) {}

    for (const r of botConfig.responses) {
      if (text.toLowerCase().includes(r.trigger.toLowerCase())) {
        await sock.sendMessage(from, { text: r.reply });
        return;
      }
    }
    if (botConfig.geminiKey && text.trim()) {
      try {
        const res = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${'$'}{botConfig.geminiKey}`, {
          method: 'POST', headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ contents: [{ parts: [{ text: text }] }] })
        });
        const data = await res.json();
        const reply = data.candidates?.[0]?.content?.parts?.[0]?.text;
        if (reply) await sock.sendMessage(from, { text: reply });
      } catch(e) { console.error('Gemini error:', e); }
    }
  });
  return sock;
}

app.get('/status', (req, res) => res.json({ status: botStatus, pairingCode }));
app.post('/connect', async (req, res) => {
  const { phone } = req.body;
  try {
    await startBot();
    setTimeout(async () => {
      if (sock && botStatus !== 'connected') {
        const code = await sock.requestPairingCode(phone.replace(/\D/g,''));
        pairingCode = code;
        res.json({ success: true, code });
      } else { res.json({ success: true, status: botStatus });}
    }, 3000);
  } catch(e) { res.json({ success: false, error: e.message }); }
});
app.post('/config', (req, res) => {
  botConfig = { ...botConfig, ...req.body };
  require('fs').writeFileSync('config.json', JSON.stringify(botConfig));
  res.json({ success: true });
});
app.get('/config', (req, res) => res.json(botConfig));
app.post('/update-bot', (req, res) => {
  const { code } = req.body;
  const wrappedCode = `module.exports = async function({sock, msg, from, text, botConfig}) {\n${'$'}{code}\n};`;
  require('fs').writeFileSync('bot-logic.js', wrappedCode);
  res.json({ success: true, message: 'Código actualizado. Reinicia el bot para aplicar cambios.' });
});
app.get('/get-code', (req, res) => {
    try {
        const code = require('fs').readFileSync('bot-logic.js', 'utf8');
        res.send(code);
    } catch(e) { res.send("// Sin código aún"); }
});
app.listen(3000, () => console.log('🟢 BotForge Server corriendo en puerto 3000'));
SERVEREOF
echo "✅ Instalación completada. Ejecuta: node server.js"""

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!serverOnline) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    Text("Termux Offline. Inicia el servidor.", color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Paso 1: Instalar Termux", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Instala Termux desde F-Droid.")
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Paso 2: Instalar servidor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().background(Color.Black, RoundedCornerShape(8.dp)).padding(12.dp)) {
                    Text("curl -sL https://raw.githubusercontent.com/botforge-app/setup/main/install.sh | bash", color = Color.Green, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { copyToClipboard(context, "curl -sL https://raw.githubusercontent.com/botforge-app/setup/main/install.sh | bash") }, modifier = Modifier.fillMaxWidth()) {
                    Text("📋 Copiar Comando")
                }
                Text("O usar instalación manual:", modifier = Modifier.padding(top=8.dp))
                Button(onClick = { copyToClipboard(context, scriptStr) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                    Text("📋 Copiar Instalación Manual")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Paso 3: Iniciar Servidor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().background(Color.Black, RoundedCornerShape(8.dp)).padding(12.dp)) {
                    Text("cd ~/botforge && node server.js", color = Color.Green, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { copyToClipboard(context, "cd ~/botforge && node server.js") }, modifier = Modifier.fillMaxWidth()) {
                    Text("📋 Copiar Comando")
                }
            }
        }
    }
}
