$files = @(
    "d:\Proyectos\Contabilidad\app\src\main\java\com\xxcamixx\contabilidad\ui\dialogs\ChatDialog.kt",
    "d:\Proyectos\Contabilidad\app\src\main\java\com\xxcamixx\contabilidad\ui\screens\FinanceScreen.kt",
    "d:\Proyectos\Contabilidad\app\src\main\java\com\xxcamixx\contabilidad\ui\screens\LoginScreen.kt",
    "d:\Proyectos\Contabilidad\app\src\main\java\com\xxcamixx\contabilidad\model\ApiModels.kt"
)

$utf8 = [System.Text.Encoding]::UTF8
$ansi = [System.Text.Encoding]::GetEncoding(1252)

foreach ($file in $files) {
    if (Test-Path $file) {
        $text = [System.IO.File]::ReadAllText($file, $utf8)
        # Verify if it contains corruption
        if ($text -match "TÃ©cnico" -or $text -match "Ã¡" -or $text -match "Ã³") {
            # Convert corrupted string back to bytes using ANSI
            $bytes = $ansi.GetBytes($text)
            # Interpret bytes as UTF8
            $restored = $utf8.GetString($bytes)
            [System.IO.File]::WriteAllText($file, $restored, $utf8)
            Write-Output "Restored encoding for $file"
        }
    }
}
