package com.hegocre.nextcloudpasswords.data.password

import android.content.Context
import androidx.lifecycle.LiveData
import com.hegocre.nextcloudpasswords.api.ApiController
import com.hegocre.nextcloudpasswords.databases.AppDatabase
import com.hegocre.nextcloudpasswords.utils.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Class used to manage the passwords cache and make requests to the [ApiController] password methods.
 * This is a Singleton class and will have only one instance.
 *
 * @param context Context of the application
 */
class PasswordController private constructor(context: Context) {
    private val passwordDatabase = AppDatabase.getInstance(context)
    private val apiController = ApiController.getInstance(context)

    /**
     * Sync the passwords obtained from the [ApiController] with the cached ones.
     *
     */
    suspend fun syncPasswords() {
        withContext(Dispatchers.IO) {
            val result = apiController.listPasswords()
            if (result is Result.Success) {
                val savedPasswordsSet =
                    passwordDatabase.passwordDao.fetchAllPasswordsId().toHashSet()
                for (password in result.data) {
                    val oldRevision = passwordDatabase.passwordDao.getPasswordRevision(password.id)
                    if (oldRevision == null || oldRevision != password.revision) {
                        passwordDatabase.passwordDao.insertPassword(password)
                    }
                    savedPasswordsSet.remove(password.id)
                }
                for (id in savedPasswordsSet) {
                    passwordDatabase.passwordDao.deletePassword(id)
                }
            }
        }
    }

    fun getPasswords(): LiveData<List<Password>> =
        passwordDatabase.passwordDao.fetchAllPasswords()

    companion object {
        private var instance: PasswordController? = null

        /**
         * Get the instance of the [PasswordController], and create it if null.
         *
         * @param context Context of the application.
         * @return The instance of the controller.
         */
        fun getInstance(context: Context): PasswordController {
            synchronized(this) {
                var tempInstance = instance

                if (tempInstance == null) {
                    tempInstance = PasswordController(context)
                    instance = tempInstance
                }

                return tempInstance
            }
        }
    }
}