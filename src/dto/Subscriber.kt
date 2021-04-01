package com.pushler.dto

import org.jetbrains.exposed.dao.EntityID
import org.joda.time.DateTime

data class Subscriber(
    val id: Int,
    val session: Session,
    val channel: Channel,
    val tag: String?,
    val createAt: String = DateTime.now().toString(),
)
