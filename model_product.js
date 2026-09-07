const fs = require('fs');

// 2. Modificar ComercioProduct
let p = fs.readFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/model/ComercioProduct.kt', 'utf-8');
if (!p.includes('val pedidoId')) {
    p = p.replace('val name: String,', 'val pedidoId: Int = 0,\n    val name: String,');
    fs.writeFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/model/ComercioProduct.kt', p, 'utf-8');
}
console.log('Modelo ComercioProduct modificado.');
