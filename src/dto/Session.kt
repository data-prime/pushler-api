package com.pushler.dto

import io.ktor.auth.*
import org.joda.time.DateTime
import java.util.*

data class Session(
    val id: UUID = UUID.randomUUID(),
    val fcm: String? = null,
    val subscriptions: List<String> = listOf(),
    val createAt : String = DateTime.now().toString(),
    val changeAt : String = DateTime.now().toString(),
) : Principal