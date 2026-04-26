package service

import database.AuditLogTable
import database.CharacterTable
import database.UserTable
import model.audit.AuditEntry
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object AuditService {
    //This function's main purpose is to Log the actions of users
    fun logAction(userId: Int, action: String, charId: Int) = transaction {
        AuditLogTable.insert {
            it[AuditLogTable.userId] = userId
            it[AuditLogTable.action] = action
            it[AuditLogTable.characterId] = charId
            it[AuditLogTable.timestamp] = System.currentTimeMillis()
        }
    }

    fun getLogsForUser(userId: Int): List<AuditEntry> = transaction {
        (AuditLogTable innerJoin CharacterTable innerJoin UserTable)
            .select { AuditLogTable.userId eq userId }
            .map { it.toAudit() }
    }

    //This function is for the admin to view the logs
    //Future purpose method  in case I implement role based access.
    fun getAllLogs(): List<AuditEntry> = transaction {
        (AuditLogTable innerJoin CharacterTable innerJoin UserTable).selectAll()
            .map { it.toAudit() }
    }


    //This method is to return the audit logs in readable format
    private fun ResultRow.toAudit(): AuditEntry = AuditEntry(
        id = this[AuditLogTable.id],
        username = this[UserTable.username],
        action = this[AuditLogTable.action],
        characterName = this[CharacterTable.name], // fetched via join
        timestamp = this[AuditLogTable.timestamp]
    )
}