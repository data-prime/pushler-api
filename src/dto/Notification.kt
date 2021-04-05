package com.pushler.dto

import org.joda.time.DateTime
import java.util.*

data class Notification(
    val id: UUID = UUID.randomUUID(),
    val sender: Channel,
    val recipient: String,
    val title: String,
    val body: String? = null,
    val imageURL: String? = null,
    val data: Map<String, String> = mapOf(),
    val createAt: String = DateTime.now().toString(),
)
