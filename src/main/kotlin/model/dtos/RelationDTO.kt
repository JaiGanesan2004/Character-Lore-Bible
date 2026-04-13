package model.dtos

import model.enums.RelationType

data class RelationAdd(
    val sourceName: String,
    val targetName: String,
    val relation: RelationType
)

data class RelationResponse(
    val targetName: String,
    val relationType: RelationType,
    val description: String
)
