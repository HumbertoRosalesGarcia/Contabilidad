const fs = require('fs');
let c = fs.readFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/ui/screens/FinanceScreen.kt','utf-8');
let lines = c.split('\n');

// Reemplazar líneas 833-855 (índice 832-854)
const newBlock = [
'                                            Column {',
'                                                var expanded by remember { mutableStateOf(false) }',
'                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {',
'                                                    androidx.compose.foundation.layout.Box(modifier = Modifier.weight(1f)) {',
'                                                        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {',
'                                                            Text("Cambiar Rol ?", fontSize = 12.sp)',
'                                                        }',
'                                                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {',
'                                                            DropdownMenuItem(text = { Text("Invitado") }, onClick = { roleToAssign = "INVITADO"; targetEmailToAssign = email; expanded = false })',
'                                                            DropdownMenuItem(text = { Text("Invitado Prueba") }, onClick = { roleToAssign = "INVITADO_PRUEBA"; targetEmailToAssign = email; expanded = false })',
'                                                            DropdownMenuItem(text = { Text("Básico (Madera)") }, onClick = { roleToAssign = "BÁSICO"; targetEmailToAssign = email; expanded = false })',
'                                                            DropdownMenuItem(text = { Text("Premium (Plata)") }, onClick = { roleToAssign = "PREMIUM"; targetEmailToAssign = email; expanded = false })',
'                                                            DropdownMenuItem(text = { Text("Gold (Oro)") }, onClick = { roleToAssign = "GOLD"; targetEmailToAssign = email; expanded = false })',
'                                                        }',
'                                                    }',
'                                                }',
'                                                Spacer(modifier = Modifier.height(8.dp))',
'                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {',
'                                                    OutlinedButton(onClick = { manageUser(email, "setRole", "INVITADO", 0L) }, modifier = Modifier.weight(1f)) { Text("Reset", fontSize = 12.sp) }',
'                                                    if (data.isBanned) {',
'                                                        Button(onClick = { manageUser(email, "unban") }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))) { Text("Desbloquear", fontSize = 12.sp) }',
'                                                    } else {',
'                                                        Button(onClick = { manageUser(email, "ban") }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Bloquear", fontSize = 12.sp) }',
'                                                    }',
'                                                }',
'                                            }'
];

// Replace lines 833-855 (0-indexed: 832-854)
lines.splice(832, 23, ...newBlock);

fs.writeFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/ui/screens/FinanceScreen.kt', lines.join('\n'), 'utf-8');
console.log('Admin panel replaced by line numbers');
