package com.example.moneycheck.llm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class OpenAiCompatibleEndpointTest {
    @Test
    fun buildsEndpointFromEditableBaseUrl() {
        assertEquals(
            "https://9router.com/v1/chat/completions",
            OpenAiCompatibleEndpoint.url(" https://9router.com/v1/ ", "chat/completions"),
        )
    }

    @Test
    fun addsV1WhenGivenNineRouterRootUrl() {
        assertEquals(
            "http://localhost:20128/v1/chat/completions",
            OpenAiCompatibleEndpoint.url("http://localhost:20128", "chat/completions"),
        )
    }

    @Test
    fun rejectsUrlWithQueryParameters() {
        assertThrows(IllegalArgumentException::class.java) {
            OpenAiCompatibleEndpoint.normalize("https://example.com/v1?token=secret")
        }
    }
}
