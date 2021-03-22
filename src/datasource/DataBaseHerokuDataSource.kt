package com.pushler.datasource

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import com.pushler.datasource.interfaces.DataBaseDataSource
import com.pushler.datasource.tables.Notifications
import com.pushler.datasource.tables.Channels
import com.pushler.datasource.tables.Sessions
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction


class DataBaseHerokuDataSource() : DataBaseDataSource {
    private val config = HikariConfig("/heroku.properties")

    private fun initDB() {
        config.schema = "public"
        val ds = HikariDataSource(config)
        Database.connect(ds)

        transaction {
            SchemaUtils.create(Sessions)
            SchemaUtils.create(Notifications)
            SchemaUtils.create(Channels)
        }
    }


    override fun connect() {
        initDB()
    }


}

