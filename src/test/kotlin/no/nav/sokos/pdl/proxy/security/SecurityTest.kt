package no.nav.sokos.pdl.proxy.security

import kotlinx.serialization.json.Json

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk

import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.mock.oauth2.withMockOAuth2Server
import no.nav.sokos.pdl.proxy.APPLICATION_JSON
import no.nav.sokos.pdl.proxy.PDL_PROXY_API_PATH
import no.nav.sokos.pdl.proxy.api.model.IdentRequest
import no.nav.sokos.pdl.proxy.api.pdlProxyApi
import no.nav.sokos.pdl.proxy.config.AUTHENTICATION_NAME
import no.nav.sokos.pdl.proxy.config.PropertiesConfig
import no.nav.sokos.pdl.proxy.config.authenticate
import no.nav.sokos.pdl.proxy.config.commonConfig
import no.nav.sokos.pdl.proxy.config.securityConfig
import no.nav.sokos.pdl.proxy.domain.PersonDetaljer
import no.nav.sokos.pdl.proxy.pdl.PdlClientService

private val pdlClientService: PdlClientService = mockk()

internal class SecurityTest :
    FunSpec({

        test("test http GET endepunkt uten token bør returnere 401") {
            withMockOAuth2Server {
                testApplication {
                    application {
                        securityConfig(true, authConfig())
                        routing {
                            authenticate(true, AUTHENTICATION_NAME) {
                                pdlProxyApi(pdlClientService)
                            }
                        }
                    }
                    val response = client.post(PDL_PROXY_API_PATH)
                    response.status shouldBe HttpStatusCode.Unauthorized
                }
            }
        }

        test("test http GET endepunkt med token bør returnere 200") {
            withMockOAuth2Server {
                testApplication {
                    val client =
                        createClient {
                            install(ContentNegotiation) {
                                json(
                                    Json {
                                        prettyPrint = true
                                        ignoreUnknownKeys = true
                                        encodeDefaults = true
                                        explicitNulls = false
                                    },
                                )
                            }
                        }
                    application {
                        commonConfig()
                        securityConfig(true, authConfig())
                        routing {
                            authenticate(true, AUTHENTICATION_NAME) {
                                pdlProxyApi(pdlClientService)
                            }
                        }
                    }

                    every { pdlClientService.hentPersonDetaljer(any()) } returns
                        PersonDetaljer(
                            emptyList(),
                            "Ola",
                            "Nordmann",
                            "Nordmann",
                            "Ola",
                            null,
                            emptyList(),
                            emptyList(),
                        )

                    val response =
                        client.post(PDL_PROXY_API_PATH) {
                            header(HttpHeaders.Authorization, "Bearer ${tokenFromDefaultProvider()}")
                            header(HttpHeaders.ContentType, APPLICATION_JSON)
                            setBody(IdentRequest("12345678901"))
                        }

                    response.status shouldBe HttpStatusCode.OK
                }
            }
        }
    })

private fun MockOAuth2Server.authConfig() =
    PropertiesConfig.AzureAdProperties(
        wellKnownUrl = wellKnownUrl("default").toString(),
        clientId = "default",
    )

private fun MockOAuth2Server.tokenFromDefaultProvider() =
    issueToken(
        issuerId = "default",
        clientId = "default",
        tokenCallback = DefaultOAuth2TokenCallback(),
    ).serialize()
