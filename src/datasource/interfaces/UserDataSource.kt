package com.pushler.datasource.interfaces

import com.pushler.dto.User

interface UserDataSource {

    fun create(channel: User)

    fun get(userName : String) : User?
//
//    fun update(user : User) : User?
//
//    fun getAll() : List<User>
//
//    fun delete(uuid: String)

}