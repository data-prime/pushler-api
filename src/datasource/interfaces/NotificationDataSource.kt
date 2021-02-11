package me.simplepush.datasource.interfaces

import me.simplepush.datasource.tables.Notifications
import me.simplepush.dto.Notification

interface NotificationDataSource {

    fun getNotifications(tag: String) : List<Notification>

    fun pushNotification(recipientTag: String, notification : Notification)

}