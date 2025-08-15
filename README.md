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

* Java 21
* Gradle

### Bygge prosjekt

`./gradlew clean build shadowJar`

### Lokal utvikling

NB! Du må ha [naisdevice](https://docs.nais.io/device/) kjørende på maskinen.

For å kjøre applikasjonen må du gjøre følgende:

- Kjør scriptet [setupLocalEnvironment.sh](setupLocalEnvironment.sh)
     ```
     chmod 755 setupLocalEnvironment.sh && ./setupLocalEnvironment.sh
     ```
  Denne vil opprette [default.properties](defaults.properties) med alle environment variabler du trenger for å kjøre
  applikasjonen som er definert
  i [PropertiesConfig](src/main/kotlin/no/nav/sokos/pdl/proxy/config/PropertiesConfig.kt).
  Her vil du også kunne f.eks endre om du ønsker slå på autentisering eller ikke i
  koden `"USE_AUTHENTICATION" to "true"` i
  filen [PropertiesConfig](src/main/kotlin/no/nav/sokos/pdl/proxy/config/PropertiesConfig.kt).

# 3. Programvarearkitektur

[System diagram](./dokumentasjon/system-diagram.md)

# 4. Deployment

Distribusjon av tjenesten er gjort med bruk av Github Actions.
[sokos-pdl-proxy CI / CD](https://github.com/navikt/sokos-pdl-proxy/actions)

Push/merge til main branch vil teste, bygge og deploye til produksjonsmiljø og testmiljø.
Det foreligger også mulighet for manuell deploy.

# 5. Autentisering

Applikasjonen bruker [AzureAD](https://docs.nais.io/security/auth/azure-ad/) autentisering

# 6. Drift og støtte

### Logging

https://logs.adeo.no.

Feilmeldinger og infomeldinger som ikke innheholder sensitive data logges til data view `Applikasjonslogger`.  
Sensetive meldinger logges til data view `Securelogs` [sikker-utvikling/logging](https://sikkerhet.nav.no/docs/sikker-utvikling/logging)).

- Filter for Produksjon
    * application:sokos-spk-mottak AND envclass:p

- Filter for Dev
    * application:sokos-spk-mottak AND envclass:q

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

Applikasjonen bruker [Grafana Alerting](https://grafana.nav.cloud.nais.io/alerting/) for overvåkning og varsling.
Dette er konfigurert via NAIS sin [alerting-integrasjon](https://doc.nais.io/observability/alerts).

Alarmene overvåker metrics som:

- HTTP-feilrater
- JVM-metrikker

Varsler blir sendt til følgende Slack-kanaler:

- Dev-miljø: [#team-mob-alerts-dev](https://nav-it.slack.com/archives/C042SF2FEQM)
- Prod-miljø: [#team-mob-alerts-prod](https://nav-it.slack.com/archives/C042ESY71GX)

### Grafana

- [sokos-pdl-proxy](https://grafana.nais.io/d/ytprGMj7z/sokos-pdl-proxy?orgId=1&refresh=30s)

---

# 7. Swagger

- [Prod-gcp](https://sokos-pdl-proxy.intern.nav.no/api/pdl-proxy/v1/docs)
- [Dev-gcp](https://sokos-pdl-proxy.intern.dev.nav.no/api/pdl-proxy/v1/docs)
- [Lokalt](http://0.0.0.0:8080/api/pdl-proxy/v1/docs)

# 8. Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på Github.
Interne henvendelser kan sendes via Slack i kanalen [#utbetaling](https://nav-it.slack.com/archives/CKZADNFBP)