1. **Analyze Requirements**: The user wants a new "Cierres" (Closings) feature. This entails:
   - A new screen/dialog accessible from the top-right menu.
   - The ability to close a session for "Personal", "Pedidos", and "Tienda".
   - A history of closed sessions ("Historial de cierres").
   - Moving sales with pending debts to a separate "Pending" section within "Cierres".
2. **Database Changes**:
   - Create a `CierreSession` entity: `id`, `name` (e.g., "Personal", "Pedidos", "Tienda"), `timestamp`, `country`.
   - Update `ComercioMovement` and `Transaction` to include a `cierreId` or `isClosed` flag. For debts, we might need a status (e.g., "Pending", "Closed"). We will need a Room DB migration (version 24).
3. **UI Updates (`FinanceScreen.kt`)**:
   - Add "Cierres" to the main options menu (top-right).
   - Create dialogs/screens for closing a session and viewing the history.
   - Ensure the views filter out closed transactions unless viewing history.
4. **Pre-commit Steps**: Call `pre_commit_instructions` and follow its instructions to ensure proper testing, verification, review, and reflection are done.
5. **Submit**: Commit and submit the code.
