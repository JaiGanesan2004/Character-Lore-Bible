package service

import database.AbilityTable
import database.CharacterTable
import model.ArchetypeCountDTO
import model.Character
import model.Role
import org.jetbrains.exposed.sql.Count
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime


object CharacterService {

    fun getByRole(role: Role, limit: Int, offset: Long) : List<Character> = transaction {
        CharacterTable.select{ CharacterTable.role eq role }
            .limit(limit, offset)
            .map { row ->
            val charId = row[CharacterTable.id]

            val abilitiesList = AbilityTable.select { AbilityTable.characterId eq charId }
                .map { it[AbilityTable.abilityName] }

            row.toCharacter(abilitiesList)
        }
    }

    fun getByName(name: String): Character? = transaction {
        // .singleOrNull() is the magic here. It finds 1 row or returns null.
        val row = CharacterTable.select { CharacterTable.name eq name}
            .singleOrNull()
            ?: return@transaction null

        val charId = row[CharacterTable.id]

        val abilitiesList = AbilityTable.select { AbilityTable.characterId eq charId }
            .map { it[AbilityTable.abilityName] }

        row.toCharacter(abilitiesList)
    }

    fun getByArchetypeCounts(): List<ArchetypeCountDTO> = transaction {
        val countColumn = CharacterTable.id.count()

        CharacterTable
            .slice(CharacterTable.archetype, countColumn)
            .selectAll()
            .groupBy(CharacterTable.archetype)
            .map { it.toArcheType(countColumn)
            }
    }

    //CREATE
    fun getAll(limit: Int, offset: Long, search: String?): List<Character> = transaction {
        addLogger(StdOutSqlLogger)

        val query = if(!search.isNullOrBlank()) {
            CharacterTable.select {
                CharacterTable.name.lowerCase() like "%${search.lowercase()}%"
            }
        }else {
                CharacterTable.selectAll()
            }


        query.limit(limit, offset).map { row ->
            val charId = row[CharacterTable.id]
            val abilities = AbilityTable.select { AbilityTable.characterId eq charId }
                .map{ it[AbilityTable.abilityName] }

            row.toCharacter(abilities)
        }
    }

    fun addCharacter(c: Character) = transaction {
        //1.First we insert the character and get their generated ID
        val newId = CharacterTable.insert {
            it[name] = c.name
            it[role] = c.role
            it[powerLevel] = c.powerLevel ?: 0
            it[createdAt] = LocalDateTime.now().toString()
            it[archetype] = c.archetype
            it[race] = c.race
            it[age] = c.age
            it[lore] = c.lore
        } get CharacterTable.id

        //2. Insert each ability along with the character ID
        c.abilities.forEach { abilityStr ->
            AbilityTable.insert {
                it[characterId] = newId
                it[abilityName] = abilityStr
            }
        }

    }



    fun update(name: String, character: Character): Boolean = transaction {
        //First we find the target record
        val row = CharacterTable.select { CharacterTable.name eq name }.singleOrNull() ?:  return@transaction false
        val charId = row[CharacterTable.id]

        //1. update main table
        CharacterTable.update  ({ CharacterTable.id eq charId }) {
            it[role] = character.role
            it[powerLevel] = character.powerLevel ?: 0

            //If new image Url is present then we update here.
            if(!character.imageUrl.isNullOrBlank())
                it[imageUrl] = character.imageUrl
        }

        //2. Clear old abilities
        AbilityTable.deleteWhere { AbilityTable.characterId eq charId }

        //3.Insert new abilities
        character.abilities.forEach { abilityStr ->
            AbilityTable.insert {
                it[abilityName] = abilityStr
                it[characterId] = charId
            }
        }
        true
    }

    //DELETE
    fun delete(name: String): Boolean = transaction {

        val row = CharacterTable.select { CharacterTable.name eq name }.singleOrNull() ?: return@transaction false
            val charId = row[CharacterTable.id]
            CharacterTable.deleteWhere { CharacterTable.name eq name }
            AbilityTable.deleteWhere { AbilityTable.characterId eq charId }
            true

    }


    //Extension Function to Create character object from DB query result.
    fun ResultRow.toCharacter(abilities: List<String> = emptyList()) = Character(
        id = this[CharacterTable.id],
        name = this[CharacterTable.name],
        role = this[CharacterTable.role],
        powerLevel = this[CharacterTable.powerLevel],
        abilities = abilities,
        createdAt = this[CharacterTable.createdAt],
        imageUrl = this[CharacterTable.imageUrl],
        archetype = this[CharacterTable.archetype],
        race = this[CharacterTable.race],
        age = this[CharacterTable.age],
        lore = this[CharacterTable.lore]
    )

    fun ResultRow.toArcheType(countAlias: Count) = ArchetypeCountDTO(
        archetype = this[CharacterTable.archetype],
        count = this[countAlias]
    )
}