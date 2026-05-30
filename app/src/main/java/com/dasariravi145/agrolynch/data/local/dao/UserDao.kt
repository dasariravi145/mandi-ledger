package com.dasariravi145.agrolynch.data.local.dao

import androidx.room.*
import com.dasariravi145.agrolynch.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE phoneNumber = :phone")
    suspend fun getUserByPhone(phone: String): UserEntity?
}
