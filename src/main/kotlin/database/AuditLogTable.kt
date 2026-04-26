package database

import org.jetbrains.exposed.sql.Table

object AuditLogTable: Table("audit_logs") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(UserTable.id)
    val action = varchar("event", 50)
    val characterId = integer("character_id") references(CharacterTable.id)
    val timestamp = long("timestamp")

    override val primaryKey = PrimaryKey(id)

}