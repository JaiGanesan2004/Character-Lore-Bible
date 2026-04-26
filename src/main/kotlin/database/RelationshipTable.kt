package database

import model.enums.RelationType
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object RelationshipTable: Table("relationships") {
    val id = integer("id").autoIncrement()
    val characterId = reference("character_id", CharacterTable.id, onDelete = ReferenceOption.CASCADE)
    val targetId = reference("target_id", CharacterTable.id, onDelete = ReferenceOption.CASCADE)
    val relationType = enumerationByName("relationship", 50, RelationType::class )

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(characterId, targetId)
    }
}