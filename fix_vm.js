const fs = require('fs');

let c = fs.readFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/viewmodel/FinanceViewModel.kt','utf-8');

c = c.replace('val comercioProducts = dao.getAllComercioProducts(selectedCountry).stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())', 'val comercioProducts: kotlinx.coroutines.flow.Flow<List<ComercioProduct>> = _selectedCountryFlow.flatMapLatest { dao.getAllComercioProducts(it) }');

c = c.replace('val comercioMovements = dao.getAllComercioMovements(selectedCountry).stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())', 'val comercioMovements: kotlinx.coroutines.flow.Flow<List<ComercioMovement>> = _selectedCountryFlow.flatMapLatest { dao.getAllComercioMovements(it) }');

fs.writeFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/viewmodel/FinanceViewModel.kt', c, 'utf-8');
console.log('Fixed flow behavior for country switching');
