package com.sachin.app.chat.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.sachin.app.chat.model.Friend

@Dao
interface FriendDao {
    @Query("SELECT * FROM friend ORDER BY name ASC")
    fun getAllFriendsLive(): LiveData<List<Friend>>

    @Query("SELECT * FROM friend ORDER BY name ASC")
    fun getAllFriends(): List<Friend>

    @Query("SELECT * FROM friend WHERE uid = :uid")
    suspend fun findByUid(uid: String): Friend?

/*
    @Query("SELECT * FROM friend WHERE name LIKE :name LIMIT 1")
    suspend fun findByName(name: String): Friend*/

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(friend: Friend)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(friend: Friend)

    @Delete
    suspend fun delete(friend: Friend)
/*
    @Query("DELETE FROM friend")
    suspend fun deleteAllFriends()*/
}