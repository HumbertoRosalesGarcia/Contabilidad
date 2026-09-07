const fs = require('fs');
let c = fs.readFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/ui/screens/FinanceScreen.kt','utf-8');

// 1. Agregar estado para el diálogo de edición (junto a showAddDialog)
c = c.replace(
    'var showAddDialog by remember { mutableStateOf(false) }',
    'var showAddDialog by remember { mutableStateOf(false) }\n    var editingTransaction by remember { mutableStateOf<com.xxcamixx.contabilidad.model.Transaction?>(null) }'
);

// 2. Agregar onEdit al TransactionItem en la pantalla principal
c = c.replace(
    `                                        TransactionItem(
                                            transaction = transaction,
                                            onDelete = {
                                                viewModel.deleteTransaction(transaction)
                                                coroutineScope.launch {
                                                    val result = snackbarHostState.showSnackbar(message = "Registro eliminado ???", actionLabel = "Deshacer ??", duration = SnackbarDuration.Short)
                                                    if (result == SnackbarResult.ActionPerformed) viewModel.insertRawTransaction(transaction)
                                                }
                                            },
                                            onImageClick = { uri -> expandedImageUri = uri }
                                        )`,
    `                                        TransactionItem(
                                            transaction = transaction,
                                            onDelete = {
                                                viewModel.deleteTransaction(transaction)
                                                coroutineScope.launch {
                                                    val result = snackbarHostState.showSnackbar(message = "Registro eliminado ???", actionLabel = "Deshacer ??", duration = SnackbarDuration.Short)
                                                    if (result == SnackbarResult.ActionPerformed) viewModel.insertRawTransaction(transaction)
                                                }
                                            },
                                            onEdit = { editingTransaction = transaction },
                                            onImageClick = { uri -> expandedImageUri = uri }
                                        )`
);

// 3. Agregar el diálogo de edición antes del cierre del AddTransactionDialog
c = c.replace(
    '        if (showAddDialog) {',
    `        if (editingTransaction != null) {
            com.xxcamixx.contabilidad.ui.dialogs.EditTransactionDialog(
                transaction = editingTransaction!!,
                onDismiss = { editingTransaction = null },
                onConfirm = { isCash, isDigital, note ->
                    val t = editingTransaction!!
                    val updatedTransaction = t.copy(
                        note = note,
                        cashAmount = if (isCash) t.amount else 0.0,
                        digitalAmount = if (isDigital) t.amount else 0.0
                    )
                    viewModel.updateTransaction(updatedTransaction)
                    editingTransaction = null
                }
            )
        }
        if (showAddDialog) {`
);

fs.writeFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/ui/screens/FinanceScreen.kt', c, 'utf-8');
console.log('FinanceScreen updated with edit dialog');
