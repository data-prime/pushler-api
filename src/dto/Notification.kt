package me.simplepush.dto

import java.util.*

data class Notification(
    val id: UUID = UUID.randomUUID(),
    val recipient: String,
    val title: String,
    val body: String? = null,
    val imageURL: String? = null,
    val data: Map<String, String> = mapOf(),
)
