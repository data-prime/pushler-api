package me.simplepush.datasource

import me.simplepush.datasource.interfaces.NotificationDataSource
import me.simplepush.datasource.tables.Notifications
import me.simplepush.datasource.tables.Sessions
import me.simplepush.dto.Notification
import me.simplepush.dto.Session
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class NotificationFirebaseDataSource : NotificationDataSource {

    override fun getNotifications(tag: String) : List<Notification> {
        val notifications: MutableList<Notification> = mutableListOf()

        transaction {
            Notifications.select(Notifications.recipient eq tag).map {
                notifications.add(
                    Notification(
                        it[Notifications.id],
                        it[Notifications.recipient],
                        it[Notifications.title],
                        it[Notifications.body],
                        it[Notifications.imageURL],
                    )
                )
            }
        }

        return notifications.toList()
    }

    override fun pushNotification(recipientTag: String, notification: Notification) {
        transaction {
            Notifications.insert {
                it[id] = notification.id
                it[recipient] = notification.recipient
                it[title] = notification.title
                it[body] = notification.body
                it[imageURL] = notification.imageURL
            }

        }
    }

}