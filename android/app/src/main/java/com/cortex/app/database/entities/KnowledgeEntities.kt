package com.cortex.app.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "documents",
    indices = [
        Index(value = ["category"]),
        Index(value = ["fileType"]),
        Index(value = ["collectionId"]),
        Index(value = ["importedAt"])
    ]
)
data class DocumentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val fileName: String,
    val fileType: String, // PDF, DOCX, XLSX, PPTX, TXT, MD, IMAGE, URL, NOTE, CHAT_EXPORT
    val fileSizeBytes: Long = 0,
    val storagePath: String = "",
    val extractedText: String? = null,
    val summary: String? = null,
    val category: String = "GENERAL",
    val tagsJson: String = "[]",
    val collectionId: String? = null,
    val sourceConnector: String = "knowledge_vault",
    val version: Int = 1,
    val importedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String? = null,
    val colorHex: String = "#3B82F6",
    val icon: String = "folder",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["collectionId"]),
        Index(value = ["createdAt"])
    ]
)
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val collectionId: String? = null,
    val tagsJson: String = "[]",
    val linkedDocumentIdsJson: String = "[]",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
