package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.Snippet
import com.example.data.SnippetRepository
import com.example.ui.CategorySidebar
import com.example.ui.MainViewModel
import com.example.ui.MainViewModelFactory
import com.example.ui.SnippetEditor
import com.example.ui.SnippetListScreen
import com.example.ui.SplashScreen
import com.example.ui.theme.MyApplicationTheme

import com.example.data.SecurityManager
import com.example.ui.LockScreen
import com.example.backup.BackupManager
import com.example.ui.SettingsScreen

class MainActivity : ComponentActivity() {
    private lateinit var database: AppDatabase
    private lateinit var repository: SnippetRepository
    private lateinit var backupManager: BackupManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        database = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "snippet_db").build()
        repository = SnippetRepository(database.snippetDao())
        backupManager = BackupManager(applicationContext)
        
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: MainViewModel = viewModel(factory = MainViewModelFactory(repository, backupManager, applicationContext))
                var showSplash by remember { mutableStateOf(true) }
                
                if (showSplash) {
                    SplashScreen { showSplash = false }
                } else {
                    MainAppScreen(viewModel = viewModel, backupManager = backupManager)
                }
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: MainViewModel, backupManager: com.example.backup.BackupManager) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val securityManager = remember { SecurityManager(context) }
    var isLocked by remember { mutableStateOf(securityManager.isLocked()) }
    var hasPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var isBubbleEnabled by remember { mutableStateOf(hasPermission) }
    
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = Settings.canDrawOverlays(context)
                if (securityManager.isLocked()) {
                    isLocked = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (isLocked) {
        LockScreen(
            securityManager = securityManager,
            onUnlock = {
                securityManager.unlock()
                isLocked = false
            }
        )
        return
    }

    if (hasPermission) {
        // Full App UI exactly like the floating panel
        var isSidebarOpen by remember { mutableStateOf(false) }
        var editingSnippet by remember { mutableStateOf<Snippet?>(null) }
        var isAdding by remember { mutableStateOf(false) }
        var showSettings by remember { mutableStateOf(false) }
        var showBackupExplorer by remember { mutableStateOf(false) }
        
        if (showBackupExplorer) {
            com.example.ui.BackupExplorerScreen(
                backupManager = backupManager,
                viewModel = viewModel,
                onClose = { showBackupExplorer = false }
            )
            return
        }

        if (showSettings) {
            SettingsScreen(
                securityManager = securityManager,
                backupManager = backupManager,
                viewModel = viewModel,
                onClose = { showSettings = false },
                onOpenBackupExplorer = { showSettings = false; showBackupExplorer = true }
            )
            return
        }

        Row(modifier = Modifier.fillMaxSize().padding(top = 24.dp)) { // EdgeToEdge padding
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
                        onClose = { /* Not used in main app */ },
                        isMainApp = true,
                        isBubbleEnabled = isBubbleEnabled,
                        onToggleBubble = { enable ->
                            isBubbleEnabled = enable
                            val intent = Intent(context, FloatingBubbleService::class.java)
                            if (enable) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    context.startForegroundService(intent)
                                } else {
                                    context.startService(intent)
                                }
                            } else {
                                context.stopService(intent)
                            }
                        },
                        onOpenSettings = { showSettings = true }
                    )
                }
            }
        }
        
        // Ensure service is running if enabled on startup
        LaunchedEffect(isBubbleEnabled) {
            val intent = Intent(context, FloatingBubbleService::class.java)
            if (isBubbleEnabled) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }

    } else {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Welcome to CopyBox",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "CopyBox uses a floating bubble so you can access and copy your snippets from any app on your phone.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }
                ) {
                    Text("Grant Overlay Permission")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = { hasPermission = Settings.canDrawOverlays(context) }) {
                    Text("I have granted the permission")
                }
            }
        }
    }
}

