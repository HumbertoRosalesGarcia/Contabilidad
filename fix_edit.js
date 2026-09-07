const fs = require('fs');
let c = fs.readFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/ui/components/TransactionItem.kt','utf-8');

// Agregar import Edit
if (!c.includes('icons.filled.Edit')) {
    c = c.replace(
        'import androidx.compose.material.icons.filled.Delete',
        'import androidx.compose.material.icons.filled.Delete\nimport androidx.compose.material.icons.filled.Edit'
    );
}

// Reemplazar solo el IconButton de eliminar por Row con editar + eliminar
c = c.replace(
    `                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {\n                    Icon(Icons.Filled.Delete, "Eliminar", tint = Color.Gray, modifier = Modifier.size(18.dp))\n                }`,
    `                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {\n                    IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {\n                        Icon(Icons.Filled.Edit, "Editar", tint = Color.Gray, modifier = Modifier.size(18.dp))\n                    }\n                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {\n                        Icon(Icons.Filled.Delete, "Eliminar", tint = Color.Gray, modifier = Modifier.size(18.dp))\n                    }\n                }`
);

fs.writeFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/ui/components/TransactionItem.kt', c, 'utf-8');
console.log('OK - Edit icon added');
