package com.pushler.dto

import java.util.*

data class Subscribe(
    val channel: Channel,
    val subscribe: Boolean,
    val tag: String?,
)
