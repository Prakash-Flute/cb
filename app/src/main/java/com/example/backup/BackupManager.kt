package com.example.backup

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.data.Snippet
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.R

class BackupManager(private val context: Context) {

    fun getGoogleSignInClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    private fun getDriveService(account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_FILE)
        )
        credential.selectedAccount = account.account
        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("CopyBox").build()
    }

    // Local Backup Logic
    fun getLocalBackupRoot(): File {
        val docs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val copyBox = File(docs, "CopyBox/Backups")
        if (!copyBox.exists()) copyBox.mkdirs()
        return copyBox
    }
    
    // Returns the latest backup folder, or creates one if none exists
    private fun getLatestLocalBackupDir(): File {
        val root = getLocalBackupRoot()
        val dirs = root.listFiles { it -> it.isDirectory }?.sortedByDescending { it.name }
        if (dirs.isNullOrEmpty()) {
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
            val newDir = File(root, timestamp)
            newDir.mkdirs()
            return newDir
        }
        return dirs[0]
    }

    suspend fun createFullLocalBackup(snippets: List<Snippet>) = withContext(Dispatchers.IO) {
        val root = getLocalBackupRoot()
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        val backupDir = File(root, timestamp)
        if (!backupDir.exists()) backupDir.mkdirs()

        for (snippet in snippets) {
            val catDir = File(backupDir, snippet.category.ifEmpty { "Uncategorized" })
            if (!catDir.exists()) catDir.mkdirs()
            val fileName = "${snippet.title.ifEmpty { "Untitled" }}_${snippet.id}.docx"
            val file = File(catDir, fileName)
            file.writeText(snippet.content)
        }
    }

    suspend fun updateSnippetLocal(snippet: Snippet) = withContext(Dispatchers.IO) {
        val backupDir = getLatestLocalBackupDir()
        val catDir = File(backupDir, snippet.category.ifEmpty { "Uncategorized" })
        if (!catDir.exists()) catDir.mkdirs()
        val fileName = "${snippet.title.ifEmpty { "Untitled" }}_${snippet.id}.docx"
        val file = File(catDir, fileName)
        file.writeText(snippet.content)
    }

    suspend fun deleteSnippetLocal(snippet: Snippet) = withContext(Dispatchers.IO) {
        val backupDir = getLatestLocalBackupDir()
        val catDir = File(backupDir, snippet.category.ifEmpty { "Uncategorized" })
        val fileName = "${snippet.title.ifEmpty { "Untitled" }}_${snippet.id}.docx"
        val file = File(catDir, fileName)
        if (file.exists()) {
            file.delete()
        }
    }

    // Drive Backup Logic
    suspend fun createFullDriveBackup(account: GoogleSignInAccount, snippets: List<Snippet>) = withContext(Dispatchers.IO) {
        try {
            val drive = getDriveService(account)
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())

            val copyBoxFolderId = getOrCreateDriveFolder(drive, "CopyBox", "root")
            val backupsFolderId = getOrCreateDriveFolder(drive, "Backups", copyBoxFolderId)
            val currentBackupFolderId = getOrCreateDriveFolder(drive, timestamp, backupsFolderId)

            for (snippet in snippets) {
                val catFolderId = getOrCreateDriveFolder(drive, snippet.category.ifEmpty { "Uncategorized" }, currentBackupFolderId)
                val fileName = "${snippet.title.ifEmpty { "Untitled" }}_${snippet.id}.docx"
                createDriveFile(drive, fileName, snippet.content, catFolderId)
            }
        } catch (e: Exception) {
            Log.e("BackupManager", "Drive backup failed", e)
        }
    }

    suspend fun updateSnippetDrive(account: GoogleSignInAccount, snippet: Snippet) = withContext(Dispatchers.IO) {
        try {
            val drive = getDriveService(account)
            val copyBoxFolderId = getOrCreateDriveFolder(drive, "CopyBox", "root")
            val backupsFolderId = getOrCreateDriveFolder(drive, "Backups", copyBoxFolderId)
            val currentBackupFolderId = getLatestDriveBackupFolder(drive, backupsFolderId) ?: getOrCreateDriveFolder(drive, SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date()), backupsFolderId)
            
            val catFolderId = getOrCreateDriveFolder(drive, snippet.category.ifEmpty { "Uncategorized" }, currentBackupFolderId)
            val fileName = "${snippet.title.ifEmpty { "Untitled" }}_${snippet.id}.docx"
            createDriveFile(drive, fileName, snippet.content, catFolderId)
        } catch (e: Exception) {
            Log.e("BackupManager", "Drive update failed", e)
        }
    }

    suspend fun deleteSnippetDrive(account: GoogleSignInAccount, snippet: Snippet) = withContext(Dispatchers.IO) {
        try {
            val drive = getDriveService(account)
            val copyBoxFolderId = getOrCreateDriveFolder(drive, "CopyBox", "root")
            val backupsFolderId = getOrCreateDriveFolder(drive, "Backups", copyBoxFolderId)
            val currentBackupFolderId = getLatestDriveBackupFolder(drive, backupsFolderId) ?: return@withContext
            val catFolderId = getOrCreateDriveFolder(drive, snippet.category.ifEmpty { "Uncategorized" }, currentBackupFolderId)
            val fileName = "${snippet.title.ifEmpty { "Untitled" }}_${snippet.id}.docx"
            
            val query = "name='$fileName' and '$catFolderId' in parents and trashed=false"
            val existing = drive.files().list().setQ(query).setSpaces("drive").execute().files
            if (existing != null && existing.isNotEmpty()) {
                drive.files().delete(existing[0].id).execute()
            }
        } catch (e: Exception) {
            Log.e("BackupManager", "Drive delete failed", e)
        }
    }

    private fun getLatestDriveBackupFolder(drive: Drive, backupsFolderId: String): String? {
        val query = "'$backupsFolderId' in parents and mimeType='application/vnd.google-apps.folder' and trashed=false"
        val files = drive.files().list().setQ(query).setSpaces("drive").setOrderBy("name desc").execute().files
        if (files != null && files.isNotEmpty()) {
            return files[0].id
        }
        return null
    }

    private fun getOrCreateDriveFolder(drive: Drive, name: String, parentId: String): String {
        val query = "name='$name' and '$parentId' in parents and mimeType='application/vnd.google-apps.folder' and trashed=false"
        val files = drive.files().list().setQ(query).setSpaces("drive").execute().files
        if (files != null && files.isNotEmpty()) {
            return files[0].id
        }
        val fileMetadata = com.google.api.services.drive.model.File().apply {
            this.name = name
            this.mimeType = "application/vnd.google-apps.folder"
            if (parentId != "root") {
                this.parents = listOf(parentId)
            }
        }
        val file = drive.files().create(fileMetadata).setFields("id").execute()
        return file.id
    }

    private fun createDriveFile(drive: Drive, name: String, content: String, parentId: String) {
        val query = "name='$name' and '$parentId' in parents and trashed=false"
        val existing = drive.files().list().setQ(query).setSpaces("drive").execute().files
        
        val fileContent = com.google.api.client.http.ByteArrayContent.fromString("text/plain", content)
        if (existing != null && existing.isNotEmpty()) {
            val fileId = existing[0].id
            val fileMetadata = com.google.api.services.drive.model.File()
            drive.files().update(fileId, fileMetadata, fileContent).execute()
        } else {
            val fileMetadata = com.google.api.services.drive.model.File().apply {
                this.name = name
                this.parents = listOf(parentId)
            }
            drive.files().create(fileMetadata, fileContent).execute()
        }
    }
}
