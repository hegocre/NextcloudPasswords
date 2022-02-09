package com.hegocre.nextcloudpasswords.databases.favicondatabase

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.hegocre.nextcloudpasswords.data.favicon.Favicon

@Database(entities = [Favicon::class], version = 1, exportSchema = false)
abstract class FaviconDatabase : RoomDatabase() {
    abstract val faviconDao: FaviconDatabaseDao

    companion object {
        @Volatile
        private var instance: FaviconDatabase? = null

        fun getInstance(context: Context): FaviconDatabase {
            synchronized(this) {
                var tempInstance = instance
                if (tempInstance == null) {
                    tempInstance = Room.databaseBuilder(
                        context.applicationContext,
                        FaviconDatabase::class.java,
                        "favicons.db"
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