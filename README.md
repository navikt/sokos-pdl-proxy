***sokos-pdl-proxy***

## API-dokumentasjon
Vi tilbyr følgende API-er:
* NAV-API: Tilby en integrasjon for utbetalingsreskontro og oppdragz til å hente
* person identer, fornavn, mellomnavn, familie navn og kort navn.

## Oppsett av utviklermaskin
JDK16 må være installert.

## Bygging
Fra kommandolinje
```
./gradlew build
```

## Oppsett for IntelliJ
Under "Gradle properties" må "Gradle JVM" være satt til SDK 16.

## Lokal utvikling

### Properties
Opprett en run configuration for Bootstrap.kt og angi properties nedenfor som environment-variable


```properties
NAIS_APP_NAME=sokos-pdl-proxy;
USE_AUTHENTICATION=false;
PDL_URL=http://0.0.0.0:9090/graphql;
LOG_APPENDER=CONSOLE
```

###docker-compose


# Logging

Applikasjonen logger til logs.adeo.no

Filter for preproduksjon:

* application:sokos-pdl-proxy AND envclass:q

Filter for produksjon:

* application:sokos-pdl-proxy AND envclass:p

# Nyttig informasjon

## Scope
api://dev-gcp.okonomi.sokos-pdl-proxy/.default

## Swagger URL
https://sokos-pdl-proxy.dev.intern.nav.no/person-proxy/api/v1/docs/#/

