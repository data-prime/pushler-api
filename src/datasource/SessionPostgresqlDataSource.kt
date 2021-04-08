package com.pushler.datasource

import com.pushler.datasource.interfaces.SessionDataSource
import com.pushler.datasource.tables.Channels
import com.pushler.datasource.tables.Notifications
import com.pushler.datasource.tables.Sessions
import com.pushler.datasource.tables.Subscriptions
import com.pushler.dto.Channel
import com.pushler.dto.Session
import com.pushler.dto.Subscriber
import org.jetbrains.exposed.sql.*

import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import java.sql.ResultSet
import java.util.*

class SessionPostgresqlDataSource : SessionDataSource {


    override fun create(deviceName : String?, deviceSystem : String?, appVersion : String?): Session {

        val session = Session(UUID.randomUUID(), deviceName = deviceName, deviceSystem = deviceSystem, appVersion = appVersion)

        transaction {
            Sessions.insert {
                it[id] = session.id
                it[fcm] = session.fcm
                it[Sessions.deviceName] = session.deviceName
                it[Sessions.deviceSystem] = session.deviceSystem
                it[Sessions.appVersion] = session.appVersion
                it[createAt] = DateTime.now()
                it[changeAt] = DateTime.now()
            }
        }
        return session
    }

    override fun updateMeta(
        session: Session,
        deviceName: String?,
        deviceSystem: String?,
        appVersion: String?
    ) {
        transaction {
            Sessions.update ({ Sessions.id eq session.id }) {
                it[Sessions.deviceName] = deviceName
                it[Sessions.deviceSystem] = deviceSystem
                it[Sessions.appVersion] = appVersion
            }
        }
    }

    override fun get(uuid: String) : Session? {
        var session: Session? = null

        transaction {
            Sessions.select { Sessions.id eq UUID.fromString(uuid) }.map {
                session = Session(
                    it[Sessions.id],
                    it[Sessions.fcm],
                    it[Sessions.deviceName],
                    it[Sessions.deviceSystem],
                    it[Sessions.appVersion],
                    it[Sessions.createAt].toString(),
                    it[Sessions.changeAt].toString(),
                )
            }
        }

        return session
    }

    override fun getSubscriber(session: Session) : List<Subscriber> {
        val subscriber: MutableList<Subscriber> = mutableListOf()

        transaction {
            Subscriptions.innerJoin(Channels).select { (Subscriptions.channel eq Channels.id) and (Subscriptions.session eq session.id) }.map {
                subscriber.add(Subscriber(
                    it[Subscriptions.id],
                    session,
                    Channel(
                        it[Channels.id],
                        it[Channels.owner],
                        it[Channels.name],
                        it[Channels.public],
                        it[Channels.pathURL],
                        it[Channels.imageURL],
                        it[Channels.createAt].toString(),
                        it[Channels.changeAt].toString(),
                    ),
                    it[Subscriptions.tag],
                    it[Subscriptions.createAt].toString()
                ))
            }
        }
        return subscriber.toList()
    }

    override fun getSubscriber(channel: Channel) : List<Subscriber> {
        val subscriber: MutableList<Subscriber> = mutableListOf()

        transaction {
            Subscriptions.innerJoin(Sessions).select { (Subscriptions.channel eq channel.id) and (Subscriptions.session eq Sessions.id) }.map {
                subscriber.add(Subscriber(
                    it[Subscriptions.id],
                    Session(
                        it[Sessions.id],
                        it[Sessions.fcm],
                        it[Sessions.deviceName],
                        it[Sessions.deviceSystem],
                        it[Sessions.appVersion],
                        it[Sessions.createAt].toString(),
                        it[Sessions.changeAt].toString(),
                    ),
                    channel,
                    it[Subscriptions.tag],
                    it[Subscriptions.createAt].toString()
                ))
            }
        }
        return subscriber.toList()
    }

    override fun getSubscriber(channel: Channel, tag : String) : List<Subscriber> {
        val subscriber: MutableList<Subscriber> = mutableListOf()

        transaction {
            Subscriptions.innerJoin(Sessions).select { (Subscriptions.channel eq channel.id) and (Subscriptions.session eq Sessions.id) and (Subscriptions.tag eq tag) }.map {
                subscriber.add(Subscriber(
                    it[Subscriptions.id],
                    Session(
                        it[Sessions.id],
                        it[Sessions.fcm],
                        it[Sessions.deviceName],
                        it[Sessions.deviceSystem],
                        it[Sessions.appVersion],
                        it[Sessions.createAt].toString(),
                        it[Sessions.changeAt].toString(),
                    ),
                    channel,
                    it[Subscriptions.tag],
                    it[Subscriptions.createAt].toString()
                ))
            }
        }
        return subscriber.toList()
    }

    override fun getSubscriber(channel: Channel, session: Session) : List<Subscriber> {
        val subscriber: MutableList<Subscriber> = mutableListOf()

        transaction {
            Subscriptions.select { (Subscriptions.channel eq channel.id) and (Subscriptions.session eq session.id) }.map {
                subscriber.add(Subscriber(
                    it[Subscriptions.id],
                    session,
                    channel,
                    it[Subscriptions.tag],
                    it[Subscriptions.createAt].toString()
                ))
            }
        }
        return subscriber.toList()
    }

    override fun getSubscriber(channel: Channel, session: Session, tag: String): Subscriber? {
        val subscriber: MutableList<Subscriber> = mutableListOf()

        transaction {
            Subscriptions.select { (Subscriptions.channel eq channel.id) and (Subscriptions.session eq session.id) and (Subscriptions.tag eq tag) }.map {
                subscriber.add(Subscriber(
                    it[Subscriptions.id],
                    session,
                    channel,
                    it[Subscriptions.tag],
                    it[Subscriptions.createAt].toString()
                ))
            }
        }
        return subscriber.firstOrNull()
    }

    override fun getAll(): List<Session> {
        val sessions: MutableList<Session> = mutableListOf()


        transaction {
            val conn = TransactionManager.current().connection
            val statement = conn.createStatement()
            val query = "SELECT * FROM sessions"
            val result : ResultSet = statement.executeQuery(query)
            while(result.next()) {
                sessions.add(
                    Session(
                        UUID.fromString(result.getString(Sessions.id.name)),
                        result.getString(Sessions.fcm.name),
                        result.getString(Sessions.deviceName.name),
                        result.getString(Sessions.deviceSystem.name),
                        result.getString(Sessions.appVersion.name),
                        result.getDate(Sessions.createAt.name).toString(),
                        result.getDate(Sessions.changeAt.name).toString(),
                    )
                )
            }

        }

        return sessions.toList()
    }

    override fun insertFCM(fcm: String, session : Session) {
        if (fcm != session.fcm) {

            transaction {

                if (session.fcm != null) {
                    Sessions.select { (Sessions.id neq session.id) and (Sessions.fcm eq fcm) }.forEach {
                        Notifications.deleteWhere { Notifications.recipient eq it[Sessions.id].toString() }
                    }
                    Sessions.deleteWhere { (Sessions.id neq session.id) and (Sessions.fcm eq fcm) }
                }

                Sessions.update ({ Sessions.id eq session.id }) {
                    it[Sessions.fcm] = fcm
                }
            }
        }
    }

    override fun subscribe(session : Session, channel : Channel, tag: String?) {
        transaction {
            val count = Subscriptions.select { (Subscriptions.channel eq channel.id) and (Subscriptions.session eq session.id) and (Subscriptions.tag eq tag) }.count()

            if (count < 1) {
                Subscriptions.insert {
                    it[Subscriptions.session] = session.id
                    it[Subscriptions.channel] = channel.id
                    it[Subscriptions.tag] = tag
                    it[Subscriptions.createAt] = DateTime.now()
                }
            }
        }
    }

    override fun unsubscribe(session : Session, channel : Channel, tag: String?) {
        transaction {

            if (tag != null) {
                Subscriptions.deleteWhere{ (Subscriptions.channel eq channel.id) and (Subscriptions.session eq session.id) and (Subscriptions.tag eq tag) }
            } else {
                Subscriptions.deleteWhere{ (Subscriptions.channel eq channel.id) and (Subscriptions.session eq session.id) }
            }

        }
    }
}