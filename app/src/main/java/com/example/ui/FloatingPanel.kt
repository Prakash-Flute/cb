package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.data.Snippet
import kotlin.math.roundToInt

@Composable
fun FloatingBubble(onClick: () -> Unit, onDrag: ((Float, Float) -> Unit)? = null) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .pointerInput(Unit) {
                if (onDrag != null) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x, dragAmount.y)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Just the logo, transparent bubble
        Icon(
            imageVector = Icons.Filled.ContentCopy,
            contentDescription = "Open CopyBox",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp)
        )
    }
}

@Composable
fun FloatingPanel(viewModel: MainViewModel, onClose: () -> Unit) {
    var isSidebarOpen by remember { mutableStateOf(false) }
    var editingSnippet by remember { mutableStateOf<Snippet?>(null) }
    var isAdding by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .fillMaxHeight(0.82f)
            .shadow(24.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(enabled = false) {} // block clicks from closing panel
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = isSidebarOpen,
                enter = expandHorizontally(expandFrom = Alignment.Start),
                exit = shrinkHorizontally(shrinkTowards = Alignment.Start)
            ) {
                CategorySidebar(viewModel, onClose = { isSidebarOpen = false })
            }

            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                if (editingSnippet != null || isAdding) {
                    SnippetEditor(
                        snippet = editingSnippet,
                        onSave = {
                            viewModel.saveSnippet(it)
                            editingSnippet = null
                            isAdding = false
                        },
                        onClose = {
                            editingSnippet = null
                            isAdding = false
                        }
                    )
                } else {
                    SnippetListScreen(
                        viewModel = viewModel,
                        onOpenSidebar = { isSidebarOpen = !isSidebarOpen },
                        onEdit = { editingSnippet = it },
                        onAdd = { isAdding = true },
                        onClose = onClose
                    )
                }
            }
        }
    }
}
