import json
import os
import glob

def restore_from_transcript(conv_id):
    path = f"C:/Users/Administrador/.gemini/antigravity/brain/{conv_id}/.system_generated/logs/transcript_full.jsonl"
    if not os.path.exists(path):
        path = f"C:/Users/Administrador/.gemini/antigravity/brain/{conv_id}/.system_generated/logs/transcript.jsonl"
        
    with open(path, 'r', encoding='utf-8') as f:
        for line in f:
            data = json.loads(line)
            if 'tool_calls' in data:
                for tc in data['tool_calls']:
                    if tc['function']['name'] == 'default_api:write_to_file':
                        args = json.loads(tc['function']['arguments'])
                        target_file = args['TargetFile']
                        code = args['CodeContent']
                        # Fix the incorrect imports while restoring
                        code = code.replace("import com.xxcamixx.contabilidad.network.ChatMessage", "import com.xxcamixx.contabilidad.model.ChatMessage")
                        code = code.replace("import com.xxcamixx.contabilidad.network.ChatSendRequest", "import com.xxcamixx.contabilidad.model.ChatSendRequest")
                        code = code.replace("import com.xxcamixx.contabilidad.network.UserData", "import com.xxcamixx.contabilidad.model.UserData")
                        code = code.replace("import com.xxcamixx.contabilidad.network.UserManageRequest", "import com.xxcamixx.contabilidad.model.UserManageRequest")
                        code = code.replace("import com.xxcamixx.contabilidad.network.UserSyncRequest", "import com.xxcamixx.contabilidad.model.UserSyncRequest")
                        code = code.replace("import com.xxcamixx.contabilidad.network.UserTimeRequest", "import com.xxcamixx.contabilidad.model.UserTimeRequest")
                        
                        # Fix lastActive in UserData
                        code = code.replace('val profileImage: String? = null)', 'val profileImage: String? = null, val lastActive: Long = 0L)')
                        
                        # Fix the ProductInfoDialog import in FinanceScreen
                        code = code.replace("import com.xxcamixx.contabilidad.ui.dialogs.ProductInfoDialog", "import com.xxcamixx.contabilidad.ui.components.ProductInfoDialog")

                        with open(target_file, 'w', encoding='utf-8') as out_f:
                            out_f.write(code)
                        print(f"Restored {target_file}")

# Screen files creator
restore_from_transcript("364ee6c9-427b-4530-a9d8-96f788771781")
# Complex dialogs creator
restore_from_transcript("c5125252-78c8-4318-b7df-37e539a17828")
# Model and utility file creator
restore_from_transcript("dca58b67-9b6c-4353-b144-9742537d3b02")

