$files = Get-ChildItem -Path "d:\Proyectos\Contabilidad\app\src\main\java\com\xxcamixx\contabilidad\ui" -Recurse -Filter "*.kt"

$utf8 = [System.Text.Encoding]::UTF8
$ansi = [System.Text.Encoding]::GetEncoding(1252)

foreach ($file in $files) {
    $text = [System.IO.File]::ReadAllText($file.FullName, $utf8)
    if ($text -match "ðŸ" -or $text -match "Ã" -or $text -match "") {
        $bytes = $ansi.GetBytes($text)
        $restored = $utf8.GetString($bytes)
        [System.IO.File]::WriteAllText($file.FullName, $restored, $utf8)
        Write-Output "Restored encoding for $($file.Name)"
    }
}
