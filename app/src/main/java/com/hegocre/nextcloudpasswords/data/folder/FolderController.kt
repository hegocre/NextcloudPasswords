package com.hegocre.nextcloudpasswords.data.folder

import android.content.Context
import androidx.lifecycle.LiveData
import com.hegocre.nextcloudpasswords.api.ApiController
import com.hegocre.nextcloudpasswords.databases.folderdatabase.FolderDatabase
import com.hegocre.nextcloudpasswords.utils.Result

/**
 * Class used to manage the folders cache and make requests to the [ApiController] folder methods.
 * This is a Singleton class and will have only one instance.
 *
 * @param context Context of the application
 */
class FolderController private constructor(context: Context) {
    private val folderDatabase = FolderDatabase.getInstance(context)
    private val apiController = ApiController.getInstance(context)

    /**
     * Sync the folders obtained from the [ApiController] with the cached ones.
     *
     */
    suspend fun syncFolders() {
        val result = apiController.listFolders()
        if (result is Result.Success) {
            val savedFoldersSet = folderDatabase.folderDao.fetchAllFoldersId().toHashSet()
            for (folder in result.data) {
                val oldRevision = folderDatabase.folderDao.getFolderRevision(folder.id)
                if (oldRevision == null || oldRevision != folder.revision) {
                    folderDatabase.folderDao.insertFolder(folder)
                }
                savedFoldersSet.remove(folder.id)
            }
            for (id in savedFoldersSet) {
                folderDatabase.folderDao.deleteFolder(id)
            }
        }
    }

    fun getFolders(): LiveData<List<Folder>> =
        folderDatabase.folderDao.fetchAllFolders()

    companion object {
        private var instance: FolderController? = null

        /**
         * Get the instance of the [FolderController], and create it if null.
         *
         * @param context Context of the application.
         * @return The instance of the controller.
         */
        fun getInstance(context: Context): FolderController {
            synchronized(this) {
                var tempInstance = instance

                if (tempInstance == null) {
                    tempInstance = FolderController(context)
                    instance = tempInstance
                }

                return tempInstance
            }
        }
    }
}