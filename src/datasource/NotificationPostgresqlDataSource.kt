package com.pushler.datasource

import com.google.gson.Gson
import com.pushler.datasource.interfaces.NotificationDataSource
import com.pushler.datasource.tables.Channels
import com.pushler.datasource.tables.Notifications
import com.pushler.datasource.tables.Sessions
import com.pushler.dto.Channel
import com.pushler.dto.Notification
import com.pushler.dto.Session
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import java.util.*

class NotificationPostgresqlDataSource : NotificationDataSource {


    override fun getNotifications(sender: Channel, session: Session, recipient: String) : List<Notification> {
        val notifications: MutableList<Notification> = mutableListOf()

        transaction {
            Notifications.innerJoin(Channels).select(((Notifications.recipient eq recipient) or (Notifications.recipient eq session.id.toString())) and (Notifications.sender eq sender.id) and (Notifications.archive eq false) ).map { row ->
                notifications.add(
                    Notification(
                        row[Notifications.id],
                        Channel(
                            row[Channels.id],
                            row[Channels.owner],
                            row[Channels.name],
                            row[Channels.public],
                            row[Channels.pathURL],
                            row[Channels.imageURL],
                            row[Channels.createAt].toString(),
                            row[Channels.changeAt].toString(),
                        ),
                        row[Notifications.recipient],
                        row[Notifications.title],
                        row[Notifications.viewed],
                        row[Notifications.body],
                        row[Notifications.imageURL],
                        row[Notifications.data].let { Gson().fromJson<Map<String, String>>(it, Map::class.java) },
                        row[Notifications.createAt].toString(),
                    )
                )
            }
        }

        return notifications.toList()
    }

    override fun getNotifications(uuid: String): Notification? {
        var notification: Notification? = null

        transaction {
            Notifications.innerJoin(Channels).select(Notifications.id eq UUID.fromString(uuid) and (Notifications.archive eq false) ).map { row ->
                notification = Notification(
                    row[Notifications.id],
                    Channel(
                        row[Channels.id],
                        row[Channels.owner],
                        row[Channels.name],
                        row[Channels.public],
                        row[Channels.pathURL],
                        row[Channels.imageURL],
                        row[Channels.createAt].toString(),
                        row[Channels.changeAt].toString(),
                    ),
                    row[Notifications.recipient],
                    row[Notifications.title],
                    row[Notifications.viewed],
                    row[Notifications.body],
                    row[Notifications.imageURL],
                    row[Notifications.data].let { Gson().fromJson<Map<String, String>>(it, Map::class.java) },
                    row[Notifications.createAt].toString(),
                )
            }
        }
        return notification
    }

    override fun getPushlerNotifications(session: Session): List<Notification> {
        val notifications: MutableList<Notification> = mutableListOf()

        transaction {
            Notifications.select((Notifications.sender eq null) and (Notifications.recipient eq session.id.toString()) and (Notifications.archive eq false) ).map { row ->
                notifications.add(
                    Notification(
                        row[Notifications.id],
                        null,
                        row[Notifications.recipient],
                        row[Notifications.title],
                        row[Notifications.viewed],
                        row[Notifications.body],
                        row[Notifications.imageURL],
                        row[Notifications.data].let { Gson().fromJson<Map<String, String>>(it, Map::class.java) },
                        row[Notifications.createAt].toString(),
                    )
                )
            }
        }
        return notifications.toList()
    }

    override fun deleteNotifications(uuid: String) {
        transaction {
            Notifications.update ({ Notifications.id eq UUID.fromString(uuid) }) { row ->
                row[Notifications.archive] = true
            }
        }

    }

    override fun viewedNotifications(uuid: String, viewed: Boolean) {
        transaction {
            Notifications.update ({ Notifications.id eq UUID.fromString(uuid) }) { row ->
                row[Notifications.viewed] = viewed
            }
        }
    }

    override fun pushNotification(notification: Notification) {
        transaction {
            Notifications.insert { row ->
                row[id] = notification.id
                row[sender] = notification.sender?.id
                row[recipient] = notification.recipient
                row[title] = notification.title
                row[viewed] = notification.viewed
                row[body] = notification.body
                row[data] = notification.data.let { Gson().toJson(it) }
                row[imageURL] = notification.imageURL
                row[createAt] = DateTime.parse(notification.createAt)
            }
        }
    }

    override fun pushMany(notifications: List<Notification>) {
        transaction {
            Notifications.batchInsert(notifications) { notification ->
                this[Notifications.id] = notification.id
                this[Notifications.sender] = notification.sender?.id
                this[Notifications.recipient] = notification.recipient
                this[Notifications.title] = notification.title
                this[Notifications.viewed] = notification.viewed
                this[Notifications.body] = notification.body
                this[Notifications.data] = notification.data.let { Gson().toJson(it) }
                this[Notifications.imageURL] = notification.imageURL
                this[Notifications.createAt] = DateTime.parse(notification.createAt)
            }
        }
    }
}