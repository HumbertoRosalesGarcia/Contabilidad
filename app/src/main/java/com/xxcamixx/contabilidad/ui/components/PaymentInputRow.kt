package com.xxcamixx.contabilidad.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xxcamixx.contabilidad.util.cleanAmountInput
import com.xxcamixx.contabilidad.util.cleanDecimalInput

@Composable
fun PaymentInputRow(name: String, amountRaw: String, sym: String, visualTrans: VisualTransformation, selectedCountry: String, onAmountChange: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(name, modifier = Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = amountRaw,
            onValueChange = { onAmountChange(if (selectedCountry == "Venezuela") cleanDecimalInput(it) else cleanAmountInput(it)) },
            modifier = Modifier.weight(1.5f).height(54.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            visualTransformation = visualTrans,
            leadingIcon = { Text(sym, color = Color.Gray, modifier = Modifier.padding(start=8.dp)) }
        )
    }
}
