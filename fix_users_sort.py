with open('app/src/main/java/com/xxcamixx/contabilidad/ui/screens/FinanceScreen.kt', 'r') as f:
    content = f.read()

pattern = r".thenByDescending \{ it.value.lastActive \}"
replacement = r".thenBy { it.value.name.lowercase(java.util.Locale.getDefault()) }"

# Ensure we only replace the specific one in AdminPanel
# The previous search confirmed it's lines 885-888

content = content.replace(pattern, replacement)

with open('app/src/main/java/com/xxcamixx/contabilidad/ui/screens/FinanceScreen.kt', 'w') as f:
    f.write(content)
