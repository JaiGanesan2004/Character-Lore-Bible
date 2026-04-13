package plugins

import database.AbilityTable
import database.AuditLogTable
import database.CharacterTable
import database.RelationshipTable
import database.UserTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init(){
        Database.connect("jdbc:sqlite:./lore.db", "org.sqlite.JDBC")

        transaction {
            SchemaUtils.create(CharacterTable, AbilityTable, UserTable, AuditLogTable, RelationshipTable)
        }
    }
}

