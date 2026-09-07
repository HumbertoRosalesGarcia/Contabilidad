const fs = require('fs');
let c = fs.readFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/ui/screens/FinanceScreen.kt', 'utf-8');

c = c.replace('ComercioScreen(viewModel, comercioPedidos, comercioProducts, comercioMovements, selectedCountry, viewModel.bcvRate)', 'ComercioScreen(viewModel, comercioPedidos, comercioProducts, comercioMovements, viewModel.selectedCountry, viewModel.bcvRate)');

fs.writeFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/ui/screens/FinanceScreen.kt', c, 'utf-8');
console.log('Fixed selectedCountry to viewModel.selectedCountry.');
