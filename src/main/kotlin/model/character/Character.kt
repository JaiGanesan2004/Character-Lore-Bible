package model.character

import kotlinx.serialization.Serializable
import model.enums.Role

@Serializable
data class Character(
    val id: Int = 0,
    val name:String,
    val role: Role,
    val powerLevel: Int?,
    val abilities: List<String>,
    val createdAt: String? = null,
    val imageUrl: String? = null,
    val archetype: Archetype?,
    val race: String? = null,
    val age: Int? = null,
    val lore: String? = null,
    val userId: Int = -1
)