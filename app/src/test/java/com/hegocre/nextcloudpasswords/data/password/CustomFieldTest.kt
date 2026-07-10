package com.hegocre.nextcloudpasswords.data.password

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test for [CustomField.isSensitive], which decides whether a custom field value
 * must be flagged as sensitive when copied to the clipboard.
 */
class CustomFieldTest {
    private fun field(type: String) = CustomField(label = "label", type = type, value = "value")

    @Test
    fun secretFieldIsSensitive() {
        assertTrue(field(CustomField.TYPE_SECRET).isSensitive)
    }

    @Test
    fun nonSecretFieldsAreNotSensitive() {
        assertFalse(field(CustomField.TYPE_TEXT).isSensitive)
        assertFalse(field(CustomField.TYPE_EMAIL).isSensitive)
        assertFalse(field(CustomField.TYPE_URL).isSensitive)
        assertFalse(field(CustomField.TYPE_FILE).isSensitive)
        assertFalse(field(CustomField.TYPE_DATA).isSensitive)
    }

    @Test
    fun unknownTypeIsNotSensitive() {
        assertFalse(field("something-else").isSensitive)
    }
}
