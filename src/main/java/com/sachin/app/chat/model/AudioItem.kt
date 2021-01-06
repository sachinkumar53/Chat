package com.sachin.app.chat.model

data class AudioItem(
        val title: String,
        val artist: String,
        val datModified: Long,
        val duration: Long,
        val size: Long,
        val path: String,
        var isPlaying: Boolean = false
)