$file = "d:\Proyectos\Contabilidad\app\src\main\java\com\xxcamixx\contabilidad\ui\screens\FinanceScreen.kt"
$content = Get-Content $file -Raw -Encoding UTF8

# Define the bad block we added previously
$badBlock = @"
        if (showAddDialog) {
            com.xxcamixx.contabilidad.ui.dialogs.AddTransactionDialog(
                viewModel = viewModel,
                onDismiss = { showAddDialog = false }
            )
        }
"@

# Define the good block
$goodBlock = @"
        if (showAddDialog) {
            com.xxcamixx.contabilidad.ui.dialogs.AddTransactionDialog(
                categories = viewModel.customCategories.toList(),
                onDismiss = { showAddDialog = false },
                onConfirm = { desc, amount, isInc, note, method, cat, uri -> 
                    viewModel.addTransaction(desc, amount, isInc, note, method, cat, uri)
                    showAddDialog = false
                }
            )
        }
"@

# Replace
$newContent = $content.Replace($badBlock, $goodBlock)
Set-Content $file -Value $newContent -Encoding UTF8
