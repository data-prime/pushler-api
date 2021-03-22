package com.pushler.datasource.interfaces

import com.pushler.datasource.tables.Notifications
import com.pushler.dto.Notification

interface NotificationDataSource {

    fun getNotifications(recipient: String) : List<Notification>

    fun pushNotification(notification : Notification)

}