package com.pushler.dto

import io.ktor.auth.*
import org.joda.time.DateTime
import java.util.*

data class Channel(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val public: Boolean,
    val pathURL: String?,
    val imageURL: String?,
    val createAt : String = DateTime.now().toString(),
    val changeAt : String = DateTime.now().toString(),
) : Principal
