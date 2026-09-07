const fs = require('fs');

let c = fs.readFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/ui/screens/FinanceScreen.kt','utf-8');

c = c.replace('it.isStore == (currentTab == 1)', 'it.isStore == (currentTab == 2)');
c = c.replace('it.isStore == (currentTab == 1)', 'it.isStore == (currentTab == 2)');
c = c.replace('isStore = (currentTab == 1),', 'isStore = (currentTab == 2),');
c = c.replace('isStore = (currentTab == 1 || checkoutToFiadorCart.isNotEmpty()),', 'isStore = (currentTab == 2 || checkoutToFiadorCart.isNotEmpty()),');
c = c.replace('isStore = (currentTab == 1 || cart.isNotEmpty()),', 'isStore = (currentTab == 2 || cart.isNotEmpty()),');

fs.writeFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/ui/screens/FinanceScreen.kt', c, 'utf-8');
console.log('Fixed currentTab references for Tienda');
