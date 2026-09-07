const fs = require('fs');
let vm = fs.readFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/viewmodel/FinanceViewModel.kt', 'utf-8');

vm = vm.replace('fun addComercioPedido(name: String) {', 'fun addComercioPedido(name: String, imageUri: String? = null) {');
vm = vm.replace('ComercioPedido(name = name, country = _selectedCountryFlow.value)', 'ComercioPedido(name = name, imageUri = imageUri, country = _selectedCountryFlow.value)');

vm = vm.replace('fun addComercioProduct(pedidoId: Int, name: String, unit: String, quantity: Double, cost: Double, salePrice: Double) {', 'fun addComercioProduct(pedidoId: Int, name: String, unit: String, quantity: Double, cost: Double, salePrice: Double, imageUri: String? = null) {');
vm = vm.replace('val product = ComercioProduct(pedidoId = pedidoId, name = name, unit = unit, quantityInStock = quantity, totalPurchased = quantity, costPerUnit = cost, salePricePerUnit = salePrice, country = _selectedCountryFlow.value)', 'val product = ComercioProduct(pedidoId = pedidoId, name = name, unit = unit, quantityInStock = quantity, totalPurchased = quantity, costPerUnit = cost, salePricePerUnit = salePrice, imageUri = imageUri, country = _selectedCountryFlow.value)');

fs.writeFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/viewmodel/FinanceViewModel.kt', vm, 'utf-8');
console.log('ViewModel add methods updated');
