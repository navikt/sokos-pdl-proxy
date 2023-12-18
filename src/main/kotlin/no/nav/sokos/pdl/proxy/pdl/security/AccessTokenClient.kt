package no.nav.sokos.pdl.proxy.pdl.security

import com.fasterxml.jackson.annotation.JsonAlias
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import java.time.Instant
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import no.nav.sokos.pdl.proxy.util.retry
import no.nav.sokos.pdl.proxy.config.PropertiesConfig.AzureAdClientConfig
import no.nav.sokos.pdl.proxy.config.logger

class AccessTokenClient(
    private val azureAdClientConfig: AzureAdClientConfig,
    private val client: HttpClient,
    private val aadAccessTokenUrl: String = "https://login.microsoftonline.com/${azureAdClientConfig.tenantId}/oauth2/v2.0/token"
) {
    private val mutex = Mutex()

    @Volatile
    private var token: AccessToken = runBlocking { AccessToken(hentAccessTokenFraProvider()) }
    suspend fun hentAccessToken(): String {
        val omToMinutter = Instant.now().plusSeconds(120L)
        return mutex.withLock {
            when {
                token.expiresAt.isBefore(omToMinutter) -> {
                    logger.info("Henter ny accesstoken")
                    token = AccessToken(hentAccessTokenFraProvider())
                    token.accessToken
                }

                else -> token.accessToken
            }
        }
    }

    private suspend fun hentAccessTokenFraProvider(): AzureAccessToken =
        retry {
            val response: HttpResponse = client.post(aadAccessTokenUrl) {
                accept(ContentType.Application.Json)
                method = HttpMethod.Post
                setBody(FormDataContent(Parameters.build {
                    append("tenant", azureAdClientConfig.tenantId)
                    append("client_id", azureAdClientConfig.clientId)
                    append("scope", "api://${azureAdClientConfig.pdlClientId}/.default")
                    append("client_secret", azureAdClientConfig.clientSecret)
                    append("grant_type", "client_credentials")
                }))
            }

            if (response.status != HttpStatusCode.OK) {
                val feilmelding =
                    "Kunne ikke hente accesstoken Azure. Statuskode: ${response.status}"
                logger.error { feilmelding }
                throw RuntimeException(feilmelding)
            } else {
                response.body()
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