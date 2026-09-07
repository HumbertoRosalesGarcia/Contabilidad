const fs = require('fs');
let c = fs.readFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/ui/dialogs/AddComercioProductDialog.kt', 'utf-8');

if (!c.includes('import androidx.compose.ui.text.font.FontWeight')) {
    c = c.replace('import androidx.compose.ui.graphics.Color', 'import androidx.compose.ui.graphics.Color\nimport androidx.compose.ui.text.font.FontWeight');
    fs.writeFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/ui/dialogs/AddComercioProductDialog.kt', c, 'utf-8');
    console.log('FontWeight import added.');
} else {
    console.log('Import already exists.');
}
