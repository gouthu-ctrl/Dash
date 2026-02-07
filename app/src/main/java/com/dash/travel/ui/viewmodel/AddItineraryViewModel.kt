package com.dash.travel.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dash.travel.data.model.DocCategory
import com.dash.travel.data.model.DocumentVaultItem
import com.dash.travel.data.model.ItineraryItem
import com.dash.travel.data.repository.DocumentRepository
import com.dash.travel.data.repository.TripRepository
import java.io.InputStream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class AddItineraryViewModel(
    private val tripRepository: TripRepository,
    private val documentRepository: DocumentRepository,
    private val itineraryDao: com.dash.travel.data.local.dao.ItineraryDao
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _editingItem = MutableStateFlow<ItineraryItem?>(null)
    val editingItem = _editingItem.asStateFlow()

    fun loadItem(itemId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _editingItem.value = tripRepository.getItineraryItem(itemId)
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addItem(
        item: ItineraryItem,
        fileUri: Uri?,
        fileStream: InputStream?,
        fileType: String?,
        fileName: String?,
        userId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Create or Update itinerary item
                val editingId = _editingItem.value?.id
                var savedItem = item
                
                val finalItemId = if (editingId != null) {
                    savedItem = item.copy(id = editingId)
                    tripRepository.updateItineraryItem(savedItem)
                    editingId
                } else {
                    val newId = item.id ?: java.util.UUID.randomUUID().toString()
                    savedItem = item.copy(id = newId)
                    tripRepository.createItineraryItem(savedItem)
                    newId
                }

                // 2. Handle file upload if present
                if (fileUri != null && fileStream != null && fileName != null) {
                    val storagePath = documentRepository.uploadFile(
                         fileName = fileName,
                         fileBytes = fileStream.readBytes(),
                         mimeType = fileType ?: "application/octet-stream",
                         userId = userId
                    )
                    
                    // Create attachment record linked to this item
                    val attachment = com.dash.travel.data.model.ItineraryAttachment(
                        id = java.util.UUID.randomUUID().toString(),
                        itineraryId = finalItemId,
                        fileName = fileName,
                        fileType = fileType,
                        storagePath = storagePath,
                        userId = userId
                    )
                    tripRepository.addItineraryAttachment(attachment)
                }

                // 3. Local-First Update: Sync with Room immediately with ALL details
                val entity = com.dash.travel.data.local.entity.ItineraryItemEntity(
                    id = finalItemId,
                    tripId = savedItem.tripId,
                    type = savedItem.type,
                    status = savedItem.status.name.lowercase(),
                    title = savedItem.title,
                    description = savedItem.description,
                    startTime = savedItem.startTime,
                    endTime = savedItem.endTime,
                    locationName = savedItem.locationName,
                    locationLat = savedItem.locationLat,
                    locationLng = savedItem.locationLng,
                    estimatedCost = savedItem.estimatedCost,
                    currency = savedItem.currency,
                    bookingRef = savedItem.bookingRef,
                    providerDetailsJson = savedItem.providerDetails?.toString(),
                    displayOrder = 0,
                    isDirty = false,
                    lastSyncedAt = System.currentTimeMillis()
                )
                itineraryDao.insertItem(entity)
                
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to add item")
            } finally {
                _isLoading.value = false
            }
        }
    }
    fun getAttachmentUrl(path: String, onUnlReady: (String) -> Unit) {
        viewModelScope.launch {
            try {
                // If path starts with http, it's already a URL (rare but possible if public)
                if (path.startsWith("http")) {
                    onUnlReady(path)
                    return@launch
                }
                
                // Get signed URL valid for 1 hour
                val url = documentRepository.getSignedUrl(path)
                onUnlReady(url)
            } catch (e: Exception) {
                // Handle error or return empty
            }
        }
    }
}


