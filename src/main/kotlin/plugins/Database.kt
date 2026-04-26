package plugins

import database.AbilityTable
import database.AuditLogTable
import database.CharacterTable
import database.FeatsTable
import database.RelationshipTable
import database.UserTable
import io.github.cdimascio.dotenv.dotenv
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {

        // Original — used by the real app
        fun init() {
            val dotenv = dotenv()
            Database.connect(
                url = dotenv["DB_URL"],
                user = dotenv["DB_USER"],
                password = dotenv["DB_PASSWORD"],
                driver = "org.postgresql.Driver"
            )
            createTables()
        }

        // New — used by tests (or anywhere that wants to pass its own DB)
        fun init(url: String, user: String, password: String, driver: String = "org.postgresql.Driver") {
            Database.connect(url = url, driver = driver, user = user, password = password)
            createTables()
        }

    private fun createTables() = transaction {
        SchemaUtils.create(CharacterTable, AbilityTable, UserTable, AuditLogTable, RelationshipTable, FeatsTable)
    }
}

