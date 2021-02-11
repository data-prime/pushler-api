package me.simplepush.datasource

import me.simplepush.datasource.interfaces.SessionDataSource
import me.simplepush.datasource.tables.Sessions
import me.simplepush.dto.Session

import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.postgresql.jdbc.PgArray
import java.sql.ResultSet
import java.util.*

class SessionDataBaseDataSource : SessionDataSource {


    override fun insertSession(firebase: String): Session {
        val session = Session(
            UUID.randomUUID(),
            firebase,
            listOf()
        )

        transaction {
            val conn = TransactionManager.current().connection
            val statement = conn.createStatement()
            val query = "INSERT INTO sessions (firebase, id, tags) VALUES ('${session.firebase}', '${session.id}', ARRAY${session.tags?.map { "'${it}'" }.toString()}::text[])"
            statement.execute(query)
        }

        return session
    }

    override fun insertTag(tag: String, session : Session) {
        if (session.tags?.contains(tag) == false) {
            transaction {
                val conn = TransactionManager.current().connection
                val statement = conn.createStatement()
                val query = "UPDATE sessions SET tags = array_append(tags, '${tag}') WHERE id = '${session.id}'"
                statement.execute(query)
            }
        }
    }

    override fun getSessionsFromTag(tag: String) : List<Session> {
        val sessions: MutableList<Session> = mutableListOf()



        transaction {
            val conn = TransactionManager.current().connection
            val statement = conn.createStatement()
            val query = "SELECT * FROM sessions WHERE array_to_string(tags, ' , ') like '%$tag%'"
            val result : ResultSet = statement.executeQuery(query)
            while(result.next()) {
                sessions.add(
                    Session(
                        UUID.fromString(result.getString(Sessions.id.name)),
                        result.getString(Sessions.firebase.name),
                        ((result.getArray(Sessions.tags.name) as PgArray).array as Array<*>).map { it as String }.toList()
                    )
                )
            }

        }
        return sessions.toList()
    }

    override fun getSession(uuid: String) : Session? {
        var session: Session? = null

        transaction {
            Sessions.select { Sessions.id eq UUID.fromString(uuid) }.map {
                session = Session(
                    it[Sessions.id],
                    it[Sessions.firebase],
                    it[Sessions.tags],
                )
            }
        }

        return session
    }

}