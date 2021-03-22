package com.pushler.datasource.interfaces

import com.pushler.dto.Session
import java.util.*

interface SessionDataSource {

    fun create() : Session

    fun get(uuid: String) : Session?

    fun getFromTag(tag: String) : List<Session>

    fun getAll() : List<Session>

    fun insertFCM(fcm: String, session : Session)

    fun insertTag(tag: String, session : Session)

    fun removeTag(tag: String, session : Session)



}