package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "snippets")
data class Snippet(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val category: String = "All Items",
    val isPinned: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
