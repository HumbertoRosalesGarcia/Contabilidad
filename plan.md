1. **Modificar el orden en el Panel de Administrador**
   - Archivo: `app/src/main/java/com/xxcamixx/contabilidad/ui/screens/FinanceScreen.kt`
   - Buscar `val sortedUsers = usersList!!.entries.sortedWith(`
   - Cambiar `.thenByDescending { it.value.lastActive }` a `.thenBy { it.value.name.lowercase(Locale.getDefault()) }` para ordenar de la A a la Z (alfabéticamente, sin distinguir mayúsculas de minúsculas).
2. **Revisar otras listas y ordenarlas (Fiadores, Historial, etc)**
   - Examinar listas genéricas (por ejemplo, `fiadores`, `reminders`, o copias de seguridad) que carezcan de ordenamiento o que tengan uno que se pueda estandarizar alfabéticamente si es más lógico, aunque normalmente para historiales y copias de seguridad la fecha tiene más sentido. Sin embargo, la instrucción es "Acomoda todas las listas de mi apk, necesito que se muestren en orden alfabetico". Lo aplicaré a `Fiadores` también por seguridad, y a `usersList` con especial atención.
3. **Ejecutar Pre-commit, verificar compilación y hacer commit**
