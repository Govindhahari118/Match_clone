package com.match.app.core.telemetry

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MatreeTelemetryContractTest {

    @Test
    fun `analytics schema contains no sensitive parameter names`() {
        val forbiddenFragments = setOf(
            "phone",
            "email",
            "message",
            "body",
            "token",
            "latitude",
            "longitude",
            "location",
            "address",
            "document",
            "kyc",
            "identity",
            "birth",
            "dob",
            "contact_value",
            "purchase_token"
        )

        MatreeTelemetry.Event.entries.forEach { event ->
            event.allowedParameters.forEach { key ->
                forbiddenFragments.forEach { forbidden ->
                    assertFalse(
                        "${event.key} exposes sensitive analytics key $key",
                        key.lowercase().contains(forbidden)
                    )
                }
            }
        }
    }

    @Test
    fun `analytics event names remain bounded and production safe`() {
        MatreeTelemetry.Event.entries.forEach { event ->
            assertTrue(event.key.length in 1..40)
            assertTrue(event.key.matches(Regex("[a-z][a-z0-9_]*")))
            event.allowedParameters.forEach { key ->
                assertTrue(key.length in 1..40)
                assertTrue(key.matches(Regex("[a-z][a-z0-9_]*")))
            }
        }
    }
}
