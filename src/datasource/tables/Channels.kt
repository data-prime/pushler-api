package com.pushler.datasource.tables

import com.pushler.datasource.extensions.textArray
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.joda.time.DateTime
import java.util.*


object Channels : Table() {
    val id : Column<UUID> = uuid("id").primaryKey()
    val tag : Column<String> = text("tag").uniqueIndex()
    val name: Column<String> = text("name")
    val public: Column<Boolean> = bool("public")
    val pathURL: Column<String?> = text("path_url").nullable()
    val imageURL : Column<String?> = text("image_url").nullable()
    val workspace : Column<String?> = text("workspace").nullable()
    val createAt: Column<DateTime> = datetime("create_at")
    val changeAt: Column<DateTime> = datetime("change_at")
}



