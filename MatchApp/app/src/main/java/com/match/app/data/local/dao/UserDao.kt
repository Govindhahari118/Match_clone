package com.match.app.data.local.dao

import androidx.room.*
import com.match.app.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity): Long

    @Update
    suspend fun update(user: UserEntity)

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): UserEntity?

    @Query("SELECT * FROM users WHERE firebaseUid = :uid LIMIT 1")
    suspend fun findByFirebaseUid(uid: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id != :excludeId")
    suspend fun allExcluding(excludeId: Long): List<UserEntity>

    @Query("SELECT COUNT(*) FROM users WHERE isSeed = 1")
    suspend fun seedCount(): Int

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteById(userId: Long)

    @Query("UPDATE users SET lastActiveAt = :timestamp WHERE firebaseUid = :uid")
    suspend fun updateLastActiveAt(uid: String, timestamp: Long)

    @Query("UPDATE users SET isPremium = :premium WHERE id = :userId")
    suspend fun updatePremiumStatus(userId: Long, premium: Boolean)

    @Query("UPDATE users SET isVerified = :verified WHERE id = :userId")
    suspend fun updateVerifiedStatus(userId: Long, verified: Boolean)

    @Query("UPDATE users SET boostActiveUntil = :boostUntil WHERE id = :userId")
    suspend fun updateBoostExpiry(userId: Long, boostUntil: Long)

    @Query("UPDATE users SET isIncognito = :incognito WHERE id = :userId")
    suspend fun updateIncognitoMode(userId: Long, incognito: Boolean)

    @Query("UPDATE users SET bio = :bio WHERE id = :userId")
    suspend fun updateBio(userId: Long, bio: String)

    @Query("UPDATE users SET profileViewCount = profileViewCount + 1 WHERE id = :userId")
    suspend fun incrementProfileViewCount(userId: Long)
}
