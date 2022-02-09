package com.hegocre.nextcloudpasswords.databases.passworddatabase

import androidx.lifecycle.LiveData
import androidx.room.*
import com.hegocre.nextcloudpasswords.data.password.Password

@Dao
interface PasswordDatabaseDao {
    @Query("SELECT * FROM passwords")
    fun fetchAllPasswords(): LiveData<List<Password>>

    @Query("SELECT id FROM passwords")
    suspend fun fetchAllPasswordsId(): List<String>

    @Query("SELECT revision FROM passwords WHERE id = :id")
    suspend fun getPasswordRevision(id: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPassword(password: Password)

    @Update
    suspend fun updatePassword(password: Password)

    @Delete
    suspend fun deletePassword(password: Password)

    @Query("DELETE FROM passwords WHERE id = :id")
    suspend fun deletePassword(id: String)

    @Query("DELETE FROM passwords")
    suspend fun deleteDatabase()
}