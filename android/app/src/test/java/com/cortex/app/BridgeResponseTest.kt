package com.cortex.app

import com.cortex.app.bridge.BridgeResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BridgeResponseTest {

    @Test
    fun testBridgeResponse_Ok() {
        val resp = BridgeResponse.ok("success_payload")
        assertTrue(resp.success)
        assertEquals("success_payload", resp.data)
        val json = resp.toJsonString()
        assertTrue(json.contains("\"success\":true"))
        assertTrue(json.contains("\"data\":\"success_payload\""))
    }

    @Test
    fun testBridgeResponse_Err() {
        val resp = BridgeResponse.err("Something went wrong")
        assertTrue(!resp.success)
        assertEquals("Something went wrong", resp.error)
        val json = resp.toJsonString()
        assertTrue(json.contains("\"success\":false"))
        assertTrue(json.contains("\"error\":\"Something went wrong\""))
    }
}
