import database.CharacterTable
import database.RelationshipTable
import model.dtos.RelationAdd
import model.dtos.RelationResponse
import model.enums.RelationType
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object RelationshipService {
    // Forge the bond
    fun addRelationship(req: RelationAdd): Boolean = transaction {
        val sourceId = CharacterTable.select { CharacterTable.name eq req.sourceName }
            .singleOrNull()?.get(CharacterTable.id) ?: return@transaction false

        val targetId = CharacterTable.select { CharacterTable.name eq req.targetName }
            .singleOrNull()?.get(CharacterTable.id) ?: return@transaction false

        RelationshipTable.insert {
            it[characterId] = sourceId
            it[this.targetId] = targetId
            it[relationType] = req.relation
        }
        true
    }

    // Get the connections
    fun getRelationships(name: String): List<RelationResponse> = transaction {
        // 1. Get the Source Character's ID
        val charId = CharacterTable.select { CharacterTable.name eq name }
            .singleOrNull()?.get(CharacterTable.id) ?: return@transaction emptyList()

        // 2. Single Query with a specific Join condition
        // We link RelationshipTable's targetId to CharacterTable's id
        (RelationshipTable innerJoin CharacterTable)
            .select { RelationshipTable.characterId eq charId }
            // The .on is implicit here if the foreign key is defined,
            // but it's safer to be explicit in complex joins
            .map { row ->
                val type = row[RelationshipTable.relationType] // You are using Enum column now
                val targetName = row[CharacterTable.name] // This is already the Target's name!

                RelationResponse(
                    targetName = targetName,
                    relationType = type,
                    description = "${type.name} OF $targetName"
                )
            }
    }
}