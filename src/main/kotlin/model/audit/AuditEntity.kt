package model.audit

import kotlinx.serialization.Serializable

@Serializable
data class AuditEntry(
    val id: Int,
    val username: String,
    val action: String,
    val characterName: String,
    val timestamp: Long
)
