# sokos-pdl-proxy

# Innholdsoversikt

* [1. Funksjonelle krav](#1-funksjonelle-krav)
* [2. Utviklingsmiljø](#2-utviklingsmiljø)
* [3. Programvarearkitektur](#3-programvarearkitektur)
* [4. Deployment](#4-deployment)
* [5. Autentisering](#5-autentisering)
* [6. Drift og støtte](#6-drift-og-støtte)
* [7. Swagger](#7-swagger)
* [8. Henvendelser](#8-henvendelser)
---

# 1. Funksjonelle Krav

Applikasjonen er et bindeledd mellom stormaskin og PDL (Persondataløsningen). Stormaskin har ikke mulighet til å gjøre
GraphQL rest-kall men ved hjelp av denne proxy har vi mulighet til å hente et lite subset av persondata og identer fra
PDL.

API tilbyr følgende:

- Person identer, fornavn, mellomnavn, etternavn, forkortet navn, bostedsadresse, martikkeladresse, utenlandskadresse,
  ukjent bosted, metdata, kontaktadresse, oppholdsadresse

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

# 5. Autentisering

Applikasjonen bruker [AzureAD](https://docs.nais.io/security/auth/azure-ad/) autentisering

### Hente token

1. Installer `vault` kommandolinje verktøy: https://github.com/navikt/utvikling/blob/main/docs/teknisk/Vault.md
2. Installer `jq` kommandolinje verktøy: https://github.com/stedolan/jq
3. Gi rettighet for å kjøre scriptet `chmod 755 getToken.sh`
4. Kjør scriptet [getToken.sh](getToken.sh)
      ```
      chmod 755 getToken.sh && ./getToken.sh
      ```
4. Skriv inn applikasjonsnavn du vil hente `client_id` og `client_secret` for

# 6. Drift og støtte

### Logging

Vi logger til logs.adeo.no.

For å se på logger må man logge seg på logs.adeo.no og velge NAV logs.

Feilmeldinger og infomeldinger som ikke innheholder sensitive data logges til indeksen `logstash-apps`, mens meldinger
som inneholder sensitive data logges til indeksen `tjenestekall`.

- Filter for Produksjon
    * application:sokos-pdl-proxy AND envclass:p

- Filter for Dev
    * application:sokos-pdl-proxy AND envclass:q

[sikker-utvikling/logging](https://sikkerhet.nav.no/docs/sikker-utvikling/logging) - Anbefales å lese
- Filter for sikkerhet logs på https://logs.adeo.no
    * Bytte Change index pattern fra: logstash-* til: tjenestekall-*
    * Bruk samme filter for dev og prod som er vist over

### Kubectl

For dev-gcp:

```shell script
kubectl config use-context dev-gcp
kubectl get pods -n okonomi | grep sokos-pdl-proxy
kubectl logs -f sokos-pdl-proxy-<POD-ID> --namespace okonomi -c sokos-pdl-proxy
```

For prod-gcp:

```shell script
kubectl config use-context prod-gcp
kubectl get pods -n okonomi | grep sokos-pdl-proxy
kubectl logs -f sokos-pdl-proxy-<POD-ID> --namespace okonomi -c sokos-pdl-proxy
```

### Alarmer

Vi bruker [nais-alerts](https://doc.nais.io/observability/alerts) for å sette opp alarmer. Disse finner man konfigurert
i [.nais/alerterator.yaml](.nais/alerterator.yaml) filen.

### Grafana

- [sokos-pdl-proxy](https://grafana.nais.io/d/ytprGMj7z/sokos-pdl-proxy?orgId=1&refresh=30s)

---

# 7. Swagger

- [Prod-gcp](https://sokos-pdl-proxy.intern.nav.no/pdl-proxy/api/pdl-proxy/v1/docs)
- [Dev-gcp](https://sokos-pdl-proxy.dev.intern.nav.no/api/pdl-proxy/v1/docs)
- [Lokalt](http://0.0.0.0:8080/api/pdl-proxy/v1/docs)

# 8. Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på Github.
Interne henvendelser kan sendes via Slack i kanalen `#po-utbetaling`