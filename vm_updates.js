const fs = require('fs');
let vm = fs.readFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/viewmodel/FinanceViewModel.kt', 'utf-8');

if (!vm.includes('ComercioPedido')) {
    vm = vm.replace('import com.xxcamixx.contabilidad.model.ComercioProduct', 'import com.xxcamixx.contabilidad.model.ComercioProduct\nimport com.xxcamixx.contabilidad.model.ComercioPedido');
    
    vm = vm.replace('val comercioProducts: kotlinx.coroutines.flow.Flow<List<ComercioProduct>> = _selectedCountryFlow.flatMapLatest { dao.getAllComercioProducts(it) }', 'val comercioPedidos: kotlinx.coroutines.flow.Flow<List<ComercioPedido>> = _selectedCountryFlow.flatMapLatest { dao.getAllComercioPedidos(it) }\n    val comercioProducts: kotlinx.coroutines.flow.Flow<List<ComercioProduct>> = _selectedCountryFlow.flatMapLatest { dao.getAllComercioProducts(it) }');

    // addComercioPedido
    const insertPedido = `
    fun addComercioPedido(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertComercioPedido(ComercioPedido(name = name, country = selectedCountry))
        }
    }
    fun deleteComercioPedido(pedido: ComercioPedido) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteComercioPedido(pedido)
            // ideally cascade delete products, but assuming Room doesn't have it configured, we can manually delete products later or leave it.
        }
    }
    `;
    
    // Modify addComercioProduct to take pedidoId
    vm = vm.replace('fun addComercioProduct(name: String, unit: String, quantity: Double, cost: Double, salePrice: Double) {', 'fun addComercioProduct(pedidoId: Int, name: String, unit: String, quantity: Double, cost: Double, salePrice: Double) {\n        if(pedidoId == 0) return');
    vm = vm.replace('name = name, unit = unit, quantityInStock = quantity', 'pedidoId = pedidoId, name = name, unit = unit, quantityInStock = quantity');
    
    // Add checkout cart
    const checkoutCartMethod = `
    fun checkoutComercioCart(items: List<Triple<ComercioProduct, Double, Double>>) {
        // Triple: Product, Qty, SalePrice
        viewModelScope.launch(Dispatchers.IO) {
            for (item in items) {
                val p = item.first
                val q = item.second
                val sp = item.third
                dao.updateComercioProduct(p.copy(
                    quantityInStock = p.quantityInStock - q,
                    totalSold = p.totalSold + q,
                    salePricePerUnit = sp
                ))
                dao.insertComercioMovement(ComercioMovement(
                    productId = p.id, productName = p.name, type = "VENTA",
                    quantity = q, pricePerUnit = sp, total = q * sp,
                    country = selectedCountry
                ))
            }
        }
    }
    `;

    // find where addComercioPedido should be inserted
    vm = vm.replace('fun addComercioProduct(', insertPedido + '\n' + checkoutCartMethod + '\n    fun addComercioProduct(');

    fs.writeFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/viewmodel/FinanceViewModel.kt', vm, 'utf-8');
    console.log('ViewModel updated');
} else {
    console.log('ViewModel already updated');
}
