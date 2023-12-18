package no.nav.sokos.pdl.proxy.config

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import java.io.File
object PropertiesConfig {

    private val defaultProperties = ConfigurationMap(
        mapOf(
            "NAIS_APP_NAME" to "sokos-pdl-proxy",
            "NAIS_NAMESPACE" to "okonomi",
        )
    )
    private val localDevProperties = ConfigurationMap(
        "USE_AUTHENTICATION" to "true",
        "APPLICATION_PROFILE" to Profile.LOCAL.toString(),
    )
    private val devProperties = ConfigurationMap(mapOf("APPLICATION_PROFILE" to Profile.DEV.toString()))
    private val prodProperties = ConfigurationMap(mapOf("APPLICATION_PROFILE" to Profile.PROD.toString()))

    private val config = when (System.getenv("NAIS_CLUSTER_NAME") ?: System.getProperty("NAIS_CLUSTER_NAME")) {
        "dev-gcp" -> ConfigurationProperties.systemProperties() overriding EnvironmentVariables() overriding devProperties overriding defaultProperties
        "prod-gcp" -> ConfigurationProperties.systemProperties() overriding EnvironmentVariables() overriding prodProperties overriding defaultProperties
        else ->
            ConfigurationProperties.systemProperties() overriding EnvironmentVariables() overriding ConfigurationProperties.fromOptionalFile(
                File("defaults.properties")
            ) overriding localDevProperties overriding defaultProperties
    }

    private operator fun get(key: String): String = config[Key(key, stringType)]

    data class Configuration(
        val naisAppName: String = get("NAIS_APP_NAME"),
        val profile: Profile = Profile.valueOf(this["APPLICATION_PROFILE"]),
        val useAuthentication: Boolean = get("USE_AUTHENTICATION").toBoolean(),
        val azureAdClientConfig: AzureAdClientConfig = AzureAdClientConfig(),
        val azureAdServerConfig: AzureAdServerConfig = AzureAdServerConfig(),
        val pdlConfig: PdlConfig = PdlConfig()
    )

    data class AzureAdClientConfig(
        val clientId: String = get("AZURE_APP_CLIENT_ID"),
        val wellKnownUrl: String = get("AZURE_APP_WELL_KNOWN_URL"),
        val tenantId: String = get("AZURE_APP_TENANT_ID"),
        val clientSecret: String = get("AZURE_APP_CLIENT_SECRET"),
        val pdlClientId: String = get("PDL_CLIENT_ID")
    )

    data class AzureAdServerConfig(
        val clientId: String = get("AZURE_APP_CLIENT_ID"),
        val wellKnownUrl: String = get("AZURE_APP_WELL_KNOWN_URL"),
    )

    data class PdlConfig(
        val pdlUrl: String = this["PDL_URL"]
    )

    enum class Profile {
        LOCAL, DEV, PROD
    }

}
