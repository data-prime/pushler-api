package com.pushler.dto

import com.pushler.datasource.tables.Subscriptions
import com.pushler.datasource.tables.Users
import io.ktor.auth.*
import org.jetbrains.exposed.sql.ReferenceOption
import org.joda.time.DateTime
import java.util.*

data class Channel(
    val id: UUID = UUID.randomUUID(),
    var owner: UUID,
    val name: String,
    val public: Boolean,
    val pathURL: String?,
    val imageURL: String?,
    val createAt : String = DateTime.now().toString(),
    val changeAt : String = DateTime.now().toString(),
) : Principal
