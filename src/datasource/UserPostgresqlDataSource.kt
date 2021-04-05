package com.pushler.datasource

import com.pushler.datasource.interfaces.UserDataSource
import com.pushler.datasource.tables.Users
import com.pushler.dto.User
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
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

//    override fun getAll(): List<User> {
//        val partners: MutableList<User> = mutableListOf()
//
//        transaction {
//            Users.selectAll().map {
//                partners.add(
//                    User(
//                        it[Users.id],
//                        it[Users.name],
//                        it[Users.public],
//                        it[Users.pathURL],
//                        it[Users.imageURL],
//                        it[Users.createAt].toString(),
//                        it[Users.changeAt].toString(),
//                    )
//                )
//            }
//        }
//
//        return partners.toList()
//    }
//
//    override fun get(uuid: String): User? {
//
//        return  transaction {
//            Users.select { Users.id eq UUID.fromString(uuid) }.map {
//                User(
//                    it[Users.id],
//                    it[Users.tag],
//                    it[Users.name],
//                    it[Users.public],
//                    it[Users.pathURL],
//                    it[Users.imageURL],
//                    it[Users.createAt].toString(),
//                    it[Users.changeAt].toString(),
//                )
//            }
//        }.firstOrNull()
//    }
//
//    override fun delete(uuid: String) {
//        transaction {
//            Users.deleteWhere { Users.id eq UUID.fromString(uuid) }
//        }
//    }
}