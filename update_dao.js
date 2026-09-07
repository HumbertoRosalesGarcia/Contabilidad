const fs = require('fs');
let dao = fs.readFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/data/FinanceDao.kt', 'utf-8');

if (!dao.includes('updateComercioPedido(pedido: ComercioPedido)')) {
    dao = dao.replace('@Delete suspend fun deleteComercioPedido(pedido: ComercioPedido)', '@Update suspend fun updateComercioPedido(pedido: ComercioPedido)\n    @Delete suspend fun deleteComercioPedido(pedido: ComercioPedido)');
    fs.writeFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/data/FinanceDao.kt', dao, 'utf-8');
    console.log('DAO updated with updateComercioPedido');
} else {
    console.log('DAO already has updateComercioPedido');
}
