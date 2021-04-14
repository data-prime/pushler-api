package com.pushler.datasource.interfaces

import com.pushler.dto.User

interface UserDataSource {

    fun create(channel: User)

    fun getByName(userName : String) : User?

    fun get(id : String) : User?
//
//    fun update(user : User) : User?
//
//    fun getAll() : List<User>
//
    fun delete(uuid: String)

    fun deleteByName(name: String)
}