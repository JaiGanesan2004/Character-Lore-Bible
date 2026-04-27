package service

import database.CharacterTable
import database.RelationshipTable
import model.dtos.RelationAdd
import model.dtos.RelationResponse
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object RelationshipService {
    // Forge the bond
    fun addRelationship(req: RelationAdd, userId: Int): Boolean = transaction {
        val sourceExists = CharacterTable.select {
            (CharacterTable.id eq req.sourceId) and
                    (CharacterTable.userId eq userId)
        }.empty().not()

        val targetExists = CharacterTable.select {
            (CharacterTable.id eq req.targetId) and
                    (CharacterTable.userId eq userId)
        }.empty().not()

        if(!sourceExists || !targetExists) return@transaction false

        RelationshipTable.insert {
            it[characterId] = req.sourceId
            it[targetId] = req.targetId
            it[relationType] = req.relation
        }

        true
    }

    // Get the connections
    fun getRelationships(charId: Int, userId: Int): List<RelationResponse> = transaction {
       val exists = CharacterTable.select {
           (CharacterTable.id eq charId) and (CharacterTable.userId eq userId)
       }.empty().not()

        if(!exists) return@transaction emptyList()

        val join = RelationshipTable.innerJoin(
            CharacterTable,
            onColumn = { RelationshipTable.targetId},
            otherColumn = { CharacterTable.id}
        )

        join.slice(RelationshipTable.relationType, CharacterTable.name)
            .select { RelationshipTable.characterId eq charId }
            .map {
                row ->
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