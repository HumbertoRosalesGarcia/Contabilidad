const fs = require('fs');
let content = fs.readFileSync('app/src/main/java/com/xxcamixx/contabilidad/ui/screens/FinanceScreen.kt', 'utf-8');

// Use proper Locale for lowercase to avoid compilation errors
content = content.replace(
    /val sortedUsers = usersList!!.entries.filter \{ !it.value.name.lowercase\(\).contains\("usuario de prueba"\) && !it.value.name.lowercase\(\).contains\("prueba_"\) && !it.key.lowercase\(\).contains\("prueba_"\) \}/,
    `val sortedUsers = usersList!!.entries.filter { !it.value.name.lowercase(java.util.Locale.getDefault()).contains("usuario de prueba") && !it.value.name.lowercase(java.util.Locale.getDefault()).contains("prueba_") && !it.key.lowercase(java.util.Locale.getDefault()).contains("prueba_") }`
);

fs.writeFileSync('app/src/main/java/com/xxcamixx/contabilidad/ui/screens/FinanceScreen.kt', content, 'utf-8');
