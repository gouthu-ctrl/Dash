package com.dash.travel.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dash.travel.data.model.DocCategory
import com.dash.travel.data.model.DocumentVaultItem
import com.dash.travel.data.repository.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DocumentVaultViewModel(
    private val documentRepository: DocumentRepository,
    private val userId: String // In real app, injected or retrieved
) : ViewModel() {

    private val _documents = MutableStateFlow<List<DocumentVaultItem>>(emptyList())
    val documents = _documents.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun clearError() { _error.value = null }

    fun loadDocuments(tripId: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                if (tripId != null) {
                    _documents.value = documentRepository.getDocumentsForTrip(tripId)
                } else {
                    _documents.value = documentRepository.getAllDocuments()
                }
            } catch (e: Exception) {
                _error.value = "Failed to load documents: ${e.message}"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun uploadDocument(
        fileName: String,
        fileBytes: ByteArray,
        mimeType: String,
        category: DocCategory,
        tripId: String?
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Upload file
                val storagePath = documentRepository.uploadFile(fileName, fileBytes, mimeType, userId)
                
                // Create metadata
                val doc = DocumentVaultItem(
                    fileName = fileName,
                    storagePath = storagePath,
                    docType = category,
                    tripId = tripId,
                    userId = userId
                )
                documentRepository.createDocument(doc)
                
                // Refresh
                loadDocuments(tripId)
            } catch (e: Exception) {
                _error.value = "Upload failed: ${e.message}"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
