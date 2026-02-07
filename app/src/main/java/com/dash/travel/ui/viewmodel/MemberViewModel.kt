package com.dash.travel.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dash.travel.data.model.TripRole
import com.dash.travel.data.repository.TripRepository
import com.dash.travel.ui.components.MemberRole
import com.dash.travel.ui.components.MemberStatus
import com.dash.travel.ui.components.TripMemberData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MemberViewModel(
    private val tripRepository: TripRepository
) : ViewModel() {

    private val _members = MutableStateFlow<List<TripMemberData>>(emptyList())
    val members = _members.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun loadMembers(tripId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val rawMembers = tripRepository.getTripMembersWithProfiles(tripId)
                _members.value = rawMembers.map { member ->
                    TripMemberData(
                        id = member.id,
                        name = member.profiles?.fullName ?: member.invitedEmail ?: "Unknown",
                        email = member.invitedEmail,
                        avatarUrl = member.profiles?.avatarUrl,
                        role = mapRoleToUi(member.role),
                        status = mapStatusToUi(member.status)
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun inviteMember(tripId: String, email: String, role: com.dash.travel.ui.components.MemberRole, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Map UI role to Domain role
                val domainRole = when (role) {
                    com.dash.travel.ui.components.MemberRole.OWNER -> TripRole.OWNER
                    com.dash.travel.ui.components.MemberRole.EDITOR -> TripRole.EDITOR
                    com.dash.travel.ui.components.MemberRole.VIEWER -> TripRole.VIEWER
                }
                
                tripRepository.inviteMember(tripId, email, domainRole)
                loadMembers(tripId)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to invite member")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateMemberRole(tripId: String, memberId: String, newRole: MemberRole) {
        viewModelScope.launch {
            try {
                tripRepository.updateMemberRole(memberId, mapRoleFromUi(newRole))
                loadMembers(tripId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun removeMember(tripId: String, memberId: String) {
        viewModelScope.launch {
            try {
                tripRepository.removeMember(memberId)
                loadMembers(tripId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun mapRoleToUi(role: TripRole): MemberRole {
        return when (role) {
            TripRole.OWNER -> MemberRole.OWNER
            TripRole.EDITOR -> MemberRole.EDITOR
            TripRole.VIEWER -> MemberRole.VIEWER
        }
    }

    private fun mapRoleFromUi(role: MemberRole): TripRole {
        return when (role) {
            MemberRole.OWNER -> TripRole.OWNER
            MemberRole.EDITOR -> TripRole.EDITOR
            MemberRole.VIEWER -> TripRole.VIEWER
        }
    }

    private fun mapStatusToUi(status: com.dash.travel.data.model.MemberStatus): MemberStatus {
        return when (status) {
            com.dash.travel.data.model.MemberStatus.ACCEPTED -> MemberStatus.ACCEPTED
            com.dash.travel.data.model.MemberStatus.PENDING -> MemberStatus.PENDING
            com.dash.travel.data.model.MemberStatus.DECLINED -> MemberStatus.DECLINED
        }
    }
}
