const fs = require('fs');

let dao = fs.readFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/data/FinanceDao.kt', 'utf-8');
if (!dao.includes('updateTransaction')) {
    dao = dao.replace('@Insert suspend fun insertTransaction(transaction: Transaction)', 
        '@Insert suspend fun insertTransaction(transaction: Transaction)\n    @androidx.room.Update suspend fun updateTransaction(transaction: Transaction)');
    fs.writeFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/data/FinanceDao.kt', dao, 'utf-8');
    console.log('Added to DAO');
}

let vm = fs.readFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/viewmodel/FinanceViewModel.kt', 'utf-8');
if (!vm.includes('updateTransaction')) {
    vm = vm.replace('fun deleteTransaction(transaction: Transaction) = viewModelScope.launch(Dispatchers.IO) { dao.deleteTransaction(transaction) }',
        'fun deleteTransaction(transaction: Transaction) = viewModelScope.launch(Dispatchers.IO) { dao.deleteTransaction(transaction) }\n    fun updateTransaction(transaction: Transaction) = viewModelScope.launch(Dispatchers.IO) { dao.updateTransaction(transaction) }');
    fs.writeFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/viewmodel/FinanceViewModel.kt', vm, 'utf-8');
    console.log('Added to ViewModel');
}
