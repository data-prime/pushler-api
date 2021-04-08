package com.pushler.datasource

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import com.pushler.datasource.interfaces.DataBaseDataSource
import com.pushler.datasource.tables.Notifications
import com.pushler.datasource.tables.Channels
import com.pushler.datasource.tables.Sessions
import com.pushler.datasource.tables.Users
import com.pushler.datasource.tables.Subscriptions
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction


class DataBaseLocalDataSource() : DataBaseDataSource {
    private val config = HikariConfig("/local.properties")

    private fun initDB() {
        config.schema = "public"
        val ds = HikariDataSource(config)
        Database.connect(ds)

        transaction {
            SchemaUtils.createMissingTablesAndColumns(Sessions, Notifications, Channels, Subscriptions, Users)
        }
    }

    override fun connect() {
        initDB()
    }


}

