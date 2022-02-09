package com.hegocre.nextcloudpasswords.databases.folderdatabase

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.hegocre.nextcloudpasswords.data.folder.Folder

@Database(entities = [Folder::class], version = 3, exportSchema = false)
abstract class FolderDatabase : RoomDatabase() {
    abstract val folderDao: FolderDatabaseDao

    companion object {
        @Volatile
        private var instance: FolderDatabase? = null

        fun getInstance(context: Context): FolderDatabase {
            synchronized(this) {
                var tempInstance = instance

                if (tempInstance == null) {
                    tempInstance = Room.databaseBuilder(
                        context.applicationContext,
                        FolderDatabase::class.java,
                        "folders.db"
                    )
                        .fallbackToDestructiveMigration()
                        .build()

                    instance = tempInstance
                }
                return tempInstance
            }
        }
    }

}