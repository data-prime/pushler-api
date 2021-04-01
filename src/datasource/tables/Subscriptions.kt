package com.pushler.datasource.tables

import com.pushler.datasource.extensions.textArray
import com.pushler.datasource.tables.Channels.nullable
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.joda.time.DateTime
import java.util.*


object Subscriptions : Table() {
    val id = integer("id").autoIncrement().uniqueIndex().primaryKey()
    val session = reference("session", Sessions.id, onDelete = ReferenceOption.CASCADE)
    val channel = reference("channel", Channels.id, onDelete = ReferenceOption.CASCADE)
    val tag: Column<String?> = text("tag").nullable()
    val createAt: Column<DateTime> = datetime("create_at")
}

