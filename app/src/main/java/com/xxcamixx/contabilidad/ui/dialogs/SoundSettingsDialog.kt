package com.xxcamixx.contabilidad.ui.dialogs

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.xxcamixx.contabilidad.util.AppSounds

@Composable
fun SoundSettingsDialog(personalSoundUri: String?, storeSoundUri: String?, touchSoundUri: String?, isVoiceEnabled: Boolean, onDismiss: () -> Unit, onSelectPersonal: (String) -> Unit, onSelectStore: (String) -> Unit, onSelectTouch: (String) -> Unit, onVoiceToggle: (Boolean) -> Unit) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION).toFloat() }
    var currentVolume by remember { mutableStateOf(audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION).toFloat()) }
    var targetForPicker by remember { mutableStateOf("") }
    var selectedSoundTab by remember { mutableStateOf(0) }
    val tabs = listOf("General ⚙️", "Personal \uD83D\uDC64", "Tienda \uD83C\uDFEA")

    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> if (uri != null) { try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (e: Exception) {}; when (targetForPicker) { "PERSONAL" -> onSelectPersonal(uri.toString()); "STORE" -> onSelectStore(uri.toString()); "TOUCH" -> onSelectTouch(uri.toString()) } } }
    val ringtonePicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result -> if (result.resultCode == android.app.Activity.RESULT_OK) { val uri: Uri? = result.data?.getParcelableExtra(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI); if (uri != null) { when (targetForPicker) { "PERSONAL" -> onSelectPersonal(uri.toString()); "STORE" -> onSelectStore(uri.toString()); "TOUCH" -> onSelectTouch(uri.toString()) } } } }

    fun openRingtonePicker(target: String) { targetForPicker = target; val intent = Intent(android.media.RingtoneManager.ACTION_RINGTONE_PICKER).apply { putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TYPE, android.media.RingtoneManager.TYPE_NOTIFICATION); putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true); putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true) }; ringtonePicker.launch(intent) }
    fun openAudioPicker(target: String) { targetForPicker = target; audioPicker.launch("audio/*") }

    AlertDialog(
        onDismissRequest = { }, properties = DialogProperties(dismissOnClickOutside = false),
        title = { Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Configuración de Sonido \uD83C\uDFB5", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center); IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Close, "Cerrar") } } },
        containerColor = MaterialTheme.colorScheme.surface,
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                TabRow(selectedTabIndex = selectedSoundTab, containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.primary) { tabs.forEachIndexed { index, title -> Tab(selected = selectedSoundTab == index, onClick = { selectedSoundTab = index }, text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1) }) } }
                Spacer(modifier = Modifier.height(16.dp))
                Crossfade(targetState = selectedSoundTab, label = "SoundTabs") { tab ->
                    Column(modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        when (tab) {
                            0 -> {
                                Text("Ajusta el volumen (escucharás un tono al soltar).", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center); Spacer(modifier = Modifier.height(16.dp))
                                Text("Volumen del Sistema", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) { Text("\uD83D\uDD09", fontSize = 20.sp); Slider(value = currentVolume, onValueChange = { currentVolume = it; audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, it.toInt(), 0) }, onValueChangeFinished = { AppSounds.play(context, "") }, valueRange = 0f..maxVolume, modifier = Modifier.weight(1f).padding(horizontal = 8.dp)); Text("\uD83D\uDD0A", fontSize = 20.sp) }
                                Text("${(currentVolume / maxVolume * 100).toInt()}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold); Spacer(modifier = Modifier.height(24.dp)); Divider(color = Color.Gray.copy(alpha = 0.2f)); Spacer(modifier = Modifier.height(16.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onVoiceToggle(!isVoiceEnabled) }.padding(vertical = 8.dp)) { Switch(checked = isVoiceEnabled, onCheckedChange = onVoiceToggle); Spacer(modifier = Modifier.width(12.dp)); Column { Text("Asistente de Voz \uD83C\uDF99️", fontWeight = FontWeight.Bold); Text("Leer notificaciones en voz alta", fontSize = 12.sp, color = Color.Gray) } }
                                Spacer(modifier = Modifier.height(16.dp)); Divider(color = Color.Gray.copy(alpha = 0.2f)); Spacer(modifier = Modifier.height(16.dp))
                                Text("\uD83D\uDC46 Sonido de Toques (Acciones en App)", fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center); Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { OutlinedButton(onClick = { onSelectTouch("") }, modifier = Modifier.weight(1f).padding(end=4.dp), contentPadding = PaddingValues(0.dp)) { Text("Por Defecto", fontSize = 11.sp) }; Button(onClick = { openRingtonePicker("TOUCH") }, modifier = Modifier.weight(1f).padding(horizontal=2.dp), contentPadding = PaddingValues(0.dp)) { Text("Tono", fontSize = 11.sp) }; Button(onClick = { openAudioPicker("TOUCH") }, modifier = Modifier.weight(1f).padding(start=4.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary), contentPadding = PaddingValues(0.dp)) { Text("Audio", fontSize = 11.sp) } }
                            }
                            1 -> {
                                Text("Notificaciones de Agenda Personal", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center); Spacer(modifier = Modifier.height(16.dp)); Text("\uD83D\uDD35 Recordatorios de Deudas", fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center); Spacer(modifier = Modifier.height(16.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { OutlinedButton(onClick = { onSelectPersonal("") }, modifier = Modifier.weight(1f).padding(end=4.dp), contentPadding = PaddingValues(0.dp)) { Text("Por Defecto", fontSize = 11.sp) }; Button(onClick = { openRingtonePicker("PERSONAL") }, modifier = Modifier.weight(1f).padding(horizontal=2.dp), contentPadding = PaddingValues(0.dp)) { Text("Tono", fontSize = 11.sp) }; Button(onClick = { openAudioPicker("PERSONAL") }, modifier = Modifier.weight(1f).padding(start=4.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary), contentPadding = PaddingValues(0.dp)) { Text("Audio", fontSize = 11.sp) } }
                            }
                            2 -> {
                                Text("Notificaciones de la Tienda", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center); Spacer(modifier = Modifier.height(16.dp)); Text("\uD83C\uDFEA Cobros, Fiadores y Stock", fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center); Spacer(modifier = Modifier.height(16.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { OutlinedButton(onClick = { onSelectStore("") }, modifier = Modifier.weight(1f).padding(end=4.dp), contentPadding = PaddingValues(0.dp)) { Text("Por Defecto", fontSize = 11.sp) }; Button(onClick = { openRingtonePicker("STORE") }, modifier = Modifier.weight(1f).padding(horizontal=2.dp), contentPadding = PaddingValues(0.dp)) { Text("Tono", fontSize = 11.sp) }; Button(onClick = { openAudioPicker("STORE") }, modifier = Modifier.weight(1f).padding(start=4.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary), contentPadding = PaddingValues(0.dp)) { Text("Audio", fontSize = 11.sp) } }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { }, dismissButton = { }
    )
}
