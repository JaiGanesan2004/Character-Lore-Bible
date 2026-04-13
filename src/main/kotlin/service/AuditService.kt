package service

import database.AuditLogTable
import database.UserTable
import model.AuditEntry
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object AuditService {
    //This function's main purpose is to Log the actions of users
    fun logAction(username: String, action: String, charName: String) = transaction {
        val userId = UserTable.select { UserTable.username eq username }
            .singleOrNull()?.get(UserTable.id) ?: return@transaction

        AuditLogTable.insert {
            it[AuditLogTable.userId] = userId
            it[AuditLogTable.action] = action
            it[AuditLogTable.characterName] = charName
            it[AuditLogTable.timestamp] = System.currentTimeMillis()
        }

    }

    fun getLogsForUser(targetUsername: String): List<AuditEntry> = transaction {
        (AuditLogTable innerJoin UserTable)
            .select { UserTable.username eq  targetUsername}
            .map { it.toAudit() }
    }

    //This function is for the admin to view the logs
    fun getAllLogs(): List<AuditEntry> = transaction {
        (AuditLogTable innerJoin UserTable).selectAll()
            .map { it.toAudit() }
    }


    private fun ResultRow.toAudit(): AuditEntry = AuditEntry(
        id = this[AuditLogTable.id],
        username = this[UserTable.username],
        action = this[AuditLogTable.action],
        characterName = this[AuditLogTable.characterName],
        timestamp = this[AuditLogTable.timestamp]
    )
}