package database

import org.jetbrains.exposed.sql.Table

object UserTable : Table("users") {
    val id = integer("id").autoIncrement()
    val username = varchar("username", 50).uniqueIndex() //Unique column which means it doesn't allow any duplicate usernames
    val hashword = varchar("password_hash", 255)

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(username)
    }
}