package com.hegocre.nextcloudpasswords

import com.hegocre.nextcloudpasswords.utils.AppLockHelper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the passcode attempt evaluation used by the app lock screen.
 */
class AppLockHelperTest {
    @Test
    fun emptyInputIsNeverRejected() {
        assertFalse(AppLockHelper.shouldRejectPasscodeAttempt("", 4))
        assertFalse(AppLockHelper.shouldRejectPasscodeAttempt("", null))
    }

    @Test
    fun incompleteInputIsNotRejected() {
        assertFalse(AppLockHelper.shouldRejectPasscodeAttempt("12", 4))
        assertFalse(AppLockHelper.shouldRejectPasscodeAttempt("123", 4))
    }

    @Test
    fun fullLengthIncorrectInputIsRejected() {
        assertTrue(AppLockHelper.shouldRejectPasscodeAttempt("9999", 4))
    }

    @Test
    fun overLengthInputIsRejected() {
        // Guards against physical keyboard input exceeding the passcode length
        assertTrue(AppLockHelper.shouldRejectPasscodeAttempt("99999", 4))
    }

    @Test
    fun unreadablePasscodeRejectsAnyNonEmptyInput() {
        // When the stored passcode cannot be read, the code can never be
        // verified, so no input should be silently accepted
        assertTrue(AppLockHelper.shouldRejectPasscodeAttempt("1", null))
        assertTrue(AppLockHelper.shouldRejectPasscodeAttempt("123456", null))
    }

    @Test
    fun longPasscodeIsSupported() {
        // Passcodes longer than 9 digits must still be handled (they used to
        // overflow when parsed as an Int)
        assertFalse(AppLockHelper.shouldRejectPasscodeAttempt("1234567890", 12))
        assertTrue(AppLockHelper.shouldRejectPasscodeAttempt("123456789012", 12))
    }
}
