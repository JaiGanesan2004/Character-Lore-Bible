package database

import model.enums.RelationType
import org.jetbrains.exposed.sql.Table

object RelationshipTable: Table("relationships") {
    val id = integer("id").autoIncrement()
    val characterId = integer("character_id").references(CharacterTable.id)
    val targetId = integer("target_id").references(CharacterTable.id)
    val relationType = enumerationByName("relationship", 50, RelationType::class )

    override val primaryKey = PrimaryKey(id)
}