$file = "d:\Proyectos\Contabilidad\app\src\main\java\com\xxcamixx\contabilidad\ui\screens\FinanceScreen.kt"
$content = Get-Content $file -Raw -Encoding UTF8

$injection = @"
        if (showAddDialog) {
            com.xxcamixx.contabilidad.ui.dialogs.AddTransactionDialog(
                viewModel = viewModel,
                onDismiss = { showAddDialog = false }
            )
        }
    }
}
"@

$content = $content -replace "    `}`\r?\n`}`$", $injection

Set-Content $file -Value $content -Encoding UTF8
