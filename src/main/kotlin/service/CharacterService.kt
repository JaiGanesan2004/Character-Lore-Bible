package service

import database.AbilityTable
import database.CharacterTable
import model.dtos.ArchetypeCountDTO
import model.character.Character
import model.dtos.CharacterUpdateRequest
import model.enums.Role
import org.jetbrains.exposed.sql.Count
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.io.File
import java.time.LocalDateTime


object CharacterService {

    fun getByRole(role: Role, limit: Int, offset: Long, userId: Int) : List<Character> = transaction {
        CharacterTable.select{ (CharacterTable.role eq role) and (CharacterTable.userId eq userId) }
            .limit(limit, offset)
            .map { row ->
            val charId = row[CharacterTable.id]

            val abilitiesList = AbilityTable.select { AbilityTable.characterId eq charId }
                .map { it[AbilityTable.abilityName] }

            row.toCharacter(abilitiesList)
        }
    }

    fun getByName(name: String, userId: Int): List<Character> = transaction {

        CharacterTable.select {
            (CharacterTable.name eq name) and (CharacterTable.userId eq userId)
        } .map { row ->
            val charId = row[CharacterTable.id]
            val abilitiesList = AbilityTable.select { AbilityTable.characterId eq charId }
                .map { it[AbilityTable.abilityName] }

            row.toCharacter(abilitiesList)
        }


    }

    fun getByArchetypeCounts(userId: Int): List<ArchetypeCountDTO> = transaction {
        val countColumn = CharacterTable.id.count()

        CharacterTable
            .slice(CharacterTable.archetype, countColumn)
            .select(CharacterTable.userId eq userId )
            .groupBy(CharacterTable.archetype)
            .map {
                it.toArcheType(countColumn)
            }
    }

    fun getAbilitiesByCharacterId(charId: Int?): List<String> = transaction {
        charId?.let {
            AbilityTable.select { AbilityTable.characterId eq charId }
                .map { it[AbilityTable.abilityName] }
        }
            ?: listOf()
    }

    fun getCharacterById(charId: Int, userId: Int): Character? = transaction {
        val character = CharacterTable.select {
            (CharacterTable.id eq charId) and (CharacterTable.userId eq userId)
        }.singleOrNull() ?: return@transaction null

        character.toCharacter()
    }

    fun getAll(limit: Int, offset: Long, search: String?, userId: Int): List<Character> = transaction {
        addLogger(StdOutSqlLogger)

        val query = if(!search.isNullOrBlank()) {
            CharacterTable.select { (CharacterTable.userId eq userId) and
                (CharacterTable.name.lowerCase() like "%${search.lowercase()}%")
            }
        }else {
                CharacterTable.select {
                    CharacterTable.userId eq userId
                }
            }


        query.limit(limit, offset).map { row ->
            val charId = row[CharacterTable.id]
            val abilities = AbilityTable.select { AbilityTable.characterId eq charId }
                .map{ it[AbilityTable.abilityName] }

            row.toCharacter(abilities)
        }
    }

    fun addCharacter(c: Character, userId: Int): Int = transaction {
        //1.First we insert the character and get their generated ID
        val newId = CharacterTable.insert {
            it[name] = c.name
            it[role] = c.role
            it[powerLevel] = c.powerLevel ?: 0
            it[CharacterTable.userId] = userId
            it[createdAt] = LocalDateTime.now().toString()
            it[archetype] = c.archetype
            it[race] = c.race
            it[age] = c.age
            it[lore] = c.lore
            it[imageUrl] = c.imageUrl
        } get CharacterTable.id

        //2. Insert each ability along with the character ID
        c.abilities.forEach { abilityStr ->
            AbilityTable.insert {
                it[characterId] = newId
                it[abilityName] = abilityStr
            }
        }

        RedisCacheManager.evict(charId = newId, userId = userId)

        newId
    }

    fun updateById(charId: Int, request: CharacterUpdateRequest, userId: Int): Boolean = transaction {
        val row = CharacterTable.select { (CharacterTable.id eq charId) and (CharacterTable.userId eq userId) }
            .singleOrNull() ?: return@transaction false

        CharacterTable.update ({ CharacterTable.id eq charId }){
            request.role?.let { it1 -> it[role] = it1 }
            request.powerLevel?.let { it1 -> it[powerLevel] = it1 }
            request.imageUrl?.let { it1 -> it[imageUrl] = it1 }
            request.archetype?.let { it1 -> it[archetype] = it1 }
            request.race?.let { it1 -> it[race] = it1 }
            request.age?.let { it1 -> it[age] = it1 }
            request.lore?.let { it1 -> it[lore] = it1 }
        }

        request.abilities?.let { newAbilities ->
            AbilityTable.deleteWhere { AbilityTable.characterId eq charId }

            newAbilities.forEach {
                ability ->
                AbilityTable.insert {
                    it[characterId] = charId
                    it[abilityName] = ability
                }
            }
        }

        RedisCacheManager.evict(charId, userId)

        true
    }

    fun deleteById(charId: Int, userId: Int): Pair<Boolean, String?> = transaction {
        val row = CharacterTable.select { (CharacterTable.id eq charId) and (CharacterTable.userId eq userId) }.singleOrNull() ?: return@transaction Pair(false, null)

        val imageUrl = row[CharacterTable.imageUrl]
        val charName = row[CharacterTable.name]

        CharacterTable.deleteWhere { CharacterTable.id eq row[CharacterTable.id]}

        imageUrl?.let{
            val file = File("uploads/${it.substringAfterLast("/")}")
            if(file.exists()) file.delete()
        }

        RedisCacheManager.evict(charId, userId)

        Pair(true, charName)
    }

    fun getCharacterNameById(userId: Int, charId: Int): String = transaction {
        CharacterTable.select { (CharacterTable.id eq charId) and (CharacterTable.userId eq userId) }
            .singleOrNull()?.get(CharacterTable.name) ?: return@transaction "Character with given ID does not exist"
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
        lore = this[CharacterTable.lore],
        userId = this[CharacterTable.userId]
    )

    fun ResultRow.toArcheType(countAlias: Count) = ArchetypeCountDTO(
        archetype = this[CharacterTable.archetype],
        count = this[countAlias]
    )
}