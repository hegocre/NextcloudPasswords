package com.hegocre.nextcloudpasswords

import com.hegocre.nextcloudpasswords.data.password.Password
import com.hegocre.nextcloudpasswords.ui.components.folderPasswordCounts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for the per-folder password counting used by the folder list.
 */
class FolderPasswordCountTest {
    private fun password(
        id: String,
        folder: String,
        hidden: Boolean = false,
        trashed: Boolean = false
    ) = Password(
        id = id,
        label = id,
        username = "",
        password = "",
        url = "",
        notes = "",
        customFields = "",
        status = 0,
        statusCode = "GOOD",
        hash = "",
        folder = folder,
        revision = "",
        share = null,
        shared = false,
        cseType = "",
        cseKey = "",
        sseType = "",
        client = "",
        hidden = hidden,
        trashed = trashed,
        favorite = false,
        editable = true,
        edited = 0,
        created = 0,
        updated = 0
    )

    private val folderA = "folder-a"
    private val folderB = "folder-b"

    @Test
    fun countsPasswordsPerFolder() {
        val counts = folderPasswordCounts(
            listOf(
                password("1", folderA),
                password("2", folderA),
                password("3", folderB)
            )
        )
        assertEquals(2, counts[folderA])
        assertEquals(1, counts[folderB])
    }

    @Test
    fun folderWithoutPasswordsIsAbsent() {
        val counts = folderPasswordCounts(listOf(password("1", folderA)))
        assertNull(counts["empty-folder"])
    }

    @Test
    fun emptyListYieldsEmptyMap() {
        assertEquals(emptyMap<String, Int>(), folderPasswordCounts(emptyList()))
    }

    @Test
    fun hiddenAndTrashedPasswordsAreExcluded() {
        val counts = folderPasswordCounts(
            listOf(
                password("1", folderA),
                password("2", folderA, hidden = true),
                password("3", folderA, trashed = true)
            )
        )
        assertEquals(1, counts[folderA])
    }

    @Test
    fun folderWithOnlyHiddenOrTrashedPasswordsIsAbsent() {
        val counts = folderPasswordCounts(
            listOf(
                password("1", folderA, hidden = true),
                password("2", folderA, trashed = true)
            )
        )
        assertNull(counts[folderA])
    }
}
