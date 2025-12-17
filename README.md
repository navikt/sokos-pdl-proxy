# sokos-pdl-proxy

* [1. Dokumentasjon](dokumentasjon/dokumentasjon.md)
* [2. Funksjonelle krav](#2-funksjonelle-krav)
* [3. Utviklingsmiljø](#3-utviklingsmiljø)
* [4. Programvarearkitektur](#4-programvarearkitektur)
* [5. Deployment](#5-deployment)
* [6. Autentisering](#6-autentisering)
* [7. Drift og støtte](#7-drift-og-støtte)
* [8. Swagger](#8-swagger)
* [9. Henvendelser](#9-henvendelser)

---

# 2. Funksjonelle Krav

Applikasjonen er et bindeledd mellom stormaskin og PDL (Persondataløsningen). Stormaskin har ikke mulighet til å gjøre
GraphQL rest-kall men ved hjelp av denne proxy har vi mulighet til å hente et lite subset av persondata og identer fra
PDL.

API tilbyr følgende:

- Person identer, fornavn, mellomnavn, etternavn, forkortet navn, bostedsadresse, martikkeladresse, utenlandskadresse,
  ukjent bosted, metdata, kontaktadresse, oppholdsadresse

# 3. Utviklingsmiljø

### Forutsetninger

* Java 25
* [Gradle](https://gradle.org/)
* [Kotest IntelliJ Plugin](https://plugins.jetbrains.com/plugin/14080-kotest)

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

# 4. Programvarearkitektur

```mermaid
flowchart TB

subgraph k1 [Stormaskin]
  direction TB
  OS     
  UR     
end


k1 --> |REST| SPP
SPP[sokos-pdl-proxy] --> |GraphQL| PDL
```

# 5. Deployment

Distribusjon av tjenesten er gjort med bruk av Github Actions.
[sokos-pdl-proxy CI / CD](https://github.com/navikt/sokos-pdl-proxy/actions)

Push/merge til main branch vil teste, bygge og deploye til produksjonsmiljø og testmiljø.
Det foreligger også mulighet for manuell deploy.

# 6. Autentisering

Applikasjonen bruker [AzureAD](https://docs.nais.io/security/auth/azure-ad/) autentisering

# 7. Drift og støtte

### Logging

Feilmeldinger og infomeldinger som ikke innheholder sensitive data logges til [Grafana Loki](https://docs.nais.io/observability/logging/#grafana-loki).  
Sensitive meldinger logges til [Team Logs](https://doc.nais.io/observability/logging/how-to/team-logs/).

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

# 8. Swagger

- [Prod-gcp](https://sokos-pdl-proxy.intern.nav.no/api/pdl-proxy/v1/docs)
- [Dev-gcp](https://sokos-pdl-proxy.intern.dev.nav.no/api/pdl-proxy/v1/docs)
- [Lokalt](http://0.0.0.0:8080/api/pdl-proxy/v1/docs)

# 9. Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på Github.
Interne henvendelser kan sendes via Slack i kanalen [#utbetaling](https://nav-it.slack.com/archives/CKZADNFBP)