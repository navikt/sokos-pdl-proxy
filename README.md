# sokos-pdl-proxy

Applikasjonen er et bindeledd mellom stormaskin og PDL (Persondataløsningen).
Stormaskin har ikke mulighet til å gjøre GraphQL rest-kall men ved hjelp av denne proxy har vi mulighet 
til å hente et lite subset av persondata og identer.

## API-dokumentasjon
Tilbyr følgende API-er:
* Person identer, fornavn, mellomnavn, familie navn og kort navn.

---

## Oppsett av utviklermaskin
* JDK17
* Gradle

---

## Bygging
Fra kommandolinje
```
./gradlew clean build
```

---

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

---

# Logging

Vi logger til logs.adeo.no.

For å se på logger må man logge seg på logs.adeo.no og velge NAV logs.

Feilmeldinger og infomeldinger som ikke innheholder sensitive data logges til indeksen `logstash-apps`, mens meldinger som inneholder sensitive data logges til indeksen `tjenestekall`.

### Filter for Produksjon

* application:sokos-pdl-proxy AND envclass:p

### Filter for Dev

* application:sokos-pdl-proxy AND envclass:q

---

# Nyttig informasjon


## Hvordan skaffe token i preprod

`curl` kommando for å hente JWT-token:
```
curl -X POST -H "Content-Type: application/x-www-form-urlencoded" -d "client_id={{AZURE_CLIENT_ID_UR}}&scope=api://{{AZURE_APP_CLIENT_ID}}/.default&client_secret={{AZURE_CLIENT_SECRET_UR}}&grant_type=client_credentials" "https://login.microsoftonline.com/$AZURE_APP_TENANT_ID/oauth2/v2.0/token"
```

Vi trenger følgende:
* `AZURE_CLIENT_ID-<konsument-system>` -> Finner i vault under `secrets/azuread/show/dev/creds/<system>` 
* `AZURE_CLIENT_SECRET-<kosument-system>` -> Finner i vault under `secrets/azuread/show/dev/creds/<system>`
* `AZURE_APP_CLIENT_ID` -> Hente fra pod
* `AZURE_APP_TENANT_ID` -> Hente fra pod

### Slik kan man logge seg på pod med bash
```
kubectl config use-context dev-gcp
POD=$(kubectl get pods -nokonomi | grep sokos-pdl-proxy | grep Running | awk '{ print $1; }' | sed -n 1p )
kubectl -nokonomi exec --stdin --tty $POD --container sokos-pdl-proxy  -- /bin/bash
```

## Swagger URL

- [Prod-gcp](https://sokos-pdl-proxy.intern.nav.no/person-proxy/api/v1/docs/#/)
- [Dev-gcp](https://sokos-pdl-proxy.dev.intern.nav.no/person-proxy/api/v1/docs/#/)
- [Lokalt](http://0.0.0.0:8080/person-proxy/api/v1/docs/)
