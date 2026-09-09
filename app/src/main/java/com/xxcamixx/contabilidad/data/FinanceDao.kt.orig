package com.xxcamixx.contabilidad.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.xxcamixx.contabilidad.model.Transaction
import com.xxcamixx.contabilidad.model.Reminder
import com.xxcamixx.contabilidad.model.Fiador
import com.xxcamixx.contabilidad.model.Product
import com.xxcamixx.contabilidad.model.ComercioProduct
import com.xxcamixx.contabilidad.model.ComercioMovement
import com.xxcamixx.contabilidad.model.ComercioPedido
import com.xxcamixx.contabilidad.model.CierreSession

@Dao
interface FinanceDao {
    @Query("SELECT * FROM transactions WHERE country = :country AND cierreId IS NULL ORDER BY timestamp DESC")
    fun getAllTransactions(country: String): Flow<List<Transaction>>

    @Insert suspend fun insertTransaction(transaction: Transaction)
    @Delete suspend fun deleteTransaction(transaction: Transaction)
    @Query("DELETE FROM transactions") suspend fun deleteAllTransactions()

    @Query("DELETE FROM transactions WHERE description NOT LIKE 'Venta: %' AND country = :country")
    suspend fun deletePersonalTransactions(country: String)

    @Query("UPDATE transactions SET profit = 0.0, cashAmount = 0.0, digitalAmount = 0.0 WHERE country = :country")
    suspend fun resetAllProfits(country: String)

    @Query("SELECT * FROM reminders WHERE country = :country ORDER BY targetDateInMillis ASC")
    fun getAllReminders(country: String): Flow<List<Reminder>>

    @Insert suspend fun insertReminder(reminder: Reminder): Long
    @Update suspend fun updateReminder(reminder: Reminder)
    @Delete suspend fun deleteReminder(reminder: Reminder)
    @Query("DELETE FROM reminders") suspend fun deleteAllReminders()

    @Query("SELECT * FROM fiadores WHERE country = :country AND cierreId IS NULL ORDER BY targetDateInMillis ASC")
    fun getAllFiadores(country: String): Flow<List<Fiador>>

    @Insert suspend fun insertFiador(fiador: Fiador): Long
    @Update suspend fun updateFiador(fiador: Fiador)
    @Delete suspend fun deleteFiador(fiador: Fiador)
    @Query("DELETE FROM fiadores") suspend fun deleteAllFiadores()

    @Query("SELECT * FROM products WHERE country = :country ORDER BY name ASC")
    fun getAllProducts(country: String): Flow<List<Product>>

    @Insert suspend fun insertProduct(product: Product): Long
    @Update suspend fun updateProduct(product: Product)
    @Delete suspend fun deleteProduct(product: Product)
    @Query("DELETE FROM products") suspend fun deleteAllProducts()

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC") suspend fun getBackupTransactions(): List<Transaction>
    @Query("SELECT * FROM reminders ORDER BY targetDateInMillis ASC") suspend fun getBackupReminders(): List<Reminder>
    @Query("SELECT * FROM fiadores ORDER BY targetDateInMillis ASC") suspend fun getBackupFiadores(): List<Fiador>
    @Query("SELECT * FROM products ORDER BY name ASC") suspend fun getBackupProducts(): List<Product>

    // --- COMERCIO ---
    @Query("SELECT * FROM comercio_pedidos WHERE country = :country ORDER BY timestamp DESC")
    fun getAllComercioPedidos(country: String): Flow<List<ComercioPedido>>

    @Insert suspend fun insertComercioPedido(pedido: ComercioPedido): Long
    @Update suspend fun updateComercioPedido(pedido: ComercioPedido)
    @Delete suspend fun deleteComercioPedido(pedido: ComercioPedido)

    @Query("SELECT * FROM comercio_pedidos ORDER BY timestamp DESC")
    suspend fun getBackupComercioPedidos(): List<ComercioPedido>

    @Query("SELECT * FROM comercio_products WHERE country = :country ORDER BY name ASC")
    fun getAllComercioProducts(country: String): Flow<List<ComercioProduct>>

    @Query("DELETE FROM comercio_products WHERE pedidoId = :pedidoId")
    suspend fun deleteComercioProductsByPedidoId(pedidoId: Int)

    @Query("SELECT * FROM comercio_movements WHERE country = :country AND cierreId IS NULL ORDER BY timestamp DESC")
    fun getAllComercioMovements(country: String): Flow<List<ComercioMovement>>

    @Insert suspend fun insertComercioProduct(product: ComercioProduct): Long
    @Update suspend fun updateComercioProduct(product: ComercioProduct)
    @Delete suspend fun deleteComercioProduct(product: ComercioProduct)

    @Insert suspend fun insertComercioMovement(movement: ComercioMovement)
    @Delete suspend fun deleteComercioMovement(movement: ComercioMovement)

    @Query("DELETE FROM comercio_movements WHERE productId = :productId")
    suspend fun deleteComercioMovementsByProductId(productId: Int)

    @Query("DELETE FROM comercio_movements WHERE productId IN (SELECT id FROM comercio_products WHERE pedidoId = :pedidoId)")
    suspend fun deleteComercioMovementsByPedidoId(pedidoId: Int)

    @Query("DELETE FROM comercio_movements WHERE productId NOT IN (SELECT id FROM comercio_products)")
    suspend fun deleteOrphanComercioMovements()

    @Query("SELECT * FROM comercio_movements ORDER BY timestamp DESC")
    suspend fun getBackupComercioMovements(): List<ComercioMovement>

    @Query("SELECT * FROM comercio_products ORDER BY name ASC")
    suspend fun getBackupComercioProducts(): List<ComercioProduct>

    @Query("DELETE FROM comercio_products") suspend fun deleteAllComercioProducts()
    @Query("DELETE FROM comercio_movements") suspend fun deleteAllComercioMovements()
    @Query("DELETE FROM comercio_pedidos") suspend fun deleteAllComercioPedidos()

    // --- CIERRES ---
    @Query("SELECT * FROM cierre_sessions WHERE country = :country ORDER BY timestamp DESC")
    fun getAllCierreSessions(country: String): Flow<List<CierreSession>>

    @Insert suspend fun insertCierreSession(session: CierreSession): Long
    @Delete suspend fun deleteCierreSession(session: CierreSession)

    @Query("SELECT * FROM cierre_sessions ORDER BY timestamp DESC")
    suspend fun getBackupCierreSessions(): List<CierreSession>

    @Query("DELETE FROM cierre_sessions") suspend fun deleteAllCierreSessions()

    // Query para obtener transacciones sin cierre de tipo personal/pedidos/tienda (isIncome no se usa aquí pero podríamos filtrar por categoría si quisiéramos)
    @Query("SELECT * FROM transactions WHERE country = :country AND cierreId IS NULL")
    fun getTransactionsWithoutCierre(country: String): Flow<List<Transaction>>

    @Query("SELECT * FROM fiadores WHERE country = :country AND cierreId IS NULL")
    fun getFiadoresWithoutCierre(country: String): Flow<List<Fiador>>

    @Query("SELECT * FROM comercio_movements WHERE country = :country AND cierreId IS NULL")
    fun getComercioMovementsWithoutCierre(country: String): Flow<List<ComercioMovement>>

    @Query("UPDATE transactions SET cierreId = :cierreId WHERE country = :country AND cierreId IS NULL AND description NOT LIKE 'Venta: %'")
    suspend fun updatePersonalTransactionsWithCierre(country: String, cierreId: Int)

    @Query("UPDATE transactions SET cierreId = :cierreId WHERE country = :country AND cierreId IS NULL AND description LIKE 'Venta: %'")
    suspend fun updateTiendaTransactionsWithCierre(country: String, cierreId: Int)

    @Query("UPDATE fiadores SET cierreId = :cierreId WHERE country = :country AND cierreId IS NULL AND originMode = :mode")
    suspend fun updateFiadoresWithCierre(country: String, cierreId: Int, mode: String)

    @Query("UPDATE comercio_movements SET cierreId = :cierreId WHERE country = :country AND cierreId IS NULL")
    suspend fun updateComercioMovementsWithCierre(country: String, cierreId: Int)
}
