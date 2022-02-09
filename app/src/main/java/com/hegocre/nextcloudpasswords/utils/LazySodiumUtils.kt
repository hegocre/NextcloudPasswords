package com.hegocre.nextcloudpasswords.utils

import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid

/**
 * Singleton class used to provide the [LazySodiumAndroid] instance. This is used to avoid creating new instances
 * and potentially crashing the application.
 *
 */
class LazySodiumUtils private constructor() {
    companion object {
        var instance: LazySodiumAndroid? = null

        /**
         * Get the [LazySodiumAndroid] instance, and create one if not present.
         *
         * @return A LazySodium instance
         */
        fun getSodium(): LazySodiumAndroid {
            synchronized(this) {
                var tempInstance = instance

                if (tempInstance == null) {
                    tempInstance = LazySodiumAndroid(SodiumAndroid())
                    instance = tempInstance
                }

                return tempInstance
            }
        }
    }
}