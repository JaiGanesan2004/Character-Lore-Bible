package model

import kotlinx.serialization.Serializable

@Serializable
data class Character(
    val id: Int? = null,
    val name:String,
    val role: Role,
    val powerLevel: Int?,
    val abilities: List<String>,
    val createdAt: String? = null,
    val imageUrl: String? = null
)