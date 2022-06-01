package no.nav.sokos.pdl.proxy.pdl.security

import com.fasterxml.jackson.annotation.JsonAlias
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.Parameters
import java.time.Instant
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging
import no.nav.sokos.pdl.proxy.config.Configuration
import no.nav.sokos.pdl.proxy.util.retry

private val logger = KotlinLogging.logger {}

class AccessTokenClient(
    private val azureAd: Configuration.AzureAdClient,
    private val client: HttpClient,
    private val aadAccessTokenUrl: String = "https://login.microsoftonline.com/${azureAd.tenant}/oauth2/v2.0/token"
) {
    private val mutex = Mutex()

    @Volatile
    private var token: AccessToken = runBlocking { AccessToken(hentAccessTokenFraProvider()) }
    suspend fun hentAccessToken(): String {
        val omToMinutter = Instant.now().plusSeconds(120L)
        return mutex.withLock {
            when {
                token.expiresAt.isBefore(omToMinutter) -> {
                    logger.info("henter ny token")
                    token = AccessToken(hentAccessTokenFraProvider())
                    token.accessToken
                }
                else -> token.accessToken
            }
        }
    }

    private suspend fun hentAccessTokenFraProvider(): AzureAccessToken =
        retry {
            client.post(aadAccessTokenUrl) {
                accept(ContentType.Application.Json)
                method = HttpMethod.Post
                setBody(FormDataContent(Parameters.build {
                    append("tenant", azureAd.tenant)
                    append("client_id", azureAd.clientId)
                    append("scope", "api://${azureAd.pdlClientId}/.default")
                    append("client_secret", azureAd.clientSecret)
                    append("grant_type", "client_credentials")
                }))
            }.body()
        }
}

private data class AzureAccessToken(
    @JsonAlias("access_token")
    val accessToken: String,
    @JsonAlias("expires_in")
    val expiresIn: Long
)

private data class AccessToken(
    val accessToken: String,
    val expiresAt: Instant
) {
    constructor(azureAccessToken: AzureAccessToken) : this(
        accessToken = azureAccessToken.accessToken,
        expiresAt = Instant.now().plusSeconds(azureAccessToken.expiresIn)
    )
}