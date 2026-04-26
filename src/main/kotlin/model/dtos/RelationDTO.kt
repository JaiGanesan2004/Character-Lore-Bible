package model.dtos

import kotlinx.serialization.Serializable
import model.enums.RelationType

@Serializable
data class RelationAdd(
    val sourceId: Int,
    val targetId: Int,
    val relation: RelationType
)

@Serializable
data class RelationResponse(
    val targetName: String,
    val relationType: RelationType,
    val description: String
)
