$file = "d:\Proyectos\Contabilidad\app\src\main\java\com\xxcamixx\contabilidad\ui\screens\FinanceScreen.kt"
$content = Get-Content $file -Raw -Encoding UTF8

# Busca la última ocurrencia de "    }" y la reemplaza por nuestra inyección
$index = $content.LastIndexOf("    }")
if ($index -ge 0) {
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
    # Cortar el string
    $newContent = $content.Substring(0, $index) + $injection
    Set-Content $file -Value $newContent -Encoding UTF8
}
