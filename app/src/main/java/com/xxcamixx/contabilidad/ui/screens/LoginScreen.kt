package com.xxcamixx.contabilidad.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.xxcamixx.contabilidad.network.RetrofitInstance
import com.xxcamixx.contabilidad.model.UserManageRequest
import com.xxcamixx.contabilidad.model.UserSyncRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

@SuppressLint("ContextGetResource", "DiscouragedApi")
@Composable
fun LoginScreen(onLoginSuccess: (String, String, String, Long, Long) -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }

    // SOLUCIÓN ERROR 1: Extraemos el valor del stringResource fuera del onClick para respetar el entorno Composable
    val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
    val webClientId = if (resId != 0) androidx.compose.ui.res.stringResource(id = resId) else ""

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.AccountCircle, contentDescription = "Login", modifier = Modifier.size(100.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(24.dp))
            Text("Acceso a Billetera", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Sincronización segura en la nube", fontSize = 14.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(32.dp))

            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else {
                Button(
                    onClick = {
                        isLoading = true
                        coroutineScope.launch {
                            try {
                                val googleIdOption = GetGoogleIdOption.Builder()
                                    .setFilterByAuthorizedAccounts(false)
                                    .setServerClientId(webClientId)
                                    .setAutoSelectEnabled(true)
                                    .build()
                                val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
                                val result = credentialManager.getCredential(request = request, context = context)
                                val credential = result.credential

                                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                    val displayName = googleIdTokenCredential.displayName ?: "Usuario"
                                    val userId = googleIdTokenCredential.id
                                    val isSuperAdmin = userId.lowercase(Locale.getDefault()) == "zonacami77777@gmail.com"

                                    try {
                                        val response = RetrofitInstance.api.syncUser(UserSyncRequest(email = userId, name = displayName))
                                        val finalRole = if (isSuperAdmin) "ADMIN" else (response.role ?: "INVITADO")
                                        if (response.isBanned && !isSuperAdmin) {
                                            isLoading = false
                                            Toast.makeText(context, "🚫 Tu cuenta está bloqueada o vencida.", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Usted se encuentra bajo el PLAN $finalRole, Bienvenido", Toast.LENGTH_LONG).show()
                                            onLoginSuccess(displayName, userId, finalRole, response.consumedSeconds, response.planDuration)
                                        }
                                    } catch (_: Exception) {
                                        val fallbackRole = if (isSuperAdmin) "ADMIN" else "BÁSICO"
                                        Toast.makeText(context, "Modo sin conexión activado. Usted se encuentra bajo el PLAN $fallbackRole, Bienvenido", Toast.LENGTH_LONG).show()
                                        onLoginSuccess(displayName, userId, fallbackRole, 0L, 2592000L)
                                    }
                                } else {
                                    isLoading = false
                                    Toast.makeText(context, "Error al procesar la credencial", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                isLoading = false
                                Toast.makeText(context, "Inicio de sesión cancelado o fallido", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(0.8f).height(50.dp)
                ) {
                    Text("Iniciar sesión con Google", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        try {
                            val addAccountIntent = Intent(Settings.ACTION_ADD_ACCOUNT).apply { putExtra(Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.google")) }
                            context.startActivity(addAccountIntent)
                        } catch (_: Exception) {
                            Toast.makeText(context, "No se pudo abrir la configuración", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(0.8f).height(50.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Añadir cuenta", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Añadir cuenta nueva", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(24.dp))
                Divider(color = Color.Gray.copy(alpha = 0.2f), modifier = Modifier.fillMaxWidth(0.8f))
                Spacer(modifier = Modifier.height(16.dp))

                // BOTÓN DE INVITADO GOLD
                // BOTÓN DE INVITADO GOLD
                Button(
                    onClick = {
                        isLoading = true
                        coroutineScope.launch(Dispatchers.IO) {
                            val authPrefs = context.getSharedPreferences("GlobalAuthPrefs", Context.MODE_PRIVATE)
                            var guestId = authPrefs.getString("trialGuestId", null)
                            if (guestId == null) {
                                // NUEVO: Vinculamos la prueba al identificador físico del hardware del teléfono.
                                // Aunque desinstale la app o borre datos, será el mismo usuario y el tiempo no se reiniciará.
                                val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: UUID.randomUUID().toString().substring(0, 8)
                                guestId = "dispositivo_$androidId"
                                authPrefs.edit().putString("trialGuestId", guestId).apply()
                            }
                            try {
                                RetrofitInstance.api.syncUser(UserSyncRequest(email = guestId, name = "Usuario de Prueba"))
                                RetrofitInstance.api.manageUser(UserManageRequest(guestId, "setRole", "Invitado-Gold", 86400L))
                                launch(Dispatchers.Main) {
                                    Toast.makeText(context, "Modo Invitado-Gold Activado ⏳", Toast.LENGTH_LONG).show()
                                    onLoginSuccess("Usuario de Prueba", guestId, "Invitado-Gold", 0L, 86400L)
                                }
                            } catch (e: Exception) {
                                launch(Dispatchers.Main) {
                                    Toast.makeText(context, "Modo Invitado-Gold Local (Sin conexión) ⏳", Toast.LENGTH_LONG).show()
                                    onLoginSuccess("Usuario de Prueba", guestId, "Invitado-Gold", 0L, 86400L)
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(0.8f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color.Black)
                ) {
                    Text("Prueba 1 día gratis 🌟", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_SYNC_SETTINGS)
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            Toast.makeText(context, "No se pudo abrir la configuración", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Gestionar o Eliminar cuentas del dispositivo", color = Color.Gray, textDecoration = TextDecoration.Underline, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
