package com.billgenie.dao

import androidx.room.*
import com.billgenie.model.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    
    @Query("SELECT * FROM users WHERE isActive = 1 ORDER BY fullName")
    fun getAllActiveUsers(): Flow<List<User>>
    
    @Query("SELECT * FROM users ORDER BY isActive DESC, fullName")
    fun getAllUsers(): Flow<List<User>>
    
    @Query("SELECT * FROM users ORDER BY isActive DESC, fullName")
    suspend fun getAllUsersOnce(): List<User>
    
    @Query("SELECT * FROM users WHERE username = :username AND isActive = 1 LIMIT 1")
    suspend fun getUserByUsername(username: String): User?
    
    @Query("SELECT * FROM users WHERE username = :username AND password = :password AND isActive = 1 LIMIT 1")
    suspend fun authenticateUser(username: String, password: String): User?
    
    @Query("SELECT COUNT(*) FROM users WHERE isActive = 1")
    suspend fun getUserCount(): Int
    
    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: Int): User?
    
    @Insert
    suspend fun insertUser(user: User): Long
    
    @Update
    suspend fun updateUser(user: User)
    
    @Query("UPDATE users SET isActive = 0 WHERE id = :userId")
    suspend fun deactivateUser(userId: Int)
    
    @Delete
    suspend fun deleteUser(user: User)
    
    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUserById(userId: Int)
    
    @Query("UPDATE users SET lastLoginAt = :loginTime WHERE id = :userId")
    suspend fun updateLastLogin(userId: Int, loginTime: java.util.Date)
    
    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE username = :username AND isActive = 1)")
    suspend fun isUsernameExists(username: String): Boolean
    
    @Query("SELECT * FROM users WHERE email = :email AND isActive = 1 LIMIT 1")
    suspend fun getUserByEmail(email: String): User?
}