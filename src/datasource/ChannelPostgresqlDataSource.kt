package com.pushler.datasource

import com.pushler.datasource.interfaces.ChannelDataSource
import com.pushler.datasource.tables.Channels
import com.pushler.dto.Channel
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import java.util.*

class ChannelPostgresqlDataSource : ChannelDataSource {


    override fun getAll(): List<Channel> {
        val partners: MutableList<Channel> = mutableListOf()

        transaction {
            Channels.selectAll().map {
                partners.add(
                    Channel(
                        it[Channels.id],
                        it[Channels.tag],
                        it[Channels.name],
                        it[Channels.public],
                        it[Channels.pathURL],
                        it[Channels.imageURL],
                        it[Channels.createAt].toString(),
                        it[Channels.changeAt].toString(),
                    )
                )
            }
        }

        return partners.toList()
    }

    override fun get(uuid: String): Channel? {

        return  transaction {
            Channels.select { Channels.id eq UUID.fromString(uuid) }.map {
                Channel(
                    it[Channels.id],
                    it[Channels.tag],
                    it[Channels.name],
                    it[Channels.public],
                    it[Channels.pathURL],
                    it[Channels.imageURL],
                    it[Channels.createAt].toString(),
                    it[Channels.changeAt].toString(),
                )
            }
        }.firstOrNull()
    }

    override fun getFromTag(tag: String): Channel? {
        return  transaction {
            Channels.select { Channels.tag eq tag }.map {
                Channel(
                    it[Channels.id],
                    it[Channels.tag],
                    it[Channels.name],
                    it[Channels.public],
                    it[Channels.pathURL],
                    it[Channels.imageURL],
                    it[Channels.createAt].toString(),
                    it[Channels.changeAt].toString(),
                )
            }
        }.firstOrNull()
    }

    override fun create(channel: Channel) {
        transaction {
            Channels.insert {
                it[id] = channel.id
                it[tag] = channel.tag
                it[name] = channel.name
                it[public] = channel.public
                it[pathURL] = channel.pathURL
                it[imageURL] = channel.imageURL
                it[createAt] = DateTime.parse(channel.createAt)
                it[changeAt] = DateTime.parse(channel.changeAt)
            }
        }
    }

    override fun delete(uuid: String) {
        transaction {
            Channels.deleteWhere { Channels.id eq UUID.fromString(uuid) }
        }
    }
}