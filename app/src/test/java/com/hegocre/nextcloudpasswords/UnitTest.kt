package com.hegocre.nextcloudpasswords

import com.hegocre.nextcloudpasswords.utils.sha1Hash
import org.junit.Assert.assertEquals
import org.junit.Test


/**
 * Unit which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */

class UnitTest {
    @Test
    fun sha1Test() {
        val hash = "test_string".sha1Hash()
        assertEquals(hash, "c58efadcf9f6b303e44e4a62dd984bbc8cae6e99")
    }
}