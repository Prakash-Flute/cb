package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Snippet
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnippetListScreen(
    viewModel: MainViewModel,
    onOpenSidebar: () -> Unit,
    onEdit: (Snippet) -> Unit,
    onAdd: () -> Unit,
    onClose: () -> Unit,
    isMainApp: Boolean = false,
    isBubbleEnabled: Boolean = false,
    onToggleBubble: ((Boolean) -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null
) {
    val snippets by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    
    var snippetToDelete by remember { mutableStateOf<Snippet?>(null) }
    
    if (snippetToDelete != null) {
        DeleteSnippetDialog(
            snippet = snippetToDelete!!,
            onDismiss = { snippetToDelete = null },
            onConfirm = { _, removeDrive, removeLocal ->
                viewModel.deleteSnippet(snippetToDelete!!.id, removeFromLocalBackup = removeLocal, removeFromDrive = removeDrive)
                snippetToDelete = null
            }
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(selectedCategory, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onOpenSidebar) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        if (isMainApp && onToggleBubble != null) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                                Text("Bubble", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(
                                    checked = isBubbleEnabled,
                                    onCheckedChange = onToggleBubble
                                )
                                if (onOpenSettings != null) {
                                    IconButton(onClick = onOpenSettings) {
                                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                                    }
                                }
                            }
                        } else {
                            IconButton(onClick = onClose) {
                                Icon(Icons.Filled.Close, contentDescription = "Close")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                SearchBarUI(searchQuery, viewModel::updateSearchQuery)
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Filled.Add, contentDescription = "Add Snippet")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        if (snippets.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Inbox, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No snippets found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(snippets, key = { it.id }) { snippet ->
                    SnippetCard(
                        snippet = snippet,
                        onClick = { onEdit(snippet) },
                        onCopy = { /* Handled inside SnippetCard */ },
                        onTogglePin = { viewModel.saveSnippet(snippet.copy(isPinned = !snippet.isPinned)) },
                        onDelete = { snippetToDelete = snippet }
                    )
                }
            }
        }
    }
}

@Composable
fun SearchBarUI(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        placeholder = { Text("Search snippets...") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = "Clear")
                }
            }
        },
        shape = RoundedCornerShape(100), // fully rounded
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
            focusedBorderColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
fun SnippetCard(
    snippet: Snippet,
    onClick: () -> Unit,
    onCopy: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var expandedMenu by remember { mutableStateOf(false) }
    val isCode = snippet.content.contains("```") || snippet.content.contains("{")

    val containerColor = if (isCode && snippet.isPinned) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }
    
    val borderColor = if (isCode && snippet.isPinned) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    } else {
        androidx.compose.ui.graphics.Color.White.copy(alpha = 0.05f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (snippet.isPinned) {
                            Icon(Icons.Filled.PushPin, contentDescription = "Pinned", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = snippet.title.ifEmpty { "Untitled" },
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            maxLines = 1,
                            color = if (snippet.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isCode) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text("CODE", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(snippet.content.replace(Regex("```[a-z]*\n|```"), "").trim()))
                            onCopy()
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .background(if (isCode && snippet.isPinned) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", tint = if (isCode && snippet.isPinned) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Box {
                        IconButton(onClick = { expandedMenu = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More Options", modifier = Modifier.size(20.dp))
                        }
                        DropdownMenu(
                            expanded = expandedMenu,
                            onDismissRequest = { expandedMenu = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (snippet.isPinned) "Unpin" else "Pin") },
                                onClick = { onTogglePin(); expandedMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = { onClick(); expandedMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Copy Plain Text") },
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(snippet.content.replace(Regex("```[a-z]*\n|```"), "").trim()))
                                    expandedMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                onClick = { onDelete(); expandedMenu = false }
                            )
                        }
                    }
                }
            }
        }
    }
}
