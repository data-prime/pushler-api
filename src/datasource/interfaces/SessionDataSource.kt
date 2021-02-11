package me.simplepush.datasource.interfaces

import me.simplepush.dto.Session
import java.util.*

interface SessionDataSource {

    fun getSession(uuid: String) : Session?

    fun getSessionsFromTag(tag: String) : List<Session>

    fun insertSession(firebase: String) : Session

    fun insertTag(tag: String, session : Session)

}