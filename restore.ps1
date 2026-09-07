$convIds = @("364ee6c9-427b-4530-a9d8-96f788771781", "c5125252-78c8-4318-b7df-37e539a17828", "dca58b67-9b6c-4353-b144-9742537d3b02", "a747fad0-89f0-4471-aedb-60f978df39f4", "38c0efff-bc43-4647-8b92-53e0ecb1d76d", "5dc6dbd2-654e-4c09-a498-cb14e9f3f774", "ea7298af-60e6-4853-93f6-2c993ee95199", "a2c18cf7-e0f3-4bc0-aef5-a11b736b4a36")

foreach ($convId in $convIds) {
    $logFile = "C:/Users/Administrador/.gemini/antigravity/brain/$convId/.system_generated/logs/transcript_full.jsonl"
    if (-not (Test-Path $logFile)) {
        $logFile = "C:/Users/Administrador/.gemini/antigravity/brain/$convId/.system_generated/logs/transcript.jsonl"
    }
    
    if (Test-Path $logFile) {
        $lines = Get-Content -Path $logFile -Encoding UTF8
        foreach ($line in $lines) {
            $json = $line | ConvertFrom-Json -ErrorAction SilentlyContinue
            if ($json -and $json.tool_calls) {
                foreach ($tc in $json.tool_calls) {
                    if ($tc.function.name -eq "default_api:write_to_file") {
                        $argsJson = $tc.function.arguments | ConvertFrom-Json -ErrorAction SilentlyContinue
                        if ($argsJson -and $argsJson.TargetFile -and $argsJson.CodeContent) {
                            $targetFile = $argsJson.TargetFile
                            $code = $argsJson.CodeContent
                            
                            $code = $code -replace 'import com\.xxcamixx\.contabilidad\.network\.ChatMessage', 'import com.xxcamixx.contabilidad.model.ChatMessage'
                            $code = $code -replace 'import com\.xxcamixx\.contabilidad\.network\.ChatSendRequest', 'import com.xxcamixx.contabilidad.model.ChatSendRequest'
                            $code = $code -replace 'import com\.xxcamixx\.contabilidad\.network\.UserData', 'import com.xxcamixx.contabilidad.model.UserData'
                            $code = $code -replace 'import com\.xxcamixx\.contabilidad\.network\.UserManageRequest', 'import com.xxcamixx.contabilidad.model.UserManageRequest'
                            $code = $code -replace 'import com\.xxcamixx\.contabilidad\.network\.UserSyncRequest', 'import com.xxcamixx.contabilidad.model.UserSyncRequest'
                            $code = $code -replace 'import com\.xxcamixx\.contabilidad\.network\.UserTimeRequest', 'import com.xxcamixx.contabilidad.model.UserTimeRequest'
                            
                            $code = $code -replace 'val profileImage: String\? = null\)', 'val profileImage: String? = null, val lastActive: Long = 0L)'
                            $code = $code -replace 'import com\.xxcamixx\.contabilidad\.ui\.dialogs\.ProductInfoDialog', 'import com.xxcamixx.contabilidad.ui.components.ProductInfoDialog'
                            
                            Set-Content -Path $targetFile -Value $code -Encoding UTF8
                            Write-Output "Restored $targetFile"
                        }
                    }
                }
            }
        }
    }
}
