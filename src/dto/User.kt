package com.pushler.dto

import java.util.*

data class User (
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val hash: String,
)