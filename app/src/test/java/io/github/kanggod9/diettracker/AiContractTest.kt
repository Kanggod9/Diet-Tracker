package io.github.kanggod9.diettracker

import io.github.kanggod9.diettracker.domain.NutrientKey
import io.github.kanggod9.diettracker.integration.AiContractParser
import io.github.kanggod9.diettracker.integration.DeterministicFakePhotoProvider
import io.github.kanggod9.diettracker.integration.GatewayConnection
import org.junit.Assert.*
import org.junit.Test

class AiContractTest {
    @Test fun parserKeepsJsonNullUnknown() {
        val draft = AiContractParser.parse(DeterministicFakePhotoProvider.DEMO_RESPONSE)
        assertEquals(420.0, draft.nutrients[NutrientKey.ENERGY]!!, 0.0)
        assertNull(draft.nutrients[NutrientKey.SODIUM])
    }

    @Test(expected = IllegalArgumentException::class) fun gatewayRejectsHttp() { GatewayConnection("http://example.com/analyze", "1234567890123456") }
    @Test(expected = IllegalArgumentException::class) fun gatewayRejectsEmbeddedCredentials() { GatewayConnection("https://key@example.com/analyze", "1234567890123456") }
}
