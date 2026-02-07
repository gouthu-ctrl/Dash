package com.dash.travel.data.repository

import com.dash.travel.data.model.DocCategory
import com.dash.travel.data.model.DocumentVaultItem
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.*
import kotlin.time.Duration.Companion.seconds

/**
 * Repository for Document Vault operations with Supabase
 */
class DocumentRepository(private val supabase: SupabaseClient) {

    companion object {
        private const val BUCKET_NAME = "documents"
        private const val TABLE_NAME = "document_vault"
    }

    // ==================== DOCUMENT METADATA ====================

    /**
     * Get all documents for current user
     */
    suspend fun getAllDocuments(): List<DocumentVaultItem> {
        return supabase.from(TABLE_NAME)
            .select {
                order("created_at", Order.DESCENDING)
            }
            .decodeList()
    }

    /**
     * Get documents for a specific trip
     */
    suspend fun getDocumentsForTrip(tripId: String): List<DocumentVaultItem> {
        return supabase.from(TABLE_NAME)
            .select {
                filter { eq("trip_id", tripId) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList()
    }

    /**
     * Get documents by category
     */
    suspend fun getDocumentsByCategory(category: DocCategory): List<DocumentVaultItem> {
        return supabase.from(TABLE_NAME)
            .select {
                filter { eq("doc_type", category.name.lowercase()) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList()
    }

    /**
     * Get documents expiring soon (within next 90 days)
     */
    suspend fun getExpiringDocuments(): List<DocumentVaultItem> {
        val today = java.time.LocalDate.now()
        val futureDate = today.plusDays(90)
        
        return supabase.from(TABLE_NAME)
            .select {
                filter {
                    lte("expiry_date", futureDate.toString())
                    gte("expiry_date", today.toString())
                }
                order("expiry_date", Order.ASCENDING)
            }
            .decodeList()
    }

    /**
     * Create document metadata record
     */
    suspend fun createDocument(document: DocumentVaultItem): DocumentVaultItem {
        val documentToInsert = if (document.id == null) {
            document.copy(id = java.util.UUID.randomUUID().toString())
        } else {
            document
        }
        return supabase.from(TABLE_NAME)
            .insert(documentToInsert) {
                select()
            }
            .decodeSingle()
    }

    /**
     * Update document metadata
     */
    suspend fun updateDocument(documentId: String, updates: Map<String, Any?>) {
        supabase.from(TABLE_NAME)
            .update(updates) {
                filter { eq("id", documentId) }
            }
    }

    /**
     * Delete document (metadata and file)
     */
    suspend fun deleteDocument(documentId: String, storagePath: String) {
        // Delete file from storage first
        supabase.storage.from(BUCKET_NAME).delete(storagePath)
        
        // Then delete metadata
        supabase.from(TABLE_NAME)
            .delete {
                filter { eq("id", documentId) }
            }
    }

    // ==================== FILE STORAGE ====================

    /**
     * Upload file to Supabase Storage
     */
    suspend fun uploadFile(
        fileName: String,
        fileBytes: ByteArray,
        mimeType: String,
        userId: String
    ): String {
        // Sanitize filename: remove non-alphanumeric characters (except dots, hyphens, underscores)
        val sanitizedFileName = fileName.replace(Regex("[^a-zA-Z0-9.\\-_]"), "_")
        val path = "$userId/${System.currentTimeMillis()}_$sanitizedFileName"
        
        try {
            supabase.storage.from(BUCKET_NAME).upload(
                path = path,
                data = fileBytes
            )
        } catch (e: Exception) {
            // Check for "Bucket not found" error
            val msg = e.message ?: ""
            if (msg.contains("Bucket not found", ignoreCase = true) || msg.contains("not found", ignoreCase = true)) {
                try {
                    // Try to create the bucket (private by default)
                    supabase.storage.createBucket(id = BUCKET_NAME)
                    // Retry upload
                    supabase.storage.from(BUCKET_NAME).upload(
                        path = path,
                        data = fileBytes
                    )
                } catch (createError: Exception) {
                    throw Exception("Failed to create bucket: ${createError.message}. Original error: $msg")
                }
            } else {
                throw e
            }
        }
        
        return path
    }

    /**
     * Download file from Supabase Storage
     */
    suspend fun downloadFile(storagePath: String): ByteArray {
        return supabase.storage.from(BUCKET_NAME).downloadAuthenticated(storagePath)
    }

    /**
     * Get public URL for file (if bucket is public)
     */
    fun getPublicUrl(storagePath: String): String {
        return supabase.storage.from(BUCKET_NAME).publicUrl(storagePath)
    }



    /**
     * Get signed URL for private file access
     */
    suspend fun getSignedUrl(storagePath: String, expiresInSeconds: Long = 3600): String {
        return supabase.storage.from(BUCKET_NAME)
            .createSignedUrl(storagePath, expiresInSeconds.seconds)
    }
}
