package com.hegocre.nextcloudpasswords.databases.passworddatabase

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.hegocre.nextcloudpasswords.data.password.Password

@Database(entities = [Password::class], version = 8, exportSchema = false)
abstract class PasswordDatabase : RoomDatabase() {
    abstract val passwordDao: PasswordDatabaseDao

    companion object {
        @Volatile
        private var instance: PasswordDatabase? = null

        fun getInstance(context: Context): PasswordDatabase {
            synchronized(this) {
                var tempInstance = instance

                if (tempInstance == null) {
                    tempInstance = Room.databaseBuilder(
                        context.applicationContext,
                        PasswordDatabase::class.java,
                        "passwords.db"
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