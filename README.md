# sokos-pdl-proxy

Applikasjonen er en bindeledd mellom stormaskin og PDL (Persondataløsningen).
Stormaskin har ikke mulighet til å gjøre GraphQL rest-kall men ved hjelp av denne proxy har vi mulighet 
til å hente et lite subset av persondata og identer.

## API-dokumentasjon
Tilbyr følgende API-er:
- Person identer, fornavn, mellomnavn, familie navn og kort navn.

## Oppsett av utviklermaskin
- JDK17
- Gradle

## Bygging
Fra kommandolinje
```
./gradlew build
```

## Lokal utvikling

### Properties
Opprett en run configuration for Bootstrap.kt og angi properties nedenfor som environment variabler

```properties
NAIS_APP_NAME=sokos-pdl-proxy;
USE_AUTHENTICATION=false;
PDL_URL=http://0.0.0.0:9090/graphql;
LOG_APPENDER=CONSOLE
```

### PDL proxy
Start [mockPdlServer](src/test/kotlin/devtools/mockPdlServer.kt)

# Logging

Applikasjonen logger til [logs.adeo.no](logs.adeo.no)

Filter for preproduksjon:

```
application:sokos-pdl-proxy AND envclass:q`
```

Filter for produksjon:
```
application:sokos-pdl-proxy AND envclass:p`
```

# Nyttig informasjon

## Scope
api://dev-gcp.okonomi.sokos-pdl-proxy/.default

## Swagger URL
https://sokos-pdl-proxy.dev.intern.nav.no/person-proxy/api/v1/docs/#/

