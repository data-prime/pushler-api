package com.pushler.datasource.tables

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.joda.time.DateTime
import java.util.*


object Notifications : Table() {
    val id : Column<UUID> = uuid("id").primaryKey()
    val sender = reference("sender", Channels.id, onDelete = ReferenceOption.CASCADE)
    val recipient: Column<String> = text("recipient")
    val title: Column<String> = text("title")
    val body: Column<String?> = text("body").nullable()
    val imageURL: Column<String?> = text("imageURL").nullable()
    val data: Column<String?> = text("data").nullable()
    val createAt: Column<DateTime> = datetime("create_at")
}