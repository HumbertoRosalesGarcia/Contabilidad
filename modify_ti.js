const fs = require('fs');
let content = fs.readFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/ui/components/TransactionItem.kt', 'utf-8');

// Agregar onEdit al constructor
content = content.replace(
    'fun TransactionItem(transaction: Transaction, onDelete: () -> Unit, onImageClick: (String) -> Unit)',
    'fun TransactionItem(transaction: Transaction, onDelete: () -> Unit, onEdit: () -> Unit = {}, onImageClick: (String) -> Unit)'
);

// Importar el icono de edición si no está
if (!content.includes('import androidx.compose.material.icons.filled.Edit')) {
    content = content.replace(
        'import androidx.compose.material.icons.filled.Delete',
        'import androidx.compose.material.icons.filled.Delete\nimport androidx.compose.material.icons.filled.Edit'
    );
}

// Agregar el botón de edición al lado del de eliminar
content = content.replace(
    'IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {\n                    Icon(Icons.Filled.Delete, "Eliminar", tint = Color.Gray, modifier = Modifier.size(18.dp))\n                }',
    'Row {\n                    IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {\n                        Icon(Icons.Filled.Edit, "Editar", tint = Color.Gray, modifier = Modifier.size(18.dp))\n                    }\n                    Spacer(modifier = Modifier.width(8.dp))\n                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {\n                        Icon(Icons.Filled.Delete, "Eliminar", tint = Color.Gray, modifier = Modifier.size(18.dp))\n                    }\n                }'
);

fs.writeFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/ui/components/TransactionItem.kt', content, 'utf-8');
console.log('TransactionItem updated');
