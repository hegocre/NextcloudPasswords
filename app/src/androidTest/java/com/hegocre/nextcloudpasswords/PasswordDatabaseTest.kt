package com.hegocre.nextcloudpasswords

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.hegocre.nextcloudpasswords.data.password.Password
import com.hegocre.nextcloudpasswords.databases.AppDatabase
import com.hegocre.nextcloudpasswords.databases.passworddatabase.PasswordDatabaseDao
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import java.io.IOException

class PasswordDatabaseTest {

    @get:Rule
    val instantExecutorRUle: TestRule = InstantTaskExecutorRule()

    private lateinit var passwordDatabaseDao: PasswordDatabaseDao
    private lateinit var database: AppDatabase

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        passwordDatabaseDao = database.passwordDao
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    @Test
    @Throws(Exception::class)
    fun insertAndGet() = runTest {
        val password = Password(
            id = "",
            label = "Nextcloud",
            username = "john_doe",
            password = "secret_value",
            url = "https://nextcloud.com/",
            notes = "",
            customFields = "",
            status = 0,
            statusCode = "GOOD",
            hash = "",
            folder = "",
            revision = "",
            share = null,
            shared = false,
            cseType = "",
            cseKey = "",
            sseType = "",
            client = "",
            hidden = false,
            trashed = false,
            favorite = true,
            editable = true,
            edited = 0,
            created = 0,
            updated = 0
        )

        passwordDatabaseDao.insertPassword(password)

        passwordDatabaseDao.fetchAllPasswords().value?.let {
            assertEquals(it, listOf(password))
        }
    }
}