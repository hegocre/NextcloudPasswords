package com.hegocre.nextcloudpasswords.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DynamicColorDefaultTest {
    @Test
    fun dynamicColorIsOnByDefaultWhenSupported() {
        assertTrue(
            resolveUseSystemDynamicColor(
                storedValue = null,
                useInstanceColor = false,
                isDynamicColorSupported = true
            )
        )
    }

    @Test
    fun dynamicColorIsOffByDefaultWhenNotSupported() {
        assertFalse(
            resolveUseSystemDynamicColor(
                storedValue = null,
                useInstanceColor = false,
                isDynamicColorSupported = false
            )
        )
    }

    @Test
    fun dynamicColorIsOffByDefaultWhenInstanceColorIsUsed() {
        assertFalse(
            resolveUseSystemDynamicColor(
                storedValue = null,
                useInstanceColor = true,
                isDynamicColorSupported = true
            )
        )
    }

    @Test
    fun explicitlyDisabledDynamicColorStaysDisabled() {
        assertFalse(
            resolveUseSystemDynamicColor(
                storedValue = false,
                useInstanceColor = false,
                isDynamicColorSupported = true
            )
        )
    }

    @Test
    fun explicitlyEnabledDynamicColorStaysEnabled() {
        assertTrue(
            resolveUseSystemDynamicColor(
                storedValue = true,
                useInstanceColor = false,
                isDynamicColorSupported = true
            )
        )
    }
}
