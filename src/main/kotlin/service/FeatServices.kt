package service

import database.FeatsTable
import model.dtos.FeatRequest
import model.dtos.FeatResponse
import model.enums.FeatLevel
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

object FeatServices {

    fun setFeat(charId: Int, request: FeatRequest, userId: Int): Boolean = transaction {

        if (!validateOwnership(charId, userId)) return@transaction false

        val existing = FeatsTable.select {
            (FeatsTable.characterId eq charId) and
                    (FeatsTable.category eq request.category)
        }.singleOrNull()

        if (existing != null) {
            FeatsTable.update({
                (FeatsTable.characterId eq charId) and
                        (FeatsTable.category eq request.category)
            }) {
                it[description] = request.description
            }
        } else {
            FeatsTable.insert {
                it[FeatsTable.characterId] = charId
                it[category] = request.category
                it[description] = request.description
            }
        }

        true
    }

    fun getFeatsByCharacterId(charId: Int, userId: Int): List<FeatResponse> = transaction {

        if (!validateOwnership(charId, userId)) return@transaction emptyList()

        FeatsTable.select {
            FeatsTable.characterId eq charId
        }.map {
            FeatResponse(
                category = it[FeatsTable.category],
                description = it[FeatsTable.description]
            )
        }
    }

    fun deleteFeat(charId: Int, category: FeatLevel, userId: Int): Boolean = transaction {

        if (!validateOwnership(charId, userId)) return@transaction false

        val deleted = FeatsTable.deleteWhere {
            (FeatsTable.characterId eq charId) and
                    (FeatsTable.category eq category)
        }

        deleted > 0
    }

    private fun validateOwnership(char: Int, userId: Int): Boolean = CharacterService.getCharacterById(char, userId) != null

}
