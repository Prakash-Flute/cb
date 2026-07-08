package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SnippetDao {
    @Query("SELECT * FROM snippets ORDER BY isPinned DESC, timestamp DESC")
    fun getAllSnippets(): Flow<List<Snippet>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnippet(snippet: Snippet)

    @Query("DELETE FROM snippets WHERE id = :id")
    suspend fun deleteSnippetById(id: Int)

    @Query("SELECT * FROM snippets ORDER BY isPinned DESC, timestamp DESC")
    suspend fun getAllSnippetsSync(): List<Snippet>
}
