const fs = require('fs');

let c = fs.readFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/ui/screens/FinanceScreen.kt','utf-8');

// 1. Add flows
c = c.replace('val products by viewModel.products.collectAsState(initial = emptyList())', 'val products by viewModel.products.collectAsState(initial = emptyList())\n    val comercioProducts by viewModel.comercioProducts.collectAsState(initial = emptyList())\n    val comercioMovements by viewModel.comercioMovements.collectAsState(initial = emptyList())');

// 2. Change TopAppBar Title
c = c.replace('Text(text = if (currentTab == 0) "Hola, $firstName $crownEmoji" else "Tienda de $firstName ??"', 'Text(text = if (currentTab == 0) "Hola, $firstName $crownEmoji" else if (currentTab == 1) "Comercio ??" else "Tienda de $firstName ??"');

// 3. Navigation Bar
const oldNavBarStore = 'NavigationBarItem(icon = { Icon(Icons.Filled.Storefront, "Tienda") }, label = { Text("Tienda") }, selected = currentTab == 1, onClick = { currentTab = 1 }, colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.primary))';
const newNavBar = `NavigationBarItem(icon = { Icon(Icons.Filled.ShoppingCart, "Comercio") }, label = { Text("Comercio") }, selected = currentTab == 1, onClick = { currentTab = 1 }, colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.primary))
                            NavigationBarItem(icon = { Icon(Icons.Filled.Storefront, "Tienda") }, label = { Text("Tienda") }, selected = currentTab == 2, onClick = { currentTab = 2 }, colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.primary))`;
c = c.replace(oldNavBarStore, newNavBar);

// 4. Content switching
// Find "tab == 0" end brace
const lines = c.split('\n');
let braceCount = 0;
let insideTab0 = false;
let tab0EndLine = -1;
for(let i=0; i<lines.length; i++){
    if (lines[i].includes('if (tab == 0) {') && !insideTab0) {
        insideTab0 = true;
        braceCount = 1;
        continue;
    }
    if (insideTab0) {
        for(let j=0; j<lines[i].length; j++) {
            if (lines[i][j] === '{') braceCount++;
            if (lines[i][j] === '}') {
                braceCount--;
                if (braceCount === 0) {
                    tab0EndLine = i;
                    insideTab0 = false;
                    break;
                }
            }
        }
    }
}
if (tab0EndLine !== -1) {
    if (lines[tab0EndLine].includes('} else {')) {
        lines[tab0EndLine] = lines[tab0EndLine].replace('} else {', '} else if (tab == 1) { ComercioScreen(viewModel, comercioProducts, comercioMovements) } else {');
    }
}

// 5. DayClick logic
for(let i=0; i<lines.length; i++){
    if (lines[i].includes('if (currentTab == 0) {') && lines[i+1] && lines[i+1].includes('showReminderDialog = true')) {
        if (lines[i+2].includes('} else {')) {
            lines[i+2] = lines[i+2].replace('} else {', '} else if (currentTab == 2) {');
        }
    }
}

fs.writeFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/ui/screens/FinanceScreen.kt', lines.join('\n'), 'utf-8');
console.log('FinanceScreen.kt patched');
