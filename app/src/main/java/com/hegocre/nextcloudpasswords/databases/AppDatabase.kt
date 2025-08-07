package com.hegocre.nextcloudpasswords.databases

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteTable
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import com.hegocre.nextcloudpasswords.data.folder.Folder
import com.hegocre.nextcloudpasswords.data.password.Password
import com.hegocre.nextcloudpasswords.databases.folderdatabase.FolderDatabaseDao
import com.hegocre.nextcloudpasswords.databases.passworddatabase.PasswordDatabaseDao

@Database(
    entities = [Folder::class, Password::class],
    version = 3,
    autoMigrations = [
        AutoMigration(from = 2, to = 3, spec = AppDatabase.DeleteFaviconsMigration::class)
    ]
)
abstract class AppDatabase : RoomDatabase() {
    abstract val passwordDao: PasswordDatabaseDao
    abstract val folderDao: FolderDatabaseDao

    @DeleteTable(tableName = "favicons")
    class DeleteFaviconsMigration : AutoMigrationSpec

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
                        .fallbackToDestructiveMigration(true)
                        .build()

                    instance = tempInstance
                }

                return tempInstance
            }
        }
    }
}