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
    var name: String,
    var public: Boolean,
    var pathURL: String?,
    var imageURL: String?,
    val createAt : String = DateTime.now().toString(),
    var changeAt : String = DateTime.now().toString(),
) : Principal
