package com.hegocre.nextcloudpasswords.databases

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.hegocre.nextcloudpasswords.data.favicon.Favicon
import com.hegocre.nextcloudpasswords.data.folder.Folder
import com.hegocre.nextcloudpasswords.data.password.Password
import com.hegocre.nextcloudpasswords.databases.favicondatabase.FaviconDatabaseDao
import com.hegocre.nextcloudpasswords.databases.folderdatabase.FolderDatabaseDao
import com.hegocre.nextcloudpasswords.databases.passworddatabase.PasswordDatabaseDao

@Database(
    entities = [Favicon::class, Folder::class, Password::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract val faviconDao: FaviconDatabaseDao
    abstract val passwordDao: PasswordDatabaseDao
    abstract val folderDao: FolderDatabaseDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            synchronized(this) {
                var tempInstance = instance
                if (tempInstance == null) {
                    tempInstance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "password.db"
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