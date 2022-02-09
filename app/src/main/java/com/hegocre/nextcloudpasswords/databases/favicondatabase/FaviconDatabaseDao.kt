package com.hegocre.nextcloudpasswords.databases.favicondatabase

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hegocre.nextcloudpasswords.data.favicon.Favicon

@Dao
interface FaviconDatabaseDao {
    @Query("SELECT data from favicons WHERE url = :url")
    suspend fun getFavicon(url: String): ByteArray?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addFavicon(favicon: Favicon)

    @Query("DELETE FROM favicons")
    suspend fun deleteDatabase()
}