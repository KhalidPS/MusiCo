package com.k.sekiro.musico.playmusic.domain

interface SimpleDataSaver{
    suspend fun <T> suspendSave(key: String, value: T)


    suspend fun <T> suspendGet(key: String, default: T): T

    suspend fun <T:Any> suspendSave(vararg values: Pair<String,T>)
    fun <T> save(key: String, value: T)


    fun <T> get(key: String, default: T): T

    fun <T:Any> save(vararg values: Pair<String,T>)





}