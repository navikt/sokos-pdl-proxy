# sokos-pdl-proxy

Applikasjonen er et bindeledd mellom stormaskin og PDL (Persondataløsningen).
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
./gradlew clean build
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
application:sokos-pdl-proxy AND envclass:q
```

Filter for produksjon:
```
application:sokos-pdl-proxy AND envclass:p
```

# Nyttig informasjon


## Hent av token for PDL
Vi trenger følgende:
### Tenant-Id
Her i NAV er tenantId betyr hvilket miljø område skal vi hente token til - Dev(domain er  trygdeetaten.no) eller Prod(domain er nav.no).
Kan finnes mer detaljer på https://confluence.adeo.no/display/~G156196/Azure#Azure-Prod
### URL
https://login.microsoftonline.com/<miljø-tenant-id: dev eller prod>/oauth2/v2.0/token
### grant_type
client_credentials
### client_id
finnes på https://vault.adeo.no
### client_secret
finnes på https://vault.adeo.no 
### Content-Type
application/x-www-form-urlencoded
### Scope (PDL er ikke på GCP så brukes dev-fss)
api://dev-fss.pdl.pdl-api/.default

## Slik kan man logge seg på pod med bash
```
kubectl config use-context dev-gcp
POD=$(kubectl get pods -nokonomi | grep sokos-pdl-proxy | grep Running | awk '{ print $1; }' | sed -n 1p )
kubectl -nokonomi exec --stdin --tty $POD --container sokos-pdl-proxy  -- /bin/bash
```

## Swagger URL

- [Dev-gcp miljø](https://sokos-pdl-proxy.dev.intern.nav.no/person-proxy/api/v1/docs/#/)
- [Lokalt miljø](http://0.0.0.0:8080/person-proxy/api/v1/docs/)
