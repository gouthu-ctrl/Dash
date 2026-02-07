package com.dash.travel.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dash.travel.data.model.MemberStatus
import com.dash.travel.data.model.TripRole
import com.dash.travel.data.repository.TripRepository
import com.dash.travel.ui.components.MemberRole
import com.dash.travel.ui.components.TripMemberData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MemberManagementViewModel(
    private val tripRepository: TripRepository
) : ViewModel() {

    private val _members = MutableStateFlow<List<TripMemberData>>(emptyList())
    val members = _members.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun loadMembers(tripId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val dbMembers = tripRepository.getTripMembersWithProfiles(tripId)
                _members.value = dbMembers.map { 
                    TripMemberData(
                        id = it.id,
                        name = it.profiles?.fullName ?: it.invitedEmail ?: "Unknown",
                        email = it.invitedEmail,
                        avatarUrl = it.profiles?.avatarUrl,
                        role = mapTripRoleToMemberRole(it.role),
                        status = mapDbStatusToMemberStatus(it.status)
                    )
                }
            } catch (e: Exception) {
                _error.value = "Failed to load members: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun inviteMember(tripId: String, email: String, role: MemberRole) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val dbRole = when(role) {
                    MemberRole.OWNER -> TripRole.OWNER
                    MemberRole.EDITOR -> TripRole.EDITOR
                    MemberRole.VIEWER -> TripRole.VIEWER
                }
                tripRepository.inviteMember(tripId, email, dbRole)
                loadMembers(tripId)
            } catch (e: Exception) {
                _error.value = "Failed to invite: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateRole(memberId: String, newRole: MemberRole) {
        viewModelScope.launch {
             try {
                 val dbRole = when(newRole) {
                     MemberRole.OWNER -> TripRole.OWNER
                     MemberRole.EDITOR -> TripRole.EDITOR
                     MemberRole.VIEWER -> TripRole.VIEWER
                 }
                 tripRepository.updateMemberRole(memberId, dbRole)
                 // Optimistic update
                 _members.value = _members.value.map { 
                     if (it.id == memberId) it.copy(role = newRole) else it
                 }
             } catch (e: Exception) {
                 _error.value = "Failed to update role"
             }
        }
    }

    fun removeMember(tripId: String, memberId: String) {
        viewModelScope.launch {
            try {
                tripRepository.removeMember(memberId)
                // Optimistic remove
                _members.value = _members.value.filter { it.id != memberId }
                loadMembers(tripId) // Sync to be sure
            } catch (e: Exception) {
                 _error.value = "Failed to remove member"
            }
        }
    }

    private fun mapTripRoleToMemberRole(role: TripRole): MemberRole {
        return when (role) {
            TripRole.OWNER -> MemberRole.OWNER
            TripRole.EDITOR -> MemberRole.EDITOR
            TripRole.VIEWER -> MemberRole.VIEWER
        }
    }

    private fun mapDbStatusToMemberStatus(status: MemberStatus): com.dash.travel.ui.components.MemberStatus {
        return when (status) {
            MemberStatus.ACCEPTED -> com.dash.travel.ui.components.MemberStatus.ACCEPTED
            MemberStatus.PENDING -> com.dash.travel.ui.components.MemberStatus.PENDING
            MemberStatus.DECLINED -> com.dash.travel.ui.components.MemberStatus.DECLINED
        }
    }
}
