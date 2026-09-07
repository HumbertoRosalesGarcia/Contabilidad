const fs = require('fs');
let c = fs.readFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/ui/screens/FinanceScreen.kt','utf-8');

const oldAdmin = `                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                var expanded by remember { mutableStateOf(false) }
                                                androidx.compose.foundation.layout.Box {
                                                    Button(onClick = { expanded = true }) {
                                                        Text("Cambiar Rol", fontSize = 11.sp)
                                                    }
                                                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                                        DropdownMenuItem(text = { Text("Invitado") }, onClick = { roleToAssign = "INVITADO"; targetEmailToAssign = email; expanded = false })
                                                        DropdownMenuItem(text = { Text("Invitado Prueba") }, onClick = { roleToAssign = "INVITADO_PRUEBA"; targetEmailToAssign = email; expanded = false })`;

const newAdmin = `                                            Column {
                                                var expanded by remember { mutableStateOf(false) }
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    androidx.compose.foundation.layout.Box(modifier = Modifier.weight(1f)) {
                                                        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                                                            Text("Cambiar Rol \u25BC", fontSize = 12.sp)
                                                        }
                                                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                                            DropdownMenuItem(text = { Text("Invitado") }, onClick = { roleToAssign = "INVITADO"; targetEmailToAssign = email; expanded = false })
                                                            DropdownMenuItem(text = { Text("Invitado Prueba") }, onClick = { roleToAssign = "INVITADO_PRUEBA"; targetEmailToAssign = email; expanded = false })`;

c = c.replace(oldAdmin, newAdmin);

// Now fix the closing of that section - replace the old Row of Reset/Ban buttons
const oldBtns = `                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Button(onClick = { manageUser(email, "setRole", "INVITADO", 0L) }, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) { Text("Reset", fontSize = 11.sp) }
                                                    if (data.isBanned) {
                                                        Button(onClick = { manageUser(email, "unban") }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))) { Text("Desbloq", fontSize = 11.sp) }
                                                    } else {
                                                        Button(onClick = { manageUser(email, "ban") }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Bloquear", fontSize = 11.sp) }
                                                    }
                                                }
                                            }`;

const newBtns = `                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    OutlinedButton(onClick = { manageUser(email, "setRole", "INVITADO", 0L) }, modifier = Modifier.weight(1f)) { Text("Reset", fontSize = 12.sp) }
                                                    if (data.isBanned) {
                                                        Button(onClick = { manageUser(email, "unban") }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))) { Text("Desbloquear", fontSize = 12.sp) }
                                                    } else {
                                                        Button(onClick = { manageUser(email, "ban") }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Bloquear", fontSize = 12.sp) }
                                                    }
                                                }
                                            }`;

c = c.replace(oldBtns, newBtns);

fs.writeFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/ui/screens/FinanceScreen.kt', c, 'utf-8');
console.log('Admin panel layout fixed');
