package com.hegocre.nextcloudpasswords.data.favicon

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Data class representing a favicon associated with an URL. Used to cache the favicons on a
 * Room database.
 *
 * @property url The URL of the favicon.
 * @property data The favicon of the URL.
 */
@Entity(tableName = "favicons", indices = [Index(value = ["url"], unique = true)])
data class Favicon(
    @PrimaryKey
    val url: String,
    val data: ByteArray
) {
    /* Generated by Android Studio */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Favicon

        if (url != other.url) return false
        return data.contentEquals(other.data)
    }

    /* Generated by Android Studio */
    override fun hashCode(): Int {
        var result = url.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}
