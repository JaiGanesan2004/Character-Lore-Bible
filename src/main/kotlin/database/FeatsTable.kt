package database

import model.enums.FeatLevel
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object FeatsTable: Table()  {
    val id = integer("id").autoIncrement()
    val characterId = reference("character_id", CharacterTable.id, onDelete = ReferenceOption.CASCADE)
    val category = enumerationByName("category", 50, FeatLevel::class)
    val description = text("description")

    override val primaryKey = PrimaryKey(id)

    init {
        //So character id doesn't repeat and all the feats of the same level are written in a single feat description
        uniqueIndex(characterId, category)
    }
}