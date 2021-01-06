package com.sachin.app.chat.model

import java.util.*

data class ReceivedInvite(val user: User? = null, var time: Long? = Date().time)