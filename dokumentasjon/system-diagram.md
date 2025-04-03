# System-Arkitetktur

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