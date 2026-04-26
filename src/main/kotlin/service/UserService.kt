package service

import database.UserTable
import model.user.User
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt

object UserService {
    fun register(user: User): Boolean = transaction {
        //First we check if username already exists
        val exists = UserTable.select { UserTable.username eq user.username }.any()
        if(exists) return@transaction false

        //Second we now hash the password. gensalt() ==== generateSalt()
        val hashedpw = BCrypt.hashpw(user.password, BCrypt.gensalt())

        //Third We insert it into the DB
        UserTable.insert {
            it[username] = user.username
            it[hashword] = hashedpw
        }
        true
    }

    fun login(user: User) : Boolean = transaction {
        val row = UserTable.select { UserTable.username eq user.username }.singleOrNull() ?: return@transaction false

        val hashedPasswd = row[UserTable.hashword]

        BCrypt.checkpw(user.password, hashedPasswd)
    }

    fun getUserId(username: String): Int? = transaction {
        UserTable.select { UserTable.username eq username }
            .map { it[UserTable.id] }
            .singleOrNull()
    }
}