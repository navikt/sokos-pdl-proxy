package no.nav.sokos.pdl.proxy.listener

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.configureFor
import io.kotest.core.listeners.TestListener
import io.kotest.core.spec.Spec
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.sokos.pdl.proxy.security.AccessTokenClient

private const val WIREMOCK_SERVER_PORT = 9001

object WiremockListener : TestListener {
    val wiremock = WireMockServer(WIREMOCK_SERVER_PORT)
    val accessTokenClient = mockk<AccessTokenClient>()

    override suspend fun beforeSpec(spec: Spec) {
        configureFor(WIREMOCK_SERVER_PORT)
        wiremock.start()
        coEvery { accessTokenClient.hentAccessToken() } returns "token"
    }

    override suspend fun afterSpec(spec: Spec) {
        wiremock.stop()
    }
}
