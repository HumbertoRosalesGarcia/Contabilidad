const fs = require('fs');

let fsKt = fs.readFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/ui/screens/FinanceScreen.kt', 'utf-8');

if (!fsKt.includes('val comercioPedidos')) {
    fsKt = fsKt.replace('val comercioProducts by viewModel.comercioProducts.collectAsState(initial = emptyList())', 'val comercioPedidos by viewModel.comercioPedidos.collectAsState(initial = emptyList())\n    val comercioProducts by viewModel.comercioProducts.collectAsState(initial = emptyList())');
    
    // update NavigationBarItem and Titles from "Comercio" to "Pedidos"
    fsKt = fsKt.replace('Icon(Icons.Filled.ShoppingCart, "Comercio")', 'Icon(Icons.Filled.ShoppingCart, "Pedidos")');
    fsKt = fsKt.replace('label = { Text("Comercio") }', 'label = { Text("Pedidos") }');
    fsKt = fsKt.replace('Comercio ??', 'Pedidos ??');
    
    // update function call
    fsKt = fsKt.replace('ComercioScreen(viewModel, comercioProducts, comercioMovements)', 'ComercioScreen(viewModel, comercioPedidos, comercioProducts, comercioMovements)');
    
    fs.writeFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/ui/screens/FinanceScreen.kt', fsKt, 'utf-8');
    console.log('FinanceScreen updated for Pedidos');
} else {
    console.log('FinanceScreen already updated');
}
