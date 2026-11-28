package com.hegocre.nextcloudpasswords.ui.components

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import com.hegocre.nextcloudpasswords.data.password.CustomField
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditableStateChangesTest {
    private val saverScope = SaverScope { true }

    @Suppress("UNCHECKED_CAST")
    private fun <T> T.restoredBy(saver: Saver<T, *>): T {
        val anySaver = saver as Saver<T, Any>
        val saved = with(anySaver) { saverScope.save(this@restoredBy) }!!
        return anySaver.restore(saved)!!
    }

    @Test
    fun untouchedPasswordHasNoChanges() {
        assertFalse(EditablePasswordState(null).hasChanges())
    }

    @Test
    fun editedPasswordHasChanges() {
        val state = EditablePasswordState(null)

        state.label = "Nextcloud"

        assertTrue(state.hasChanges())
    }

    @Test
    fun passwordEditedBackToOriginalHasNoChanges() {
        val state = EditablePasswordState(null)

        state.label = "Nextcloud"
        state.label = ""

        assertFalse(state.hasChanges())
    }

    @Test
    fun addedCustomFieldIsAChange() {
        val state = EditablePasswordState(null)

        state.customFields.add(CustomField("PIN", CustomField.TYPE_SECRET, "1234"))

        assertTrue(state.hasChanges())
    }

    @Test
    fun restoredPasswordKeepsItsChanges() {
        val state = EditablePasswordState(null).apply { username = "admin" }

        val restored = state.restoredBy(EditablePasswordState.Saver)

        assertTrue(restored.hasChanges())
    }

    @Test
    fun restoredUntouchedPasswordHasNoChanges() {
        val restored = EditablePasswordState(null).restoredBy(EditablePasswordState.Saver)

        assertFalse(restored.hasChanges())
    }

    @Test
    fun defaultFolderOfNewPasswordIsNotAChange() {
        assertFalse(EditablePasswordState(null, defaultFolder = "work-folder-id").hasChanges())
    }

    @Test
    fun defaultParentOfNewFolderIsNotAChange() {
        assertFalse(EditableFolderState(null, defaultParent = "work-folder-id").hasChanges())
    }

    @Test
    fun untouchedFolderHasNoChanges() {
        assertFalse(EditableFolderState(null).hasChanges())
    }

    @Test
    fun editedFolderHasChanges() {
        val state = EditableFolderState(null)

        state.favorite = true

        assertTrue(state.hasChanges())
    }

    @Test
    fun restoredFolderKeepsItsChanges() {
        val state = EditableFolderState(null).apply { label = "Work" }

        val restored = state.restoredBy(EditableFolderState.Saver)

        assertTrue(restored.hasChanges())
    }
}
