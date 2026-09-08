package com.xxcamixx.contabilidad.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xxcamixx.contabilidad.util.formatCOP

@Composable
fun DashboardCard(balance: Double, income: Double, expense: Double, cashExpense: Double, digitalExpense: Double, onCashClick: () -> Unit, onDigitalClick: () -> Unit, onIncomeClick: () -> Unit, onExpenseClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(defaultElevation = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Saldo Total", color = Color.Gray, fontSize = 16.sp)
            Text(text = formatCOP(balance), fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = if (balance >= 0) Color(0xFF2196F3) else Color(0xFFE53935))
            Spacer(modifier = Modifier.height(16.dp))

            Text("Desglose de Gastos", fontSize=12.sp, color=Color.Gray, modifier = Modifier.align(Alignment.Start).padding(start = 8.dp))
            Row(modifier = Modifier.fillMaxWidth().background(Color.DarkGray.copy(alpha=0.1f), RoundedCornerShape(8.dp)).padding(8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onCashClick() }.padding(8.dp)) {
                    Text("Efectivo", fontSize=12.sp, color=Color.Gray)
                    Text("-${formatCOP(cashExpense)}", fontSize=14.sp, fontWeight=FontWeight.Bold, color = Color(0xFFE53935))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onDigitalClick() }.padding(8.dp)) {
                    Text("Digital", fontSize=12.sp, color=Color.Gray)
                    Text("-${formatCOP(digitalExpense)}", fontSize=14.sp, fontWeight=FontWeight.Bold, color = Color(0xFFE53935))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                // MODIFICADO: Agregada la función clickable hacia onIncomeClick
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onIncomeClick() }.padding(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.clip(CircleShape).background(Color(0xFFE8F5E9).copy(alpha = 0.2f)).padding(4.dp)) { Text("🟢", fontSize = 12.sp) }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ingresos", color = Color.Gray)
                    }
                    Text(formatCOP(income), fontWeight = FontWeight.Bold, color = Color(0xFF2196F3))
                }

                // MODIFICADO: Agregada la función clickable hacia onExpenseClick
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onExpenseClick() }.padding(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.clip(CircleShape).background(Color(0xFFFFEBEE).copy(alpha = 0.2f)).padding(4.dp)) { Text("🔴", fontSize = 12.sp) }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Gastos", color = Color.Gray)
                    }
                    Text(formatCOP(expense), fontWeight = FontWeight.Bold, color = Color(0xFFF44336))
                }
            }
        }
    }
}
