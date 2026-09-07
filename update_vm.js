const fs = require('fs');
let vm = fs.readFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/viewmodel/FinanceViewModel.kt', 'utf-8');

const newMethods = `
    fun updateComercioPedido(pedido: ComercioPedido) {
        viewModelScope.launch(Dispatchers.IO) {
            // Need to add this to DAO if missing, let's just use raw query if missing or add to DAO.
            // Wait, we didn't add updateComercioPedido to DAO. We will add it.
            dao.updateComercioPedido(pedido)
        }
    }
    fun deleteComercioProduct(product: ComercioProduct) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteComercioProduct(product)
        }
    }
    fun updateComercioProduct(product: ComercioProduct) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateComercioProduct(product)
        }
    }
`;

if (!vm.includes('updateComercioPedido(pedido: ComercioPedido)')) {
    vm = vm.replace('fun deleteComercioPedido(pedido: ComercioPedido) {', newMethods + '\n    fun deleteComercioPedido(pedido: ComercioPedido) {');
    fs.writeFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/viewmodel/FinanceViewModel.kt', vm, 'utf-8');
    console.log('ViewModel updated with edit/delete methods');
} else {
    console.log('ViewModel already has edit/delete methods');
}
