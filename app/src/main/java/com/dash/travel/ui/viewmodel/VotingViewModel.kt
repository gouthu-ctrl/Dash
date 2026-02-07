package com.dash.travel.ui.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dash.travel.data.model.ItineraryStatus
import com.dash.travel.data.model.TripRole
import com.dash.travel.data.repository.TripRepository
import com.dash.travel.ui.screens.collaborative.VoteState
import com.dash.travel.ui.screens.collaborative.VoteType
import com.dash.travel.ui.screens.collaborative.VoteableItem
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import com.dash.travel.data.remote.SupabaseManager
import kotlinx.coroutines.delay

/**
 * ViewModel for managing voting on trip items
 */
class VotingViewModel(
    private val tripId: String,
    private val tripRepository: TripRepository
) : ViewModel() {

    val voteableItems = mutableStateListOf<VoteableItem>()
    private var currentUserId: String? = null

    init {
        currentUserId = SupabaseManager.client.auth.currentUserOrNull()?.id
        loadVoteableItems()
    }

    /**
     * Load items that can be voted on (proposed status items)
     */
    private fun loadVoteableItems() {
        viewModelScope.launch {
            try {
                // 1. Get proposed items from Repository
                val items = tripRepository.getItemsByStatus(tripId, ItineraryStatus.PROPOSED)
                
                // 2. Fetch all votes for the trip once
                val allVotes = tripRepository.getVotesForTrip(tripId)
                
                // 3. Map to UI model
                val uiItems = items.map { item ->
                    val votes = allVotes.filter { it.itineraryItemId == item.id }
                    
                    // a) Calculate counts
                    val upVotes = votes.count { it.voteType == "up" }
                    val downVotes = votes.count { it.voteType == "down" }

                    
                    // b) Check if current user voted
                    val myVote = currentUserId?.let { uid -> 
                         votes.find { it.userId == uid }?.voteType
                    }?.let { typeString ->
                        when (typeString) {
                            "up" -> VoteType.UPVOTE
                            "down" -> VoteType.DOWNVOTE
                            else -> null
                        }
                    }

                    VoteableItem(
                        id = item.id ?: "",
                        title = item.title,
                        subtitle = item.locationName ?: "",
                        imageUrl = null,
                        type = item.type,
                        voteState = VoteState(
                            upvotes = upVotes, 
                            downvotes = downVotes
                        ),
                        currentUserVote = myVote
                    )
                }
                
                voteableItems.clear()
                voteableItems.addAll(uiItems)

            } catch (e: Exception) {
                // Handle error
                e.printStackTrace()
            }
        }
    }

    fun vote(itemId: String, voteType: VoteType) {
        viewModelScope.launch {
            try {
                // Optimistic update
                updateLocalVote(itemId, voteType)
                
                // Api call - map VoteType enum to DB string
                val dbVoteType = when (voteType) {
                    VoteType.UPVOTE -> "up"
                    VoteType.DOWNVOTE -> "down"
                }
                val userId = currentUserId ?: return@launch
                tripRepository.castVote(itemId, userId, dbVoteType)
                
                // Refresh to ensure sync (or rely on Realtime if subscribed)
                loadVoteableItems()
            } catch (e: Exception) {
                // Revert or show error
                e.printStackTrace()
            }
        }
    }
    
    fun removeVote(itemId: String) {
        viewModelScope.launch {
            try {
                // Optimistic update
                updateLocalVote(itemId, null)
                
                tripRepository.deleteMyVote(itemId)
                
                // Refresh
                loadVoteableItems()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    // Helper for optimistic updates
    private fun updateLocalVote(itemId: String, newVote: VoteType?) {
        val index = voteableItems.indexOfFirst { it.id == itemId }
        if (index != -1) {
            val item = voteableItems[index]
            val oldVote = item.currentUserVote
            
            if (oldVote == newVote) return

            var upDelta = 0
            var downDelta = 0

            // Remove old vote effect
            when (oldVote) {
                VoteType.UPVOTE -> upDelta--
                VoteType.DOWNVOTE -> downDelta--
                null -> {}
            }

            // Add new vote effect
            when (newVote) {
                VoteType.UPVOTE -> upDelta++
                VoteType.DOWNVOTE -> downDelta++
                null -> {}
            }

            voteableItems[index] = item.copy(
                currentUserVote = newVote,
                voteState = item.voteState.copy(
                    upvotes = (item.voteState.upvotes + upDelta).coerceAtLeast(0),
                    downvotes = (item.voteState.downvotes + downDelta).coerceAtLeast(0)
                )
            )
        }
    }

    fun setUserId(id: String) {
        this.currentUserId = id
    }
}
