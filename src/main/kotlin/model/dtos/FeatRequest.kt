package model.dtos

import kotlinx.serialization.Serializable
import model.enums.FeatLevel

@Serializable
data class FeatRequest(
    val category: FeatLevel,
    val description: String
)

@Serializable
data class FeatResponse(
    val category: FeatLevel,
    val description: String
)