package org.example.biteshare.domain

data class VoteParticipant(
    val id: String,
    val name: String,
    val isSelf: Boolean = false,
)

data class VoteSession(
    val id: String,
    val title: String,
    val createdAtEpoch: Long,
    val participants: List<VoteParticipant>,
    val restaurants: List<Restaurant>,
    val votesByUserId: Map<String, Set<String>>,
    val isClosed: Boolean = false,
    val closedAtEpoch: Long? = null,
)
