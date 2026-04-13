import database.CharacterTable
import database.RelationshipTable
import model.dtos.RelationAdd
import model.dtos.RelationResponse
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
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
        // 1. Get the Source ID (Zero)
        val sourceId = CharacterTable.select { CharacterTable.name eq name }
            .singleOrNull()?.get(CharacterTable.id) ?: return@transaction emptyList()


        val join = RelationshipTable.innerJoin(
            otherTable = CharacterTable,
            onColumn = {targetId},
            otherColumn = { CharacterTable.id}
        )

        join.slice(RelationshipTable.relationType, CharacterTable.name)
            .select { RelationshipTable.characterId eq sourceId }
            .map{row ->
                val type = row[RelationshipTable.relationType]
                val targetName = row[CharacterTable.name]

                RelationResponse(
                    targetName = targetName,
                    relationType = type,
                    description = "${type.name} OF $targetName"
                )
            }
    }
}