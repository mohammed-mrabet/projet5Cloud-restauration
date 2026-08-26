# Projet 5 — Restauration universitaire
Module Cloud Computing — Master M2I — FS Oujda

Ce dépôt contient les livrables des 4 phases du mini-projet pour le **Projet 5**,
qui fusionne 3 microservices :

| Service | Rôle |
|---|---|
| **Responsabilité** | Catalogue : produits, plats, menus, régimes spéciaux, planification par restaurant/jour |
| **Commandes** | Paniers étudiants (sur place / à emporter / réservation), suivi de statut |
| **Paiement** | Traitement des paiements et facturation |

Chaque service suit l'architecture imposée : 1 conteneur applicatif **Jakarta EE
(WildFly)** exposant des API REST + 1 conteneur **MySQL**.

---

## Architecture générale

```
                 ┌──────────────────────┐
                 │   commandes-service   │
                 │   (WildFly + REST)    │
                 └──────┬─────────┬──────┘
        appel REST GET  │         │  appel REST POST
        /plats/{id}/    │         │  /paiements
        disponibilite   ▼         ▼
   ┌──────────────────────┐   ┌──────────────────┐
   │ responsabilite-service│   │ paiement-service │
   └──────────┬───────────┘   └─────────┬─────────┘
              │                          │
              ▼                          ▼
     responsabilite-db            paiement-db
        (MySQL)                     (MySQL)

   commandes-service possède aussi sa propre base : commandes-db
```

**Principe clé (microservices) :** chaque service a SA base de données ; aucune
clé étrangère ne traverse les bases. Le lien entre `Commande` et `Plat` (ou
`Paiement`) se fait uniquement par un identifiant numérique + un appel REST
au moment voulu. C'est exactement l'"enjeu Docker" demandé par le sujet.

---

## Phase 1 — Services individuels

**Objectif :** chaque service doit fonctionner seul (Dockerfile multi-stage +
compose.yaml validant Application ↔ Base de données).

Structure de chaque service (identique pour les 3) :
```
xxx-service/
├── pom.xml                                  # build Maven → produit un .war
├── src/main/java/.../entity/                # entités JPA (tables MySQL)
├── src/main/java/.../dto/                   # objets d'échange JSON (API)
├── src/main/java/.../resource/              # endpoints REST (JAX-RS)
├── src/main/java/.../util/JPAUtil.java       # accès à l'EntityManager
├── src/main/java/.../config/ApplicationConfig.java  # active JAX-RS sur /api
├── src/main/resources/META-INF/persistence.xml      # config JPA/Hibernate + MySQL
├── src/main/webapp/WEB-INF/beans.xml        # active CDI (obligatoire, même vide)
├── Dockerfile                               # build multi-stage
└── compose.yaml                      # service applicatif + MySQL
```

### Comment ça marche

- **`pom.xml`** : packaging `war`. L'API Jakarta EE est en scope `provided`
  (déjà présente dans WildFly) ; le driver `mysql-connector-j`, lui, est
  embarqué dans le `.war` (`WEB-INF/lib`) pour éviter d'avoir à configurer un
  datasource JNDI côté serveur — plus simple à containeriser.
- **`persistence.xml`** : unité de persistance `RESOURCE_LOCAL`. L'URL JDBC
  pointe vers le nom du conteneur MySQL (ex: `responsabilite-db`), résolu par
  le DNS interne du réseau Docker.
- **Les `Resource` (JAX-RS)** : un `EntityManager` est ouvert/fermé à chaque
  requête HTTP via `JPAUtil`. Chaque méthode REST gère sa propre transaction
  (`em.getTransaction().begin()/commit()`).
- **`Dockerfile` multi-stage** :
  1. **Étape `build`** : image `maven:3.9-eclipse-temurin-17`, compile le
     projet (`mvn clean package`) → produit `target/xxx-service.war`.
  2. **Étape finale** : image officielle `quay.io/wildfly/wildfly`, on copie
     uniquement le `.war` dans `standalone/deployments/`. L'image finale ne
     contient ni Maven ni les sources : elle est petite et propre.
- **`compose.yaml`** : MySQL + le service applicatif, avec un
  `healthcheck` (`mysqladmin ping`) et `depends_on: condition: service_healthy`
  pour que WildFly attende que MySQL soit réellement prêt (pas juste démarré).

### Tester un service isolément
```bash
cd responsabilite-service
docker compose up --build
curl http://localhost:8081/responsabilite-service/api/plats
```
(Pareil pour `commandes-service` sur le port 8082 et `paiement-service` sur 8083 —
mais en isolation, les appels inter-services de `commandes-service` échoueront,
c'est normal : il leur manque le réseau commun de la Phase 2.)

---

## Phase 2 — Regroupement (Docker Compose `include`)

**Objectif :** faire communiquer les 3 services via des appels REST internes,
sur un réseau Docker commun, avec un fichier `compose.yaml` maître utilisant
la directive `include`.

Le fichier `compose.yaml` à la racine fait simplement :
```yaml
include:
  - responsabilite-service/compose.yaml
  - commandes-service/compose.yaml
  - paiement-service/compose.yaml
```
`include` (Compose v2.20+) fusionne le modèle de plusieurs fichiers comme le
ferait `docker compose -f a.yaml -f b.yaml -f c.yaml`, mais en gardant chaque
fichier autonome et réutilisable (Phase 1). Tous les services partagent le
réseau `restauration-net`.

### Les 2 appels REST internes (le vrai "enjeu Docker" du Projet 5)

1. **`commandes-service` → `responsabilite-service`** (`ResponsabiliteClient.java`)
   À la création d'une commande (`POST /commandes`), pour chaque ligne du
   panier on appelle :
   ```
   GET http://responsabilite-service:8080/responsabilite-service/api/plats/{id}/disponibilite
   ```
   Si le plat n'existe pas ou est indisponible → `409 Conflict`, sinon le
   prix renvoyé sert à calculer le montant total (jamais de prix envoyé
   par le client lui-même : on ne fait jamais confiance à l'entrée externe).

2. **`commandes-service` → `paiement-service`** (`PaiementClient.java`)
   `POST /commandes/{id}/payer` relaie la demande vers :
   ```
   POST http://paiement-service:8080/paiement-service/api/paiements
   ```
   qui simule le traitement et génère automatiquement une facture.

Notez que `http://responsabilite-service:8080` utilise le **nom du service**
Compose, pas `localhost` ni le port publié (8081) : c'est la communication
interne au réseau Docker, indépendante des ports exposés à l'hôte.

### Tester le scénario complet
```bash
docker compose up --build
curl -X POST http://localhost:8082/commandes-service/api/commandes \
  -H "Content-Type: application/json" \
  -d '{"etudiantId":1,"type":"SUR_PLACE","lignes":[{"platId":1,"quantite":2}]}'
```
(Pensez à créer au moins un `Plat` via `POST /plats` sur le port 8081 avant ce test.)

### Script de test automatisé

Un script `test-scenario.sh` (racine du projet) exécute l'intégralité du
scénario métier en une seule commande : création d'un régime spécial et de
2 plats, vérification de disponibilité, création d'une commande à 2 lignes,
test du garde-fou sur un plat inexistant (doit renvoyer `409`), paiement,
puis vérification de la facture générée. Utile pour la démo en présentation
et pour les captures d'écran du rapport.
```bash
docker compose up --build -d
chmod +x test-scenario.sh
./test-scenario.sh
```

---

## Phase 3 — Infrastructure as Code (Vagrant)

**Objectif :** scripter la création de l'infrastructure de production avec un
`Vagrantfile`.

Le `Vagrantfile` fourni crée **2 machines virtuelles** Ubuntu 22.04 :
- `k3s-server` (192.168.56.10) : nœud maître du futur cluster K3s
- `k3s-agent` (192.168.56.11) : nœud worker

Comme l'installation de K3s lui-même est scriptée directement dans les
provisioners shell du Vagrantfile, les Phases 3 et 4 s'enchaînent en une
seule commande : `vagrant up` crée les VMs **et** installe le cluster K3s.
Les manifestes Kubernetes (dossier `k8s/`) restent eux appliqués manuellement
avec `kubectl`, ce qui correspond bien à la séparation logique du sujet
("une fois l'infrastructure provisionnée, vous déploierez...").

Point technique à retenir : le dossier du projet est automatiquement monté
dans chaque VM sous `/vagrant`. On s'en sert pour transmettre le `node-token`
généré par le serveur K3s à l'agent (qui en a besoin pour rejoindre le
cluster), sans configuration réseau supplémentaire.

### Utilisation
```bash
vagrant up                 # crée et provisionne les 2 VMs (~5-10 min)
vagrant ssh k3s-server      # se connecter au nœud maître
vagrant ssh k3s-server -c "sudo k3s kubectl get nodes"   # vérifier le cluster
```
Le fichier `k3s.yaml` (kubeconfig) est copié à la racine du projet : vous
pouvez piloter le cluster depuis votre machine hôte avec
`export KUBECONFIG=./k3s.yaml && kubectl get nodes`, à condition d'avoir
`kubectl` installé localement.

---

## Phase 4 — Orchestration Kubernetes/K3s

**Objectif :** migrer le Pôle vers K3s avec Secrets, ConfigMaps, persistance
et Ingress.

Dossier `k8s/`, fichiers numérotés dans l'ordre d'application :

| Fichier | Contenu |
|---|---|
| `00-namespace.yaml` | Namespace `restauration` (isole le projet dans le cluster) |
| `01-secrets.yaml` | Identifiants MySQL des 3 bases (**uniquement** ça, comme demandé) |
| `02-configmaps.yaml` | Nom des bases (non sensible) + config applicative commune |
| `03-responsabilite-db.yaml` | PVC + Deployment + Service MySQL |
| `04-responsabilite-app.yaml` | Deployment + Service WildFly |
| `05/06-commandes-*.yaml` | Idem pour Commandes |
| `07/08-paiement-*.yaml` | Idem pour Paiement |
| `09-ingress.yaml` | Routage HTTP par chemin vers les 3 services |
| `build-and-import.sh` | Exporte + importe automatiquement les images sur les 2 nœuds K3s (voir plus bas) |

Chaque Deployment applicatif et MySQL déclare des `resources.requests/limits`
(CPU/mémoire) et des sondes `readinessProbe` **+** `livenessProbe` — bonnes
pratiques attendues en production, même sur un mini-projet pédagogique.

### Secrets vs ConfigMaps
- **Secret** = identifiants MySQL (`MYSQL_ROOT_PASSWORD`, `MYSQL_USER`,
  `MYSQL_PASSWORD`). Encodés en base64 automatiquement par Kubernetes.
- **ConfigMap** = tout ce qui n'est pas sensible : nom de la base
  (`MYSQL_DATABASE`), variables applicatives (`APP_ENV`, `TZ`, `LOG_LEVEL`).

Les deux sont injectés dans les Pods MySQL via `envFrom: [secretRef, configMapRef]`.

### Persistance
Chaque base MySQL a son propre `PersistentVolumeClaim` (1Gi, `ReadWriteOnce`).
K3s fournit par défaut un `StorageClass` nommé `local-path` (provisioning
dynamique automatique) : **aucun `PersistentVolume` à créer manuellement**.
Les données survivent donc à un redémarrage du Pod MySQL.

### Ingress (Traefik)
K3s embarque **Traefik** comme Ingress Controller par défaut — rien à
installer. Le routage se fait par chemin :

| Chemin | Service |
|---|---|
| `/responsabilite-service/...` | `responsabilite-service` |
| `/commandes-service/...` | `commandes-service` |
| `/paiement-service/...` | `paiement-service` |

### Déployer

1. **Construire les images Docker** (depuis la racine du projet, sur l'hôte) :
   ```bash
   docker compose build
   ```
2. **Exporter puis importer les images dans les 2 nœuds K3s** (les VMs n'ont
   pas accès à un registre externe type Docker Hub) : le script suivant
   exporte chaque image en `.tar` puis l'importe automatiquement dans
   containerd sur `k3s-server` **et** `k3s-agent` via `vagrant ssh -c` — les
   Pods pouvant être ordonnancés sur l'un ou l'autre nœud, les deux doivent
   connaître les images (voir `imagePullPolicy: Never` dans les manifestes,
   qui interdit volontairement tout repli vers un registre externe) :
   ```bash
   chmod +x k8s/build-and-import.sh
   ./k8s/build-and-import.sh
   ```
3. **Appliquer les manifestes** :
   ```bash
   export KUBECONFIG=./k3s.yaml
   kubectl apply -f k8s/
   kubectl get pods -n restauration -w
   ```
4. **Tester via l'Ingress** (ajoutez `192.168.56.10 restauration.local` à votre
   fichier `/etc/hosts`, ou utilisez l'option `-H` de curl) :
   ```bash
   curl -H "Host: restauration.local" http://192.168.56.10/responsabilite-service/api/plats
   ```

> **Alternative plus simple pour un seul nœud :** si vous n'utilisez qu'une
> seule VM (pas de `k3s-agent`), supprimez ce bloc du Vagrantfile et adaptez
> le tableau `NODES` dans `k8s/build-and-import.sh` pour ne garder que
> `k3s-server`.

---

## Limites connues / pistes d'amélioration pour le rapport

- Le paiement est **simulé** (toujours validé) : à mentionner explicitement
  dans le rapport comme une simplification pédagogique.
- Les URLs des services internes sont actuellement codées en dur dans le
  Java (`ResponsabiliteClient`, `PaiementClient`, `persistence.xml`). Elles
  fonctionnent à l'identique sous Docker Compose et sous Kubernetes car les
  noms de Service K8s ont été choisis identiques aux noms de conteneurs
  Compose — mais une vraie configuration externalisée (lue depuis le
  ConfigMap `app-config` via `System.getenv()`) serait une amélioration à
  proposer dans la partie "perspectives" du rapport.
- Le scaling (plusieurs réplicas) n'a pas de sens pour MySQL tel que
  configuré ici (un seul Pod, `ReadWriteOnce`) ; en revanche les services
  applicatifs (`responsabilite-service`, etc.) peuvent être passés à
  `replicas: 2` ou plus sans modification.

---

## Pour le rapport (rappel des consignes)

- Rapport Word/LaTeX **et** PDF, à envoyer avant le **5 juillet**.
- Utilisation de **Zotero** obligatoire pour les références bibliographiques.
- Codes bien commentés (fait : commentaires en français expliquant le rôle
  de chaque fichier/méthode clé, en particulier les 2 appels REST internes).
- Présentation de 15 minutes en trinôme (date à venir).

Ce README peut servir de base à la rédaction de votre rapport, mais n'en
remplace pas la rédaction académique formelle (introduction, contexte,
captures d'écran des tests, conclusion, bibliographie Zotero).

Un diagramme d'architecture (vue Docker, vue Kubernetes, diagramme de
séquence du scénario métier) est fourni au format Mermaid dans
`docs/architecture.md` — à insérer dans le rapport (capture d'écran depuis
un rendu Mermaid, ou reproduit avec draw.io/LaTeX-tikz).

---

## Arborescence complète du projet

```
projet5-restauration/
├── README.md
├── compose.yaml                    # Phase 2 : fichier maître (include)
├── test-scenario.sh                # test de bout en bout du scénario métier
├── Vagrantfile                     # Phase 3
├── .gitignore
├── docs/
│   └── architecture.md             # diagrammes Mermaid pour le rapport
├── k8s/                            # Phase 4
│   ├── 00-namespace.yaml
│   ├── 01-secrets.yaml
│   ├── 02-configmaps.yaml
│   ├── 03-responsabilite-db.yaml
│   ├── 04-responsabilite-app.yaml
│   ├── 05-commandes-db.yaml
│   ├── 06-commandes-app.yaml
│   ├── 07-paiement-db.yaml
│   ├── 08-paiement-app.yaml
│   ├── 09-ingress.yaml
│   └── build-and-import.sh
├── responsabilite-service/         # Phase 1
│   ├── pom.xml
│   ├── Dockerfile
│   ├── compose.yaml
│   └── src/main/...
├── commandes-service/               # Phase 1
│   ├── pom.xml
│   ├── Dockerfile
│   ├── compose.yaml
│   └── src/main/...
└── paiement-service/                 # Phase 1
    ├── pom.xml
    ├── Dockerfile
    ├── compose.yaml
    └── src/main/...
```
