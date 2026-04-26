package database

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object AbilityTable : Table("abilities"){
    val id = integer("id").autoIncrement()
    val abilityName = varchar("ability_name", 123)


    val characterId = reference("character_id", CharacterTable.id, onDelete = ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(id)
}