package com.example.data

import kotlinx.coroutines.flow.Flow

class SnippetRepository(private val snippetDao: SnippetDao) {
    val allSnippets: Flow<List<Snippet>> = snippetDao.getAllSnippets()

    suspend fun insert(snippet: Snippet) {
        snippetDao.insertSnippet(snippet)
    }

    suspend fun deleteById(id: Int) {
        snippetDao.deleteSnippetById(id)
    }
    
    suspend fun getAllSnippetsSync(): List<Snippet> {
        return snippetDao.getAllSnippetsSync()
    }
}
