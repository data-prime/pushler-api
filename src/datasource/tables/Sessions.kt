package me.simplepush.datasource.tables

import me.simplepush.datasource.extensions.textArray
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import java.util.*


object Sessions : Table() {
    val id : Column<UUID> = uuid("id").primaryKey()
    val firebase: Column<String> = text("firebase")
    val tags : Column<List<String>?> = textArray("tags").nullable()
}



