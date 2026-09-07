const fs = require('fs');
let c = fs.readFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/ui/screens/FinanceScreen.kt', 'utf-8');

if (!c.includes('import androidx.compose.material.icons.filled.ShoppingCart')) {
    c = c.replace('import androidx.compose.material.icons.filled.Storefront', 'import androidx.compose.material.icons.filled.Storefront\nimport androidx.compose.material.icons.filled.ShoppingCart');
    fs.writeFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/ui/screens/FinanceScreen.kt', c, 'utf-8');
    console.log('ShoppingCart import added.');
} else {
    console.log('Import already exists.');
}
