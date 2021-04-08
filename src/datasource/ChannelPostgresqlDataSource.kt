package com.pushler.datasource

import com.pushler.datasource.interfaces.ChannelDataSource
import com.pushler.datasource.tables.Channels
import com.pushler.datasource.tables.Sessions
import com.pushler.dto.Channel
import io.ktor.http.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
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
                        it[Channels.owner],
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

    override fun getFromUser(user: UUID): List<Channel> {
        val partners: MutableList<Channel> = mutableListOf()

        transaction {
            Channels.select { Channels.owner eq user }.map {
                partners.add(
                    Channel(
                        it[Channels.id],
                        it[Channels.owner],
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
                    it[Channels.owner],
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
                it[owner] = channel.owner
                it[name] = channel.name
                it[public] = channel.public
                it[pathURL] = channel.pathURL
                it[imageURL] = channel.imageURL
                it[createAt] = DateTime.parse(channel.createAt)
                it[changeAt] = DateTime.parse(channel.changeAt)
            }
        }
    }

    override fun update(channel: Channel) {
        return transaction {
            Channels.update ({ Channels.id eq channel.id }) {
                it[name] = channel.name
                it[public] = channel.public
                it[pathURL] = channel.pathURL
                it[imageURL] = channel.imageURL
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