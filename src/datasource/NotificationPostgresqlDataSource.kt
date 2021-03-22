package com.pushler.datasource

import com.google.gson.Gson
import com.pushler.datasource.interfaces.NotificationDataSource
import com.pushler.datasource.tables.Notifications
import com.pushler.dto.Notification
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

class NotificationPostgresqlDataSource : NotificationDataSource {

    override fun getNotifications(recipient: String) : List<Notification> {
        val notifications: MutableList<Notification> = mutableListOf()


        transaction {
            Notifications.select(Notifications.recipient eq recipient).map { row ->
                notifications.add(
                    Notification(
                        row[Notifications.id],
                        row[Notifications.recipient],
                        row[Notifications.title],
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

    override fun pushNotification(notification: Notification) {
        transaction {
            Notifications.insert { row ->
                row[id] = notification.id
                row[recipient] = notification.recipient
                row[title] = notification.title
                row[body] = notification.body
                row[data] = notification.data.let { Gson().toJson(it) }
                row[imageURL] = notification.imageURL
                row[createAt] = DateTime.parse(notification.createAt)
            }
        }


    }

}