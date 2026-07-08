package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.backup.BackupManager
import com.example.data.Snippet
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupExplorerScreen(
    backupManager: BackupManager,
    viewModel: MainViewModel,
    onClose: () -> Unit
) {
    var currentPath by remember { mutableStateOf(backupManager.getLocalBackupRoot()) }
    var files by remember { mutableStateOf(currentPath.listFiles()?.toList() ?: emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    
    LaunchedEffect(currentPath, searchQuery) {
        val allFiles = currentPath.listFiles()?.toList() ?: emptyList()
        files = if (searchQuery.isBlank()) {
            allFiles.sortedBy { it.name }
        } else {
            // Very naive search in names and contents if it's a file
            allFiles.filter { it.name.contains(searchQuery, true) || (it.isFile && it.readText().contains(searchQuery, true)) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (currentPath == backupManager.getLocalBackupRoot()) "Backups" else currentPath.name) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentPath != backupManager.getLocalBackupRoot()) {
                            currentPath = currentPath.parentFile ?: backupManager.getLocalBackupRoot()
                        } else {
                            onClose()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Backups") },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                singleLine = true
            )
            
            if (currentPath != backupManager.getLocalBackupRoot()) {
                Button(
                    onClick = {
                        // Import all in currentPath
                        currentPath.walkTopDown().filter { it.isFile && it.name.endsWith(".docx") }.forEach {
                            val content = it.readText()
                            val title = it.name.substringBeforeLast("_").substringBeforeLast(".docx")
                            val category = it.parentFile?.name ?: "Uncategorized"
                            val snippet = Snippet(title = title, content = content, category = category)
                            viewModel.saveSnippet(snippet)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text("Import All from ${currentPath.name}")
                }
            }

            LazyColumn {
                items(files) { file ->
                    ListItem(
                        headlineContent = { Text(file.name) },
                        leadingContent = {
                            Icon(
                                if (file.isDirectory) Icons.Filled.Folder else Icons.Filled.InsertDriveFile,
                                contentDescription = null
                            )
                        },
                        trailingContent = {
                            if (file.isFile) {
                                TextButton(onClick = {
                                    val content = file.readText()
                                    val title = file.name.substringBeforeLast("_").substringBeforeLast(".docx")
                                    val category = file.parentFile?.name ?: "Uncategorized"
                                    val snippet = Snippet(title = title, content = content, category = category)
                                    viewModel.saveSnippet(snippet)
                                }) {
                                    Text("Add")
                                }
                            }
                        },
                        modifier = Modifier.clickable {
                            if (file.isDirectory) {
                                currentPath = file
                            }
                        }
                    )
                    Divider()
                }
            }
        }
    }
}
