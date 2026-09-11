const fs = require('fs');
let content = fs.readFileSync('app/src/main/java/com/xxcamixx/contabilidad/ui/screens/FinanceScreen.kt', 'utf-8');

content = content.replace(
    /manageUser\(email, "setRole", "Invitado-Gold", 8640000L\) \/\/ 100 days roughly for unlimited feeling, but they asked for time of use so let's check what time of use means/,
    `manageUser(email, "setRole", "Invitado-Gold", 86400L)`
);

fs.writeFileSync('app/src/main/java/com/xxcamixx/contabilidad/ui/screens/FinanceScreen.kt', content, 'utf-8');
