const fs = require('fs');

let c = fs.readFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/ui/dialogs/AddComercioProductDialog.kt', 'utf-8');

c = c.replace('Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {', 
    'Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {');
// Wait, a better fix is to set maxLines = 1 for the Text.
c = c.replace('Text(u, fontSize = 14.sp)', 'Text(u, fontSize = 14.sp, maxLines = 1, softWrap = false)');

fs.writeFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/ui/dialogs/AddComercioProductDialog.kt', c, 'utf-8');
console.log('Fixed "paq" softWrap in Dialog');
