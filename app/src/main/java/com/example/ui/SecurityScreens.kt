package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.backup.BackupManager
import com.example.data.SecurityManager
import com.example.ui.MainViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch
import kotlin.random.Random
import android.content.Intent

@Composable
fun LockScreen(securityManager: SecurityManager, onUnlock: () -> Unit) {
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var isForgot by remember { mutableStateOf(false) }
    var otpMode by remember { mutableStateOf(false) }
    var otpCode by remember { mutableStateOf("") }
    var generatedOtp by remember { mutableStateOf("") }
    val context = LocalContext.current

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                try {
                    val smsManager = context.getSystemService(SmsManager::class.java)
                    generatedOtp = String.format("%04d", Random.nextInt(10000))
                    smsManager.sendTextMessage(securityManager.phoneNumber, null, "Your CopyBox OTP is: $generatedOtp", null, null)
                    otpMode = true
                    Toast.makeText(context, "OTP sent via SMS!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to send SMS", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "SMS Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (isForgot) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                if (!otpMode) {
                    Text("Forgot Password", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("We will send an OTP via SMS to: ${securityManager.phoneNumber}", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                            try {
                                val smsManager = context.getSystemService(SmsManager::class.java)
                                generatedOtp = String.format("%04d", Random.nextInt(10000))
                                smsManager.sendTextMessage(securityManager.phoneNumber, null, "Your CopyBox OTP is: $generatedOtp", null, null)
                                otpMode = true
                                Toast.makeText(context, "OTP sent via SMS!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed to send SMS: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                        }
                    }) {
                        Text("Send OTP")
                    }
                } else {
                    Text("Enter OTP", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = { otpCode = it; error = false },
                        label = { Text("4-digit OTP") },
                        singleLine = true,
                        isError = error
                    )
                    if (error) {
                        Text("Invalid OTP", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        if (otpCode == generatedOtp) {
                            securityManager.password = null // Reset password
                            Toast.makeText(context, "Password Reset Successfully", Toast.LENGTH_SHORT).show()
                            onUnlock()
                        } else {
                            error = true
                        }
                    }) {
                        Text("Verify & Reset")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = { isForgot = false; otpMode = false; error = false }) {
                    Text("Back to Login")
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(24.dp))
                Text("App Locked", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; error = false },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    isError = error,
                    singleLine = true
                )
                if (error) {
                    Text("Incorrect password", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        if (password == securityManager.password) {
                            onUnlock()
                        } else {
                            error = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(0.6f)
                ) {
                    Text("Unlock")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                if (!securityManager.phoneNumber.isNullOrEmpty()) {
                    TextButton(onClick = { isForgot = true; error = false }) {
                        Text("Forgot Password?")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(securityManager: SecurityManager, backupManager: BackupManager, viewModel: MainViewModel, onClose: () -> Unit, onOpenBackupExplorer: () -> Unit) {
    var password by remember { mutableStateOf(securityManager.password ?: "") }
    var phone by remember { mutableStateOf(securityManager.phoneNumber ?: "") }
    var rememberMe by remember { mutableStateOf(securityManager.rememberMe) }
    
    val context = LocalContext.current
    var account by remember { mutableStateOf(GoogleSignIn.getLastSignedInAccount(context)) }
    val scope = rememberCoroutineScope()
    var isBackingUp by remember { mutableStateOf(false) }
    var showScanFlow by remember { mutableStateOf(false) }

    if (showScanFlow && account != null) {
        ScanBackupDialog(
            backupManager = backupManager,
            account = account!!,
            onDismiss = { showScanFlow = false },
            onRestore = { restoreLocal, restoreDrive ->
                showScanFlow = false
                if (restoreLocal || restoreDrive) {
                    onOpenBackupExplorer()
                }
            }
        )
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                account = task.getResult(ApiException::class.java)
                Toast.makeText(context, "Signed in as ${account?.email}", Toast.LENGTH_SHORT).show()
                showScanFlow = true
            } catch (e: ApiException) {
                Toast.makeText(context, "Sign in failed: ${e.statusCode}", Toast.LENGTH_SHORT).show()
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("Security", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password (leave empty to disable)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number (+CountryCode...)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it })
                Spacer(modifier = Modifier.width(8.dp))
                Text("Remember me for 12 hours")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    securityManager.password = password.ifEmpty { null }
                    securityManager.phoneNumber = phone.ifEmpty { null }
                    securityManager.rememberMe = rememberMe
                    Toast.makeText(context, "Settings saved", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Security Settings")
            }

            Spacer(modifier = Modifier.height(32.dp))
            Divider()
            Spacer(modifier = Modifier.height(32.dp))
            
            Text("Backup & Restore", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            if (account == null) {
                Button(
                    onClick = {
                        val client = backupManager.getGoogleSignInClient()
                        googleSignInLauncher.launch(client.signInIntent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Continue with Google")
                }
            } else {
                Text("Signed in as ${account?.email}")
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val client = backupManager.getGoogleSignInClient()
                        client.signOut().addOnCompleteListener {
                            account = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sign out")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    scope.launch {
                        isBackingUp = true
                        val snippets = viewModel.getAllSnippetsSync()
                        backupManager.createFullLocalBackup(snippets)
                        if (account != null) {
                            backupManager.createFullDriveBackup(account!!, snippets)
                        }
                        isBackingUp = false
                        Toast.makeText(context, "Backup Complete!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isBackingUp
            ) {
                Text(if (isBackingUp) "Backing up..." else "Backup Now (Drive + Local)")
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onOpenBackupExplorer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Backup Explorer")
            }
        }
    }
}
