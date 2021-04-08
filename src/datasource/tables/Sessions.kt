package com.pushler.datasource.tables

import com.pushler.datasource.extensions.textArray
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.joda.time.DateTime
import java.util.*


object Sessions : Table() {
    val id : Column<UUID> = uuid("id").primaryKey()
    val fcm: Column<String?> = text("fcm_token").nullable()
    val deviceName: Column<String?> = text("device_name").nullable()
    val deviceSystem: Column<String?> = text("device_system").nullable()
    val appVersion: Column<String?> = text("app_version").nullable()
    val createAt: Column<DateTime> = datetime("create_at")
    val changeAt: Column<DateTime> = datetime("change_at")
}



