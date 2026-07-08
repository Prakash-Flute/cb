package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.backup.BackupManager
import com.example.data.Snippet
import com.example.data.SnippetRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val repository: SnippetRepository, private val backupManager: BackupManager, private val context: Context) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow("All Items")
    val selectedCategory: StateFlow<String> = _selectedCategory

    val categories: StateFlow<List<String>> = repository.allSnippets.map { snippets ->
        val dynamicCategories = snippets.map { it.category }
            .filter { it.isNotBlank() && it != "All Items" && it != "Pinned" }
            .distinct()
            .sorted()
        listOf("All Items", "Pinned") + dynamicCategories
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = listOf("All Items", "Pinned")
    )

    val uiState: StateFlow<List<Snippet>> = combine(
        repository.allSnippets,
        _searchQuery,
        _selectedCategory
    ) { snippets, query, category ->
        snippets.filter {
            (category == "All Items" || category == "Recent" || it.category == category) &&
            (it.title.contains(query, ignoreCase = true) || it.content.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true))
        }.let { filteredList -> 
            if (category == "Pinned") filteredList.filter { it.isPinned } else filteredList
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun saveSnippet(snippet: Snippet) {
        viewModelScope.launch {
            repository.insert(snippet)
            // Backup updates
            backupManager.updateSnippetLocal(snippet)
            val account = GoogleSignIn.getLastSignedInAccount(context)
            if (account != null) {
                backupManager.updateSnippetDrive(account, snippet)
            }
        }
    }

    fun deleteSnippet(id: Int, removeFromLocalBackup: Boolean = false, removeFromDrive: Boolean = false) {
        viewModelScope.launch {
            val snippet = repository.getAllSnippetsSync().find { it.id == id } ?: return@launch
            repository.deleteById(id)
            if (removeFromLocalBackup) {
                backupManager.deleteSnippetLocal(snippet)
            }
            if (removeFromDrive) {
                val account = GoogleSignIn.getLastSignedInAccount(context)
                if (account != null) {
                    backupManager.deleteSnippetDrive(account, snippet)
                }
            }
        }
    }

    suspend fun getAllSnippetsSync(): List<Snippet> {
        return repository.getAllSnippetsSync()
    }
}

class MainViewModelFactory(private val repository: SnippetRepository, private val backupManager: BackupManager, private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, backupManager, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
