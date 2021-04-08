package com.pushler.datasource.interfaces

import com.pushler.dto.Channel
import java.util.*
import io.ktor.http.*

interface ChannelDataSource {

    fun get(uuid : String) : Channel?

    fun getAll() : List<Channel>

    fun getFromUser(user : UUID) : List<Channel>

    fun create(channel: Channel)

    fun update(channel: Channel)

    fun delete(uuid: String)

}