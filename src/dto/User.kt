package com.pushler.dto

import com.pushler.datasource.tables.Channels
import com.pushler.datasource.tables.Channels.uniqueIndex
import io.ktor.auth.*
import org.jetbrains.exposed.sql.Column
import org.joda.time.DateTime
import java.util.*

data class User (
    val id: UUID = UUID.randomUUID(),
    var name: String,
    @Transient var hash: String,
    val createdAt : String = DateTime.now().toString(),
    var updatedAt : String = DateTime.now().toString(),
) : Principal

//data class UserHashless(val user: User) {
//    val id = user.id
//    val name = user.name
//    val createdAt = user.createdAt
//    val updatedAt = user.updatedAt
//}