package no.nav.sokos.pdl.proxy.pdl.security

import com.fasterxml.jackson.annotation.JsonAlias
import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.Parameters
import java.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging
import no.nav.sokos.pdl.proxy.Configuration


private val LOGGER = KotlinLogging.logger {}


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
                    LOGGER.info("henter ny token")
                    token = AccessToken(hentAccessTokenFraProvider())
                    token.accessToken
                }
                else -> token.accessToken
            }
        }
    }

    //TODO when jackson is unable to marshall it leaks all data
    private suspend fun hentAccessTokenFraProvider(): AzureAccessToken =
        retry {
            client.post(aadAccessTokenUrl) {
                accept(ContentType.Application.Json)
                method = HttpMethod.Post
                body = FormDataContent(Parameters.build {
                    append("tenant", azureAd.tenant)
                    append("client_id", azureAd.clientId)
                    append("scope", "api://${azureAd.pdlClientId}/.default")
                    append("client_secret", azureAd.clientSecret)
                    append("grant_type", "client_credentials")
                })
            }
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


suspend fun <T> retry(
    numOfRetries: Int = 5,
    initialDelayMs: Long = 250,
    block: suspend () -> T,
): T {

    var throwable: Exception? = null
    for (n in 1..numOfRetries) {
        try {
            return block()
        } catch (ex: Exception) {
            throwable = ex
            delay(initialDelayMs)
        }
    }
    throw throwable!!
}