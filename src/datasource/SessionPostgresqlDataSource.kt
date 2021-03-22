package com.pushler.datasource

import com.pushler.datasource.interfaces.SessionDataSource
import com.pushler.datasource.tables.Sessions
import com.pushler.dto.Session

import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.postgresql.jdbc.PgArray
import java.sql.ResultSet
import java.util.*

class SessionPostgresqlDataSource : SessionDataSource {


    override fun create(): Session {

        val session = Session(UUID.randomUUID())

        transaction {
            val conn = TransactionManager.current().connection
            val statement = conn.createStatement()
            val query = "INSERT INTO sessions (id, subscriptions, create_at, change_at) VALUES ('${session.id}', ARRAY${session.subscriptions.map { "'${it}'" }}::text[], NOW(), NOW())"
            statement.execute(query)
        }

        return session
    }

    override fun get(uuid: String) : Session? {
        var session: Session? = null

        transaction {
            Sessions.select { Sessions.id eq UUID.fromString(uuid) }.map {
                session = Session(
                    it[Sessions.id],
                    it[Sessions.fcm],
                    it[Sessions.subscriptions],
                    it[Sessions.createAt].toString(),
                    it[Sessions.changeAt].toString(),
                )
            }
        }

        return session
    }

    override fun getFromTag(tag: String) : List<Session> {
        val sessions: MutableList<Session> = mutableListOf()



        transaction {
            val conn = TransactionManager.current().connection
            val statement = conn.createStatement()
            val query = "SELECT * FROM sessions WHERE array_to_string(subscriptions, ' , ') like '%$tag%'"
            val result : ResultSet = statement.executeQuery(query)
            while(result.next()) {
                sessions.add(
                    Session(
                        UUID.fromString(result.getString(Sessions.id.name)),
                        result.getString(Sessions.fcm.name),
                        ((result.getArray(Sessions.subscriptions.name) as PgArray).array as Array<*>).map { it as String }.toList(),
                        result.getDate(Sessions.createAt.name).toString(),
                        result.getDate(Sessions.changeAt.name).toString(),
                    )
                )
            }

        }
        return sessions.toList()
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
                        ((result.getArray(Sessions.subscriptions.name) as PgArray).array as Array<*>).map { it as String }.toList(),
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
                val conn = TransactionManager.current().connection
                val statement = conn.createStatement()
                val query = "UPDATE sessions SET fcm_token = '${fcm}', change_at = NOW() WHERE id = '${session.id}'"
                statement.execute(query)
            }
        }
    }

    override fun insertTag(tag: String, session : Session) {
        if (!session.subscriptions.contains(tag)) {
            transaction {
                val conn = TransactionManager.current().connection
                val statement = conn.createStatement()
                val query = "UPDATE sessions SET subscriptions = array_append(subscriptions, '${tag}'), change_at = NOW() WHERE id = '${session.id}'"
                statement.execute(query)
            }
        }
    }

    override fun removeTag(tag: String, session : Session) {
        if (session.subscriptions.contains(tag)) {
            transaction {
                val conn = TransactionManager.current().connection
                val statement = conn.createStatement()
                val query = "UPDATE sessions SET subscriptions = array_remove(subscriptions, '${tag}'), change_at = NOW() WHERE id = '${session.id}'"
                statement.execute(query)
            }
        }
    }
}