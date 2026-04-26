package database

import model.character.Archetype
import model.enums.Role
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object CharacterTable : Table("characters") {
    val id = integer("id").autoIncrement()
    val userId = reference("user_id", UserTable.id, onDelete = ReferenceOption.CASCADE)
    val name = varchar("name", 250)
    val role = enumerationByName("role", 50, Role::class)
    val powerLevel = integer("powerlevel").default(0)
    val createdAt = varchar("created_at", 50)

    //New things added
    val archetype = enumerationByName("archetype", 50, Archetype::class).nullable()
    val race = varchar("race", 100).nullable()
    val age = integer("age").nullable()
    val lore = text("lore").nullable()

    //This is the column that will store the String path to the image
    val imageUrl = varchar("image_url", 255).nullable()

    override val primaryKey = PrimaryKey(id)

}