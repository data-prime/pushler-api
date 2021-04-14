package com.pushler.datasource

import com.pushler.datasource.interfaces.UserDataSource
import com.pushler.datasource.tables.Users
import com.pushler.dto.User
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import java.util.*

class UserPostgresqlDataSource : UserDataSource {

    override fun create(user: User) {
        transaction {
            Users.insert {
                it[id] = user.id
                it[name] = user.name
                it[hash] = user.hash
                it[createdAt] = DateTime.parse(user.createdAt)
                it[updatedAt] = DateTime.parse(user.updatedAt)
            }
        }
    }

    override fun getAll(): List<User> {
        val users: MutableList<User> = mutableListOf()

        transaction {
            Users.selectAll().map {
                users.add(
                    User(
                        it[Users.id],
                        it[Users.name],
                        it[Users.hash],
                        it[Users.createdAt].toString(),
                        it[Users.updatedAt].toString(),
                    )
                )
            }
        }

        return users.toList()
    }

    override fun getByName(username: String): User? {
        return  transaction {
            Users.select { Users.name eq username }.map {
                User(
                    it[Users.id],
                    it[Users.name],
                    it[Users.hash],
                    it[Users.createdAt].toString(),
                    it[Users.updatedAt].toString(),
                )
            }
        }.firstOrNull()
    }

    override fun get(id: String): User? {
        return  transaction {
            Users.select { Users.id eq UUID.fromString(id) }.map {
                User(
                    it[Users.id],
                    it[Users.name],
                    it[Users.hash],
                    it[Users.createdAt].toString(),
                    it[Users.updatedAt].toString(),
                )
            }
        }.firstOrNull()
    }

    override fun delete(uuid: String): Int {
        return transaction {
            return@transaction Users.deleteWhere { Users.id eq UUID.fromString(uuid) }
        }
    }

    override fun deleteByName(name: String): Int {
        return transaction {
            return@transaction Users.deleteWhere { Users.name eq name }
        }
    }

    override fun update(user : User) : Int {
        return transaction {
            return@transaction Users.update({ Users.id eq user.id }) {
                it[name] = user.name
                it[hash] = user.hash
                it[updatedAt] = DateTime.now()
            }
        }
    }
}