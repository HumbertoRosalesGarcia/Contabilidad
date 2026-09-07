const fs = require('fs');
let c = fs.readFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/util/Formatting.kt', 'utf-8');

// The original getSmartEmoji uses some weird characters because of previous mojibake.
// Let's replace the whole function with a clean one using unicode escapes.
const newSmartEmoji = `fun getSmartEmoji(description: String, isIncome: Boolean): String {
    val descLower = description.lowercase(java.util.Locale.getDefault())
    return when {
        listOf("comida", "almuerzo", "cena", "hamburguesa", "kfc", "pizza", "salchipapa").any { descLower.contains(it) } -> "\\uD83C\\uDF54" // ??
        listOf("papa", "papas", "patata").any { descLower.contains(it) } -> "\\uD83E\\uDD54" // ??
        listOf("mercado", "supermercado", "compras", "tienda").any { descLower.contains(it) } -> "\\uD83D\\uDED2" // ??
        listOf("transporte", "pasaje", "bus", "taxi", "uber", "gasolina").any { descLower.contains(it) } -> "\\uD83D\\uDE95" // ??
        listOf("servicios", "luz", "agua", "internet", "factura").any { descLower.contains(it) } -> "\\uD83D\\uDCA1" // ??
        listOf("casa", "arriendo", "hogar", "alquiler").any { descLower.contains(it) } -> "\\uD83C\\uDFE0" // ??
        listOf("regalo", "cumplea\\u00F1os", "fiesta", "ropa", "camisa", "pantalon").any { descLower.contains(it) } -> "\\uD83C\\uDF81" // ??
        listOf("medico", "salud", "farmacia", "pastillas").any { descLower.contains(it) } -> "\\uD83D\\uDC8A" // ??
        listOf("salario", "sueldo", "pago", "nomina").any { descLower.contains(it) } -> "\\uD83D\\uDCB5" // ??
        listOf("negocio", "venta", "cliente", "producto", "varios productos").any { descLower.contains(it) } -> "\\uD83E\\uDD1D" // ??
        listOf("ahorro", "banco", "intereses", "nequi", "bancolombia", "daviplata").any { descLower.contains(it) } -> "\\uD83C\\uDFE6" // ??
        else -> if (isIncome) "\\uD83D\\uDCB0" else "\\uD83D\\uDCB8"
    }
}`;

// find and replace the old function
const startIndex = c.indexOf('fun getSmartEmoji');
if(startIndex > 0) {
    c = c.substring(0, startIndex) + newSmartEmoji + '\n';
    fs.writeFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/util/Formatting.kt', c, 'utf-8');
    console.log('Formatting.kt smart emoji updated with unicode escapes.');
} else {
    console.log('Could not find getSmartEmoji in Formatting.kt');
}
