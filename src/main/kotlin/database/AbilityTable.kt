package database

import org.jetbrains.exposed.sql.Table

object AbilityTable : Table("abilities"){
    val id = integer("id").autoIncrement()
    val abilityName = varchar("ability_name", 123)


    val characterId = integer("character_id") references CharacterTable.id

    override val primaryKey = PrimaryKey(id)
}