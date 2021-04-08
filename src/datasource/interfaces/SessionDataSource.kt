package com.pushler.datasource.interfaces

import com.pushler.dto.Channel
import com.pushler.dto.Session
import com.pushler.dto.Subscriber

interface SessionDataSource {

    fun create(deviceName : String?, deviceSystem : String?, appVersion : String?) : Session

    fun updateMeta(session: Session, deviceName : String?, deviceSystem : String?, appVersion : String?)

    fun get(uuid: String) : Session?

    fun getSubscriber(channel: Channel) : List<Subscriber>

    fun getSubscriber(channel: Channel, tag : String) : List<Subscriber>

    fun getSubscriber(channel: Channel, session: Session) : List<Subscriber>

    fun getSubscriber(channel: Channel, session: Session, tag : String) : Subscriber?

    fun getSubscriber(session: Session) : List<Subscriber>

    fun getAll() : List<Session>

    fun insertFCM(fcm: String, session : Session)

    fun subscribe(session : Session, channel : Channel, tag: String?)

    fun unsubscribe(session : Session, channel : Channel, tag: String?)


}