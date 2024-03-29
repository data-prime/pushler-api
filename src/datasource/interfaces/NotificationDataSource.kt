package com.pushler.datasource.interfaces

import com.pushler.datasource.tables.Notifications
import com.pushler.dto.Channel
import com.pushler.dto.Notification
import com.pushler.dto.Session

interface NotificationDataSource {

    fun getNotifications(uuid: String) : Notification?

    fun getNotifications(sender: Channel, session: Session, recipient: String) : List<Notification>

    fun getPushlerNotifications(session: Session) : List<Notification>

    fun deleteNotifications(uuid: String)

    fun viewedNotifications(uuid: String, viewed: Boolean)

    fun pushNotification(notification : Notification)

    fun pushMany(notifications: List<Notification>)

}