package model.dtos

import kotlinx.serialization.Serializable
import model.character.Archetype

@Serializable
data class ArchetypeCountDTO(
    val archetype: Archetype?,
    val count: Long
)