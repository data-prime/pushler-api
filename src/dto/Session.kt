package me.simplepush.dto

import io.ktor.auth.*
import java.util.*

data class Session(
    val id: UUID = UUID.randomUUID(),
    val firebase: String,
    val tags: List<String>?,
) : Principal