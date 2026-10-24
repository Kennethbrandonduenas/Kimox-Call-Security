package com.kimoxcallsecurity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CallRuleEvaluatorTest {
    private fun input(
        number: String? = "51999111222",
        isPrivate: Boolean = false,
        isKnownContact: Boolean? = true,
        whitelist: Set<String> = emptySet(),
        blockedNumbers: Set<String> = emptySet(),
        blockedPrefixes: List<String> = emptyList(),
        blockedCountries: List<String> = emptyList(),
        blockPrivate: Boolean = false,
        blockUnknown: Boolean = false,
    ) = CallRuleInput(number, isPrivate, isKnownContact, whitelist, blockedNumbers, blockedPrefixes, blockedCountries, blockPrivate, blockUnknown)

    @Test fun whitelistAlwaysWins() {
        assertNull(CallRuleEvaluator.evaluate(input(whitelist = setOf("51999111222"), blockedNumbers = setOf("51999111222"), blockUnknown = true, isKnownContact = false)))
    }

    @Test fun detectsEveryBlockingRule() {
        assertEquals("Número privado", CallRuleEvaluator.evaluate(input(number = null, isPrivate = true, blockPrivate = true)))
        assertEquals("Número bloqueado", CallRuleEvaluator.evaluate(input(blockedNumbers = setOf("51999111222"))))
        assertEquals("Prefijo bloqueado", CallRuleEvaluator.evaluate(input(blockedPrefixes = listOf("5199"))))
        assertEquals("País bloqueado", CallRuleEvaluator.evaluate(input(blockedCountries = listOf("51"))))
        assertEquals("Número desconocido", CallRuleEvaluator.evaluate(input(isKnownContact = false, blockUnknown = true)))
    }

    @Test fun missingContactsPermissionDoesNotBlockUnknownCalls() {
        assertNull(CallRuleEvaluator.evaluate(input(isKnownContact = null, blockUnknown = true)))
    }
}
