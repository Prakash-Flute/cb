package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.Snippet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnippetEditor(
    snippet: Snippet?,
    onSave: (Snippet) -> Unit,
    onClose: () -> Unit
) {
    var title by remember { mutableStateOf(snippet?.title ?: "") }
    var content by remember { mutableStateOf(snippet?.content ?: "") }
    var category by remember { mutableStateOf(snippet?.category ?: "All Items") }
    var isPinned by remember { mutableStateOf(snippet?.isPinned ?: false) }

    val isCode = content.contains("```") || content.contains("{")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (snippet == null) "New Snippet" else "Edit Snippet", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        onSave(
                            Snippet(
                                id = snippet?.id ?: 0,
                                title = title,
                                content = content,
                                category = category,
                                isPinned = isPinned,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }) {
                        Icon(Icons.Filled.Check, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = if (category == "All Items") "" else category,
                    onValueChange = { category = if (it.isEmpty()) "All Items" else it },
                    label = { Text("Category (Optional)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                
                FilterChip(
                    selected = isPinned,
                    onClick = { isPinned = !isPinned },
                    label = { Text("Pin") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Content") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = if (isCode) FontFamily.Monospace else FontFamily.Default
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}
