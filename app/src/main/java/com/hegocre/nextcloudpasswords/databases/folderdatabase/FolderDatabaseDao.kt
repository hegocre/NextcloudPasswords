package com.hegocre.nextcloudpasswords.databases.folderdatabase

import androidx.lifecycle.LiveData
import androidx.room.*
import com.hegocre.nextcloudpasswords.data.folder.Folder

@Dao
interface FolderDatabaseDao {
    @Query("SELECT * FROM folders")
    fun fetchAllFolders(): LiveData<List<Folder>>

    @Query("SELECT id FROM folders")
    suspend fun fetchAllFoldersId(): List<String>

    @Query("SELECT revision FROM folders WHERE id = :id")
    suspend fun getFolderRevision(id: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: Folder)

    @Update
    suspend fun updateFolder(folder: Folder)

    @Delete
    suspend fun deleteFolder(folder: Folder)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteFolder(id: String)

    @Query("DELETE FROM folders")
    suspend fun deleteDatabase()
}