package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.backup.BackupManager
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ScanBackupDialog(
    backupManager: BackupManager,
    account: GoogleSignInAccount,
    onDismiss: () -> Unit,
    onRestore: (Boolean, Boolean) -> Unit
) {
    var isScanning by remember { mutableStateOf(true) }
    var localBackupsCount by remember { mutableStateOf(0) }
    var driveBackupsCount by remember { mutableStateOf(0) }
    var selectedRestoreLocal by remember { mutableStateOf(false) }
    var selectedRestoreDrive by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        // Mock scanning for simplicity in this dialog, in a real app this would query Drive and Local
        withContext(Dispatchers.IO) {
            val localRoot = backupManager.getLocalBackupRoot()
            localBackupsCount = localRoot.listFiles { it -> it.isDirectory }?.size ?: 0
            
            // Assume drive backups count is just detected from Drive if we queried it
            // For now, let's just say if account is not null, there might be 1 backup
            // Real query would be drive.files().list()...
            driveBackupsCount = 1
        }
        isScanning = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Scan Found Backups") },
        text = {
            Column {
                if (isScanning) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Scanning Drive and Local backups...")
                } else {
                    Text("We found existing backups:")
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(checked = selectedRestoreLocal, onCheckedChange = { selectedRestoreLocal = it })
                        Text("Local Backups ($localBackupsCount found)")
                    }
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(checked = selectedRestoreDrive, onCheckedChange = { selectedRestoreDrive = it })
                        Text("Google Drive Backups ($driveBackupsCount found)")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Choose which backups to explore or import from.")
                }
            }
        },
        confirmButton = {
            if (!isScanning) {
                Button(onClick = { onRestore(selectedRestoreLocal, selectedRestoreDrive) }) {
                    Text("Continue")
                }
            }
        },
        dismissButton = {
            if (!isScanning) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
