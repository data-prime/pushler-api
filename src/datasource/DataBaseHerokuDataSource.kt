package me.simplepush.datasource

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import me.simplepush.datasource.interfaces.DataBaseDataSource
import me.simplepush.datasource.tables.Notifications
import me.simplepush.datasource.tables.Sessions
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
        }
    }


    override fun connect() {
        initDB()
    }


}

