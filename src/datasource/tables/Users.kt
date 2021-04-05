package com.pushler.datasource.tables

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.joda.time.DateTime
import java.util.*

object Users: Table() {
    val id : Column<UUID> = uuid("id").primaryKey()
    val name : Column<String> = text("name").uniqueIndex()
    val hash : Column<String> = text("hash")
    val createdAt: Column<DateTime> = datetime("created_at")
    val updatedAt: Column<DateTime> = datetime("updated_at")
}