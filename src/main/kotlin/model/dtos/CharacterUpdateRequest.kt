package model.dtos

import kotlinx.serialization.Serializable
import model.character.Archetype
import model.enums.Role

@Serializable
data class CharacterUpdateRequest(
    val role: Role? = null,
    val powerLevel: Int? = null,
    val abilities: List<String>? = null,
    val imageUrl: String? = null,
    val archetype: Archetype? = null,
    val race: String? = null,
    val age: Int? = null,
    val lore: String? = null
)
