const fs = require('fs');
const path = require('path');

const convIds = [
    "364ee6c9-427b-4530-a9d8-96f788771781", "c5125252-78c8-4318-b7df-37e539a17828", 
    "dca58b67-9b6c-4353-b144-9742537d3b02", "a747fad0-89f0-4471-aedb-60f978df39f4", 
    "38c0efff-bc43-4647-8b92-53e0ecb1d76d", "5dc6dbd2-654e-4c09-a498-cb14e9f3f774", 
    "ea7298af-60e6-4853-93f6-2c993ee95199", "a2c18cf7-e0f3-4bc0-aef5-a11b736b4a36"
];

for (const convId of convIds) {
    let logFile = `C:/Users/Administrador/.gemini/antigravity/brain/${convId}/.system_generated/logs/transcript_full.jsonl`;
    if (!fs.existsSync(logFile)) {
        logFile = `C:/Users/Administrador/.gemini/antigravity/brain/${convId}/.system_generated/logs/transcript.jsonl`;
    }
    
    if (fs.existsSync(logFile)) {
        const lines = fs.readFileSync(logFile, 'utf-8').split('\n');
        for (const line of lines) {
            if (!line.trim()) continue;
            try {
                const json = JSON.parse(line);
                if (json.tool_calls) {
                    for (const tc of json.tool_calls) {
                        if (tc.name === 'write_to_file' || tc.name === 'default_api:write_to_file') {
                            const args = tc.args || tc.function?.arguments;
                            const argsObj = typeof args === 'string' ? JSON.parse(args) : args;
                            if (argsObj.TargetFile && argsObj.CodeContent) {
                                let code = argsObj.CodeContent;
                                const targetFile = argsObj.TargetFile;

                                // 2. Fix imports
                                code = code.replace(/import com\.xxcamixx\.contabilidad\.network\.ChatMessage/g, 'import com.xxcamixx.contabilidad.model.ChatMessage');
                                code = code.replace(/import com\.xxcamixx\.contabilidad\.network\.ChatSendRequest/g, 'import com.xxcamixx.contabilidad.model.ChatSendRequest');
                                code = code.replace(/import com\.xxcamixx\.contabilidad\.network\.UserData/g, 'import com.xxcamixx.contabilidad.model.UserData');
                                code = code.replace(/import com\.xxcamixx\.contabilidad\.network\.UserManageRequest/g, 'import com.xxcamixx.contabilidad.model.UserManageRequest');
                                code = code.replace(/import com\.xxcamixx\.contabilidad\.network\.UserSyncRequest/g, 'import com.xxcamixx.contabilidad.model.UserSyncRequest');
                                code = code.replace(/import com\.xxcamixx\.contabilidad\.network\.UserTimeRequest/g, 'import com.xxcamixx.contabilidad.model.UserTimeRequest');
                                code = code.replace(/import com\.xxcamixx\.contabilidad\.network\.ApiModels/g, 'import com.xxcamixx.contabilidad.model.ApiModels'); // Just in case
                                
                                // 3. Fix lastActive
                                code = code.replace('val profileImage: String? = null)', 'val profileImage: String? = null, val lastActive: Long = 0L)');
                                
                                // 4. Fix ProductInfoDialog
                                code = code.replace('import com.xxcamixx.contabilidad.ui.dialogs.ProductInfoDialog', 'import com.xxcamixx.contabilidad.ui.components.ProductInfoDialog');
                                
                                // 5. Fix AddTransactionDialog injection in FinanceScreen.kt
                                if (targetFile.endsWith("FinanceScreen.kt")) {
                                    code = code.replace(/    \}\n\}$/, `        if (showAddDialog) {\n            com.xxcamixx.contabilidad.ui.dialogs.AddTransactionDialog(\n                categories = viewModel.customCategories.toList(),\n                onDismiss = { showAddDialog = false },\n                onConfirm = { desc, amount, isInc, note, method, cat, uri -> \n                    viewModel.addTransaction(desc, amount, isInc, note, method, cat, uri)\n                    showAddDialog = false\n                }\n            )\n        }\n    }\n}`);
                                }
                                
                                // 6. Global blue color replacement for UI files
                                if (targetFile.includes("\\ui\\") || targetFile.includes("/ui/")) {
                                    code = code.replace(/0xFF4CAF50/g, '0xFF2196F3');
                                }

                                fs.writeFileSync(targetFile, code, 'utf-8');
                                console.log(`Restored and fixed ${targetFile}`);
                            }
                        }
                    }
                }
            } catch (e) {
                // Ignore parsing errors for truncated lines
            }
        }
    }
}
