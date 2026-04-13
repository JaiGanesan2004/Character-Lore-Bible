package database

import model.Role
import org.jetbrains.exposed.sql.Table

object CharacterTable : Table("characters") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 250)
    val role = enumerationByName("role", 50, Role::class)
    val powerLevel = integer("powerlevel").default(0)
    val createdAt = varchar("created_at", 50)

    //This is the column that will store the String path to the image
    val imageUrl = varchar("image_url", 255).nullable()

    override val primaryKey = PrimaryKey(id)
}