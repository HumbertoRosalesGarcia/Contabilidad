const fs = require('fs');
let dao = fs.readFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/data/FinanceDao.kt', 'utf-8');

if (!dao.includes('ComercioPedido')) {
    dao = dao.replace('import com.xxcamixx.contabilidad.model.ComercioProduct', 'import com.xxcamixx.contabilidad.model.ComercioProduct\nimport com.xxcamixx.contabilidad.model.ComercioPedido');
    const newMethods = `
    @Query("SELECT * FROM comercio_pedidos WHERE country = :country ORDER BY timestamp DESC")
    fun getAllComercioPedidos(country: String): kotlinx.coroutines.flow.Flow<List<ComercioPedido>>

    @Insert suspend fun insertComercioPedido(pedido: ComercioPedido): Long
    @Delete suspend fun deleteComercioPedido(pedido: ComercioPedido)
    
    @Query("SELECT * FROM comercio_pedidos WHERE country = :country ORDER BY timestamp DESC")
    suspend fun getBackupComercioPedidos(country: String): List<ComercioPedido>
`;
    // insert before the last brace
    const lastBrace = dao.lastIndexOf('}');
    dao = dao.substring(0, lastBrace) + newMethods + '\n}\n';
    fs.writeFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/data/FinanceDao.kt', dao, 'utf-8');
}

console.log('DAO updated');
