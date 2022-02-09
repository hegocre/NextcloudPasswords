package com.hegocre.nextcloudpasswords

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.hegocre.nextcloudpasswords.data.password.Password
import com.hegocre.nextcloudpasswords.databases.passworddatabase.PasswordDatabase
import com.hegocre.nextcloudpasswords.databases.passworddatabase.PasswordDatabaseDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
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
    private lateinit var passwordDatabase: PasswordDatabase

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        passwordDatabase = Room.inMemoryDatabaseBuilder(context, PasswordDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        passwordDatabaseDao = passwordDatabase.passwordDao
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        passwordDatabase.close()
    }

    @ExperimentalCoroutinesApi
    @Test
    @Throws(Exception::class)
    fun insertAndGet() = runBlockingTest {
        val password = Password(
            id = "",
            label = "GitHub",
            username = "hegocre",
            password = "helloWorld",
            url = "",
            revision = ""
        )

        passwordDatabaseDao.insertPassword(password)

        passwordDatabaseDao.fetchAllPasswords().value?.let {
            assertEquals(it, listOf(password))
        }
    }
}