package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.Snippet

@Composable
fun DeleteSnippetDialog(
    snippet: Snippet,
    onDismiss: () -> Unit,
    onConfirm: (Boolean, Boolean, Boolean) -> Unit // onlyWorking, plusDrive, plusLocal
) {
    var removeWorking by remember { mutableStateOf(true) }
    var removeDrive by remember { mutableStateOf(false) }
    var removeLocal by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Snippet") },
        text = {
            Column {
                Text("Do you also want to remove this from Drive and local backup?")
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = removeWorking, onCheckedChange = { }, enabled = false)
                    Text("Working Library")
                }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = removeDrive, onCheckedChange = { removeDrive = it })
                    Text("Google Drive")
                }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = removeLocal, onCheckedChange = { removeLocal = it })
                    Text("Local Backup")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(removeWorking, removeDrive, removeLocal) }) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
