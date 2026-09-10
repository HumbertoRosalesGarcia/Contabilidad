1. **Identificar la cuenta de administrador**
   - Buscar la lógica en `FinanceScreen.kt` (aprox. línea 885) donde se listan los usuarios en el Panel de Administrador.
   - La cuenta es `zonacami77777@gmail.com`.
2. **Ordenar la lista de usuarios**
   - Modificar la forma en que se ordenan los usuarios (`sortedUsers`).
   - El administrador debe aparecer siempre de primero. Para lograr esto, se puede ordenar primero por si el email es igual a `zonacami77777@gmail.com`, y luego por `lastActive` de forma descendente.
3. **Modificar el estilo visual de la tarjeta del administrador**
   - Comprobar si `email == "zonacami77777@gmail.com"`.
   - Si es el administrador, cambiar el color de la tarjeta (por ejemplo, usar un borde dorado o un fondo amarillo pálido).
4. **Ocultar los controles de rol y bloqueo para el administrador**
   - Envolver el ícono de opciones (los tres puntos verticales), el botón de "Asignar Rol" (y su menú) y el botón de "Bloquear" dentro de una condición `if (email != "zonacami77777@gmail.com")`.
   - Esto asegurará que el administrador no pueda bloquearse ni cambiar su propio rol accidentalmente a través del panel.
5. **Ejecutar pruebas y comprobaciones**
   - Ejecutar la construcción del proyecto y revisar que no existan errores de sintaxis en `FinanceScreen.kt`.
6. **Crear commit y enviar los cambios**.
