# sokos-pdl-proxy

# Innholdsoversikt
* [1. Funksjonelle krav](#1-funksjonelle-krav)
* [2. Utviklingsmiljø](#2-utviklingsmiljø)
* [3. Programvarearkitektur](#3-programvarearkitektur)
* [4. Deployment](#4-deployment)
* [5. Autentisering](#5-autentisering)
* [6. Drift og støtte](#6-drift-og-støtte)
* [7. Swagger](#7-swagger)
* [8. Henvendelser](#7-henvendelser)
---

# 1. Funksjonelle Krav
Applikasjonen er et bindeledd mellom stormaskin og PDL (Persondataløsningen). Stormaskin har ikke mulighet til å gjøre GraphQL rest-kall men ved hjelp av denne proxy har vi mulighet til å hente et lite subset av persondata og identer fra PDL.

Tilbyr følgende API-er:
- Person identer, fornavn, mellomnavn, familie navn og kort navn.


# 2. Utviklingsmiljø
### Forutsetninger
* Java 17
* Gradle

### Bygge prosjekt
`./gradlew clean build`

### Lokal utvikling
Opprett en run configuration for Bootstrap.kt og angi properties nedenfor som environment variabler

```properties
NAIS_APP_NAME=sokos-pdl-proxy;
USE_AUTHENTICATION=false;
PDL_URL=http://0.0.0.0:9090/graphql;
LOG_APPENDER=CONSOLE
```

### PDL proxy
Start [mockPdlServer](src/test/kotlin/devtools/mockPdlServer.kt)

# 3. Programvarearkitektur
[System diagram](./dokumentasjon/system-diagram.md)

# 4. Deployment
Distribusjon av tjenesten er gjort med bruk av Github Actions.
[sokos-pdl-proxy CI / CD](https://github.com/navikt/sokos-pdl-proxy/actions)

Push/merge til master branche vil teste, bygge og deploye til produksjonsmiljø og testmiljø.
Det foreligger også mulighet for manuell deploy.

# 7. Autentisering
Applikasjonen bruker [AzureAD](https://docs.nais.io/security/auth/azure-ad/) autentisering

### Hente token
1. Installer `vault` kommandolinje verktøy
2. Gi rettighet for å kjøre scriptet `chmod 755 getToken.sh`
3. Kjør scriptet:
   ```
   ./getToken.sh
   ```
4. Skriv inn applikasjonsnavn du vil hente `client_id` og `client_secret` for

# 6. Drift og støtte

### Logging
Vi logger til logs.adeo.no.

For å se på logger må man logge seg på logs.adeo.no og velge NAV logs.

Feilmeldinger og infomeldinger som ikke innheholder sensitive data logges til indeksen `logstash-apps`, mens meldinger som inneholder sensitive data logges til indeksen `tjenestekall`.

- Filter for Produksjon
  * application:sokos-pdl-proxy AND envclass:p

- Filter for Dev
  * application:sokos-pdl-proxy AND envclass:q

[sikker-utvikling/logging](https://sikkerhet.nav.no/docs/sikker-utvikling/logging) - Anbefales å lese

### Kubectl
For dev-gcp:
```shell script
kubectl config use-context dev-gcp
kubectl get pods -n okonomi | grep sokos-ktor-template
kubectl logs -f sokos-ktor-template-<POD-ID> --namespace okonomi -c sokos-ktor-template
```

For prod-gcp:
```shell script
kubectl config use-context prod-gcp
kubectl get pods -n okonomi | grep sokos-ktor-template
kubectl logs -f sokos-ktor-template-<POD-ID> --namespace okonomi -c sokos-ktor-template
```

### Alarmer
Vi bruker [nais-alerts](https://doc.nais.io/observability/alerts) for å sette opp alarmer. Disse finner man konfigurert i [.nais/alerterator.yaml](.nais/alerterator.yaml) filen.

### Grafana
- [sokos-pdl-proxy](https://grafana.nais.io/d/ytprGMj7z/sokos-pdl-proxy?orgId=1&refresh=30s)
---

# 7. Swagger

- [Prod-gcp](https://sokos-pdl-proxy.intern.nav.no/person-proxy/api/v1/docs/#/)
- [Dev-gcp](https://sokos-pdl-proxy.dev.intern.nav.no/person-proxy/api/v1/docs/#/)
- [Lokalt](http://0.0.0.0:8080/person-proxy/api/v1/docs/)

# 8. Henvendelser
Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på Github.
Interne henvendelser kan sendes via Slack i kanalen `#po-utbetaling`