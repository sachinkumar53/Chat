package com.sachin.app.chat.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.sachin.app.chat.model.Conversation

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversation ORDER BY timeStamp DESC")
    fun getAllConversationsLive(): LiveData<List<Conversation>>

    @Query("SELECT * FROM conversation ORDER BY timeStamp DESC")
    fun getAllConversations(): List<Conversation>

    @Query("SELECT * FROM conversation WHERE senderUid = :senderUid")
    suspend fun findByUid(senderUid: String): Conversation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conversation: Conversation)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(conversation: Conversation)

    @Delete
    suspend fun delete(conversation: Conversation)
}