const fs = require('fs');

let c = fs.readFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/ui/screens/FinanceScreen.kt','utf-8');

if (!c.includes('import com.xxcamixx.contabilidad.model.ComercioProduct')) {
    c = c.replace('import com.xxcamixx.contabilidad.model.Product', 'import com.xxcamixx.contabilidad.model.Product\nimport com.xxcamixx.contabilidad.model.ComercioProduct\nimport com.xxcamixx.contabilidad.model.ComercioMovement');
    fs.writeFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/ui/screens/FinanceScreen.kt', c, 'utf-8');
    console.log('Added imports to FinanceScreen');
} else {
    console.log('Imports already exist');
}
