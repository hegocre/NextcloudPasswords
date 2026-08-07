package com.hegocre.nextcloudpasswords

import com.hegocre.nextcloudpasswords.api.SessionRequestOutcome
import com.hegocre.nextcloudpasswords.api.classifySessionRequest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for the classification of an initial `session/request` response.
 *
 * A `401` must be recoverable (not a permanent deauthorization) so an ambiguous
 * or transient auth failure does not wipe the locally cached vault; only a `403`
 * deauthorizes the client.
 */
class SessionRequestClassificationTest {
    @Test
    fun okIsSuccess() {
        assertEquals(SessionRequestOutcome.SUCCESS, classifySessionRequest(200))
    }

    @Test
    fun unauthorizedIsRecoverable() {
        assertEquals(SessionRequestOutcome.RECOVERABLE_AUTH_ERROR, classifySessionRequest(401))
    }

    @Test
    fun forbiddenIsDeauthorized() {
        assertEquals(SessionRequestOutcome.DEAUTHORIZED, classifySessionRequest(403))
    }

    @Test
    fun otherCodesAreBadResponse() {
        assertEquals(SessionRequestOutcome.BAD_RESPONSE, classifySessionRequest(500))
        assertEquals(SessionRequestOutcome.BAD_RESPONSE, classifySessionRequest(404))
        assertEquals(SessionRequestOutcome.BAD_RESPONSE, classifySessionRequest(0))
    }
}
