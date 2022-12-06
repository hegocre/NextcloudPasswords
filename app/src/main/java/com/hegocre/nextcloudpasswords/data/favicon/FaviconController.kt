package com.hegocre.nextcloudpasswords.data.favicon

import android.content.Context
import com.hegocre.nextcloudpasswords.api.ApiController
import com.hegocre.nextcloudpasswords.databases.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Class used to manage the favicon cache and make the requests to the [ApiController.getFavicon]
 * method. This is a Singleton class and will have only one instance.
 *
 * @param context Context of the application.
 */
class FaviconController private constructor(context: Context) {
    private val apiController = ApiController.getInstance(context)
    private val appDatabase = AppDatabase.getInstance(context)

    /**
     * Return a favicon for the url obtained from the [ApiController].
     *
     * @param url The url of the requested site favicon.
     * @return A Bitmap of the favicon.
     */
    suspend fun getOnlineFavicon(url: String): ByteArray? {
        val favicon = withContext(Dispatchers.IO) {
            apiController.getFavicon(url)
        }

        if (favicon != null) withContext(Dispatchers.IO) {
            appDatabase.faviconDao.addFavicon(Favicon(url, favicon))
        }

        return favicon
    }

    /**
     * Return the cached favicon if present.
     *
     * @param url The url of the requested site favicon.
     * @return A Bitmap of the favicon.
     */
    suspend fun getCachedFavicon(url: String): ByteArray? =
        appDatabase.faviconDao.getFavicon(url)

    companion object {
        private var instance: FaviconController? = null

        /**
         * Get the instance of the [FaviconController], and create it if null.
         *
         * @param context Context of the application.
         * @return The instance of the controller.
         */
        fun getInstance(context: Context): FaviconController {
            synchronized(this) {
                var tempInstance = instance

                if (tempInstance == null) {
                    tempInstance = FaviconController(context)
                    instance = tempInstance
                }

                return tempInstance
            }
        }
    }
}