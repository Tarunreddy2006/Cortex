package com.cortex.app.database.dao

import androidx.room.*
import com.cortex.app.database.entities.CollectionEntity
import com.cortex.app.database.entities.DocumentEntity
import com.cortex.app.database.entities.NoteEntity

@Dao
interface KnowledgeDao {
    // Documents
    @Query("SELECT * FROM documents ORDER BY importedAt DESC")
    suspend fun getAllDocuments(): List<DocumentEntity>

    @Query("SELECT * FROM documents WHERE collectionId = :collectionId ORDER BY importedAt DESC")
    suspend fun getDocumentsByCollection(collectionId: String): List<DocumentEntity>

    @Query("SELECT * FROM documents WHERE category = :category ORDER BY importedAt DESC")
    suspend fun getDocumentsByCategory(category: String): List<DocumentEntity>

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: String): DocumentEntity?

    @Query("SELECT * FROM documents WHERE title LIKE '%' || :query || '%' OR extractedText LIKE '%' || :query || '%' OR tagsJson LIKE '%' || :query || '%' ORDER BY importedAt DESC")
    suspend fun searchDocuments(query: String): List<DocumentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDocument(doc: DocumentEntity)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocument(id: String): Int

    @Query("DELETE FROM documents WHERE sourceConnector = :sourceConnector")
    suspend fun deleteDocumentsBySource(sourceConnector: String): Int

    // Collections
    @Query("SELECT * FROM collections ORDER BY name ASC")
    suspend fun getAllCollections(): List<CollectionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCollection(collection: CollectionEntity)

    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun deleteCollection(id: String): Int

    // Notes
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    suspend fun getAllNotes(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE collectionId = :collectionId ORDER BY updatedAt DESC")
    suspend fun getNotesByCollection(collectionId: String): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    suspend fun searchNotes(query: String): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNote(id: String): Int
}
