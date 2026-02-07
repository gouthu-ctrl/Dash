package com.dash.travel.ui.screens.collaborative

import androidx.compose.runtime.Immutable

@Immutable
data class VoteableItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val imageUrl: String?,
    val type: String,
    val voteState: VoteState,
    val currentUserVote: VoteType?
)

@Immutable
data class VoteState(
    val upvotes: Int = 0,
    val downvotes: Int = 0
)

enum class VoteType {
    UPVOTE,
    DOWNVOTE
}
