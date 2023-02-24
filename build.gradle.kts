import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLGenerateClientTask
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    kotlin("jvm") version "1.8.10"
    kotlin("plugin.serialization") version "1.8.10"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("org.openapi.generator") version "6.4.0"
    id("com.expediagroup.graphql") version "6.3.0"
    id("org.betterplugin.avro") version "0.19.2-SNAPSHOT"

    application
}

group = "no.nav.sokos"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
}

val ktorVersion = "2.2.3"
val logbackVersion = "1.4.5"
val logstashVersion = "7.3"
val jacksonVersion = "2.14.1"
val prometheusVersion = "1.10.2"
val kotlinLoggingVersion = "3.0.5"
val junitVersion = "5.9.1"
val mockkVersion = "1.13.3"
val graphqlClientVersion = "6.4.0"
val avroVersion = "1.11.1"
val restAssuredVersion = "5.3.0"
val swaggerRequestValidatorVersion = "2.33.1"
val assertJvmVersion = "0.25"


dependencies {

    // Ktor server
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-call-id-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-swagger:$ktorVersion")

    // Ktor client
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-apache-jvm:$ktorVersion")

    implementation("io.ktor:ktor-serialization-jackson-jvm:$ktorVersion")

    // Security
    implementation("io.ktor:ktor-server-auth-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:$ktorVersion")

    // Jackson
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    // Monitorering
    implementation("io.ktor:ktor-server-metrics-micrometer-jvm:$ktorVersion")
    implementation("io.micrometer:micrometer-registry-prometheus:$prometheusVersion")

    // Logging
    implementation("io.github.microutils:kotlin-logging-jvm:$kotlinLoggingVersion")
    runtimeOnly("ch.qos.logback:logback-classic:$logbackVersion")
    runtimeOnly("net.logstash.logback:logstash-logback-encoder:$logstashVersion")

    // Test
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("io.ktor:ktor-client-tests:$ktorVersion")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:$assertJvmVersion")
    testImplementation("io.rest-assured:rest-assured:$restAssuredVersion")
    testImplementation("com.atlassian.oai:swagger-request-validator-restassured:$swaggerRequestValidatorVersion")

    // GraphQL
    implementation("com.expediagroup:graphql-kotlin-ktor-client:$graphqlClientVersion") {
        exclude("com.expediagroup", "graphql-kotlin-client-serialization")
    }
    runtimeOnly("com.expediagroup:graphql-kotlin-client-jackson:$graphqlClientVersion")

    implementation("org.apache.avro:avro:$avroVersion")

}

application {
    mainClass.set("no.nav.sokos.pdl.proxy.ApplicationKt")
}

sourceSets {
    main {
        java {
            srcDirs("$buildDir/generated/src/main/kotlin")
        }
    }
}

tasks {

    withType<KotlinCompile>().configureEach {
        dependsOn("openApiGenerate")
        dependsOn("graphqlGenerateClient")

        compilerOptions.jvmTarget.set(JVM_17)
    }

    withType<GenerateTask>().configureEach {
        generatorName.set("kotlin")
        generateModelDocumentation.set(false)
        inputSpec.set("$rootDir/src/main/resources/openapi/sokos-pdl-proxy-v1-swagger2.json")
        outputDir.set("$buildDir/resources/main/api")
        globalProperties.set(
            mapOf(
                "models" to ""
            )
        )
        configOptions.set(
            mapOf(
                "library" to "jvm-ktor",
                "serializationLibrary" to "jackson"
            )
        )
    }

    withType<ShadowJar>().configureEach {
        enabled = true
        archiveFileName.set("app.jar")
        manifest {
            attributes["Main-Class"] = "no.nav.sokos.pdl.proxy.ApplicationKt"
        }
    }

    ("jar") {
        enabled = false
    }

    withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging {
            exceptionFormat = FULL
            events("passed", "skipped", "failed")
        }


        // For å øke hastigheten på build kan vi benytte disse metodene
        maxParallelForks = Runtime.getRuntime().availableProcessors() / 2
        reports.forEach { report -> report.required.value(false) }
    }

    withType<GraphQLGenerateClientTask>().configureEach {
        packageName.set("no.nav.pdl")
        schemaFile.set(file("$projectDir/src/main/resources/graphql/schema.graphql"))
        queryFileDirectory.set(file("$projectDir/src/main/resources/graphql"))
    }
}
