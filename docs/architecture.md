# Diagramme d'architecture — Projet 5 : Restauration universitaire

Ce fichier peut être ouvert dans un artefact Markdown (rendu Mermaid automatique)
ou collé dans le rapport Word/LaTeX (les outils comme draw.io, Zotero+LaTeX ou
même une capture d'écran depuis un rendu Mermaid en ligne acceptent ce format).

## Vue d'ensemble Phase 2 (Docker Compose)

```mermaid
graph TB
    subgraph "Réseau Docker : restauration-net"
        CMD[commandes-service<br/>WildFly :8080]
        RESP[responsabilite-service<br/>WildFly :8080]
        PAY[paiement-service<br/>WildFly :8080]

        CMDB[(commandes-db<br/>MySQL)]
        RESPDB[(responsabilite-db<br/>MySQL)]
        PAYDB[(paiement-db<br/>MySQL)]

        CMD -->|JDBC| CMDB
        RESP -->|JDBC| RESPDB
        PAY -->|JDBC| PAYDB

        CMD -->|"GET /plats/{id}/disponibilite"| RESP
        CMD -->|"POST /paiements"| PAY
    end

    ETU[Étudiant / Client HTTP] -->|"POST /commandes<br/>POST /commandes/{id}/payer"| CMD
    ETU -->|"CRUD produits/plats/menus"| RESP
```

## Vue d'ensemble Phase 4 (Kubernetes / K3s)

```mermaid
graph TB
    subgraph "Namespace: restauration"
        ING[Ingress Traefik<br/>restauration.local]

        subgraph "Deployment: responsabilite-service"
            RESPPOD[Pod WildFly]
        end
        subgraph "Deployment: commandes-service"
            CMDPOD[Pod WildFly]
        end
        subgraph "Deployment: paiement-service"
            PAYPOD[Pod WildFly]
        end

        subgraph "Deployment: responsabilite-db"
            RESPDBPOD[Pod MySQL] --> RESPPVC[(PVC 1Gi)]
        end
        subgraph "Deployment: commandes-db"
            CMDDBPOD[Pod MySQL] --> CMDPVC[(PVC 1Gi)]
        end
        subgraph "Deployment: paiement-db"
            PAYDBPOD[Pod MySQL] --> PAYPVC[(PVC 1Gi)]
        end

        SEC[Secrets<br/>identifiants MySQL]
        CM[ConfigMaps<br/>MYSQL_DATABASE, app-config]

        SEC -.-> RESPDBPOD
        SEC -.-> CMDDBPOD
        SEC -.-> PAYDBPOD
        CM -.-> RESPDBPOD
        CM -.-> CMDDBPOD
        CM -.-> PAYDBPOD

        ING -->|"/responsabilite-service"| RESPPOD
        ING -->|"/commandes-service"| CMDPOD
        ING -->|"/paiement-service"| PAYPOD

        CMDPOD -->|REST interne| RESPPOD
        CMDPOD -->|REST interne| PAYPOD

        RESPPOD --> RESPDBPOD
        CMDPOD --> CMDDBPOD
        PAYPOD --> PAYDBPOD
    end

    USER[Utilisateur] --> ING
```

## Séquence du scénario métier principal

```mermaid
sequenceDiagram
    participant E as Étudiant
    participant C as commandes-service
    participant R as responsabilite-service
    participant P as paiement-service

    E->>C: POST /commandes (panier)
    loop pour chaque ligne du panier
        C->>R: GET /plats/{id}/disponibilite
        R-->>C: {nom, prix, disponible}
    end
    C->>C: calcule le montant total, persiste la commande
    C-->>E: 201 Created (commande)

    E->>C: POST /commandes/{id}/payer
    C->>P: POST /paiements {commandeId, montant, mode}
    P->>P: simule le traitement, génère une facture
    P-->>C: {statut: VALIDE}
    C->>C: marque la commande comme payée
    C-->>E: 200 OK (commande payée)
```
