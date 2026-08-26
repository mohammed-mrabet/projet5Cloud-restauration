# Guide d'exécution — Projet 5 : Restauration universitaire

Ce guide explique, étape par étape, **comment exécuter concrètement** les
4 phases du projet sur votre machine : quel outil sert à quoi, dans quel
ordre travailler, et quelle commande taper à chaque étape.

---

## 0. Comprendre la logique générale avant de commencer

Le projet a 4 phases, mais elles **ne demandent pas toutes les mêmes outils** :

| Phase | Outil principal | Tourne où ? |
|---|---|---|
| 1 — Services individuels | Docker | Directement sur votre machine |
| 2 — Regroupement (compose) | Docker Compose | Directement sur votre machine |
| 3 — Infrastructure (Vagrant) | Vagrant + VirtualBox | Crée 2 machines virtuelles |
| 4 — Kubernetes (K3s) | kubectl | À l'intérieur des VMs créées en Phase 3 |

**Point important à comprendre :** les Phases 1 et 2 sont indépendantes des
Phases 3 et 4. Vous pouvez faire tourner tout le scénario métier (créer des
plats, passer une commande, payer) uniquement avec Docker, sans jamais
toucher à Vagrant. Les Phases 3 et 4 servent à rejouer la même chose, mais
sur un "mini cloud" que vous construisez vous-même (2 VMs + un orchestrateur
Kubernetes).

Ordre de travail recommandé : **Phase 1 → Phase 2 → Phase 3 → Phase 4**,
dans cet ordre, sans sauter d'étape — chaque phase valide que la précédente
fonctionne avant de compliquer les choses.

---

## 1. Environnement de travail requis

### Outils nécessaires

| Outil | Pour quelle(s) phase(s) | Vous l'avez déjà ? |
|---|---|---|
| Docker + Docker Compose | 1, 2, et pour construire les images de la phase 4 | Oui (dites-vous) |
| Vagrant | 3, 4 | Oui (dites-vous) |
| VirtualBox (ou autre provider Vagrant) | 3, 4 | À vérifier — voir ci-dessous |
| `kubectl` | 4 (facultatif, voir plus bas) | À installer si besoin |
| `curl` | 1, 2, 4 (tester les API) | Généralement déjà présent |
| `jq` (facultatif) | Juste pour un affichage JSON plus lisible | Facultatif |

**Vous n'avez pas besoin d'installer Java ni Maven en local.** Le
`Dockerfile` de chaque service compile le code Java **à l'intérieur** du
conteneur (étape "build" du multi-stage). Docker s'occupe de tout.

### Vérifier que tout est bien installé

Ouvrez un terminal et tapez :
```bash
docker --version
docker compose version
vagrant --version
vboxmanage --version
```
Si une de ces commandes renvoie "command not found", il faut installer
l'outil manquant avant de continuer. `vboxmanage --version` confirme que
VirtualBox (le "provider" par défaut de Vagrant) est bien installé — c'est
souvent l'outil oublié, même quand Vagrant est présent.

### Ressources machine nécessaires

- **Phases 1 et 2** : légères, 2-4 Go de RAM libres suffisent.
- **Phases 3 et 4** : plus lourdes. Le `Vagrantfile` crée 2 VMs de 2 Go de
  RAM chacune → prévoyez **au moins 6-8 Go de RAM libres** sur votre machine
  hôte en plus de ce qu'utilisent votre OS et vos autres applications.
  Si votre machine est limitée, vous pouvez réduire `vb.memory = 2048` à
  `1024` dans le `Vagrantfile`, au prix de performances plus lentes.
- **Virtualisation matérielle (VT-x/AMD-V)** : doit être activée dans le
  BIOS/UEFI de votre machine pour que VirtualBox fonctionne. Si `vagrant up`
  échoue avec une erreur de type "VT-x is disabled" ou "raw-mode is
  unavailable", c'est généralement ça (voir section Phase 3, dépannage).

### Note pour utilisateurs Windows

Les scripts fournis (`test-scenario.sh`, `k8s/build-and-import.sh`) sont
des scripts **bash**. Sur Windows, utilisez soit :
- **WSL2** (recommandé — Docker Desktop s'intègre nativement avec),
- soit **Git Bash**.

Les commandes `docker`, `docker compose` et `vagrant` elles-mêmes
fonctionnent aussi bien en PowerShell, mais les scripts `.sh` non.

---

## 2. Récupérer et explorer le projet

```bash
unzip projet5-restauration.zip
cd projet5-restauration
ls -la
```

Vous devez voir : `compose.yaml`, `Vagrantfile`, les 3 dossiers de services
(`responsabilite-service`, `commandes-service`, `paiement-service`), le
dossier `k8s/`, `README.md`, et ce guide.

Lisez d'abord `README.md` : il explique l'architecture et le rôle de chaque
fichier. Ce guide-ci se concentre uniquement sur **les commandes à taper**.

---

## 3. PHASE 1 — Faire tourner chaque service seul

### Objectif
Vérifier que chaque service (application + sa base MySQL) démarre
correctement, **isolément**, avant de les faire communiquer entre eux.

### Étape 1.1 — Le service "Responsabilité"

```bash
cd responsabilite-service
docker compose up --build
```

**Ce qui se passe (la première fois, ~3-5 minutes) :**
1. Docker télécharge l'image `maven:3.9-eclipse-temurin-17` (étape build).
2. Maven télécharge les dépendances Jakarta EE et compile le code → produit
   `responsabilite-service.war`.
3. Docker télécharge l'image `quay.io/wildfly/wildfly` (étape run) et y
   copie le `.war`.
4. Docker télécharge l'image `mysql:8.4` et démarre la base de données.
5. WildFly attend que MySQL soit **healthy** (grâce au `healthcheck` du
   `compose.yaml`) avant de démarrer complètement.

Vous devrez voir dans les logs, à la fin, une ligne du type
`WildFly Full ... started in XXXXms - Started XXX of YYY services`.
Laissez ce terminal ouvert (le processus reste au premier plan).

**Dans un second terminal**, testez l'API :
```bash
curl http://localhost:8081/responsabilite-service/api/plats
```
→ doit renvoyer `[]` (liste vide, normal : aucun plat créé encore).

Créez un plat pour vérifier que l'écriture en base fonctionne :
```bash
curl -X POST http://localhost:8081/responsabilite-service/api/plats \
  -H "Content-Type: application/json" \
  -d '{"nom":"Tajine de poulet","categorie":"PLAT_PRINCIPAL","prix":25,"disponible":true}'
```
→ doit renvoyer le plat créé, avec un `"id":1`.

Revérifiez la liste :
```bash
curl http://localhost:8081/responsabilite-service/api/plats
```
→ doit maintenant afficher le plat créé.

**Pour arrêter ce service** (dans le premier terminal, `Ctrl+C`, puis) :
```bash
docker compose down
```
Ajoutez `-v` (`docker compose down -v`) si vous voulez aussi supprimer les
données MySQL enregistrées (repartir de zéro).

### Étape 1.2 — Le service "Commandes"

```bash
cd ../commandes-service
docker compose up --build
```
Testez (deuxième terminal) :
```bash
curl http://localhost:8082/commandes-service/api/commandes
```
→ doit renvoyer `[]`.

**Normal et attendu :** si vous essayez de créer une commande maintenant
(`POST /commandes`), ça échouera, car ce service a besoin d'appeler
`responsabilite-service` pour vérifier les plats — et ce conteneur n'existe
pas en isolation. C'est exactement pour ça que la Phase 2 existe.

Arrêtez : `Ctrl+C` puis `docker compose down`.

### Étape 1.3 — Le service "Paiement"

```bash
cd ../paiement-service
docker compose up --build
```
Testez :
```bash
curl http://localhost:8083/paiement-service/api/paiements
```
→ doit renvoyer `[]`.

Arrêtez : `Ctrl+C` puis `docker compose down`.

### Dépannage Phase 1

| Symptôme | Cause probable | Solution |
|---|---|---|
| `port is already allocated` | Un autre programme utilise déjà le port 8081/8082/8083/3306 | Arrêtez l'autre programme, ou changez le port dans `compose.yaml` (partie gauche du `"8081:8080"`) |
| Le build reste bloqué très longtemps sur `mvn dependency:go-offline` | Téléchargement des dépendances Maven lent (première fois) | Patience, c'est mis en cache pour les prochains builds |
| `mysqladmin ping` healthcheck échoue en boucle | MySQL met parfois plus de 10s à démarrer la première fois | Attendez, ou augmentez `retries` dans le `compose.yaml` |
| `curl: (7) Failed to connect` | Le service n'a pas fini de démarrer | Attendez la ligne "WildFly ... started" dans les logs avant de tester |

---

## 4. PHASE 2 — Regroupement (compose.yaml maître avec `include`)

### Objectif
Faire tourner les 3 services **ensemble**, sur le même réseau Docker, pour
que `commandes-service` puisse réellement appeler `responsabilite-service`
et `paiement-service` en interne.

### Étape 2.1 — Revenir à la racine et lancer l'ensemble

```bash
cd ..   # vous devez être dans projet5-restauration/
docker compose up --build -d
```
Le `-d` lance en arrière-plan ("detached") — plus pratique ici puisqu'on
gère 6 conteneurs (3 apps + 3 bases) en même temps.

Vérifiez que tout tourne :
```bash
docker compose ps
```
Vous devez voir 6 lignes, toutes avec un statut `Up` (ou `running`), et les
3 bases MySQL avec un statut `(healthy)`.

Si un service applicatif n'est pas encore prêt, regardez ses logs :
```bash
docker compose logs -f commandes-service
```
(`Ctrl+C` pour quitter l'affichage des logs sans arrêter le conteneur.)

### Étape 2.2 — Lancer le scénario de test automatisé

```bash
chmod +x test-scenario.sh
./test-scenario.sh
```
Ce script exécute tout le scénario métier automatiquement : création d'un
régime, création de 2 plats, vérification de disponibilité, création d'une
commande à 2 lignes (ce qui déclenche l'appel interne vers
`responsabilite-service`), test d'un plat inexistant (doit renvoyer 409),
paiement de la commande (appel interne vers `paiement-service`), et
vérification de la facture générée. Chaque étape affiche sa réponse JSON.

> Si `jq` n'est pas installé, le script fonctionne quand même (JSON brut
> affiché au lieu d'un JSON indenté). Pour l'installer : `sudo apt install jq`
> (Linux) ou `brew install jq` (Mac).

### Étape 2.3 — Comprendre ce qui vient de se passer (optionnel mais utile)

Pendant que le script tourne, ouvrez un second terminal et observez les
logs du service Commandes en temps réel :
```bash
docker compose logs -f commandes-service
```
Vous ne verrez pas explicitement "j'appelle responsabilite-service" dans
les logs par défaut (le code ne logue pas ces appels), mais vous pouvez
constater que la commande est bien créée avec le bon prix — preuve que
l'appel REST interne a fonctionné (sinon vous auriez une erreur `409`).

### Étape 2.4 — Arrêter proprement

```bash
docker compose down       # arrête et supprime les conteneurs, garde les données
docker compose down -v    # pareil, mais supprime aussi les données MySQL
```

### Dépannage Phase 2

| Symptôme | Cause probable | Solution |
|---|---|---|
| `409 Conflict` sur toutes les commandes | `responsabilite-service` n'est pas encore prêt, ou aucun plat n'existe | Attendez, ou recréez un plat via `curl POST /plats` |
| Le script `test-scenario.sh` plante sur l'extraction de l'id (`jq`/`grep`) | Le service n'a pas répondu en JSON valide (erreur 500 côté serveur) | Relancez `docker compose logs responsabilite-service` pour voir l'erreur exacte |
| `network restauration-net declared twice` (rare, selon version de Compose) | Fusion des réseaux entre fichiers inclus | Mettez à jour Docker Compose (`docker compose version` doit être ≥ v2.20), ou supprimez le bloc `networks:` de chaque `compose.yaml` de service (voir commentaire dans le fichier) |

---

## 5. PHASE 3 — Infrastructure as Code avec Vagrant

### Objectif
Créer 2 machines virtuelles Ubuntu et y installer automatiquement un
cluster K3s (Kubernetes léger), en scriptant tout dans le `Vagrantfile`.

### Étape 3.1 — Lancer la création des VMs

Toujours à la racine du projet :
```bash
vagrant up
```

**Ce qui se passe (première fois, 5-15 minutes selon votre connexion) :**
1. Vagrant télécharge la "box" `ubuntu/jammy64` (image de base de la VM).
2. Il crée et démarre la VM `k3s-server`, lui assigne l'IP `192.168.56.10`,
   puis exécute le script d'installation K3s (le `provision "shell"` du
   Vagrantfile) : installation de curl, puis `curl -sfL https://get.k3s.io | sh -`.
3. Il crée et démarre la VM `k3s-agent` (IP `192.168.56.11`), qui attend que
   le fichier `node-token` existe (généré par le serveur) puis rejoint le
   cluster.
4. Le fichier `k3s.yaml` (identifiants de connexion au cluster) est copié à
   la racine de votre projet, côté hôte — vous le verrez apparaître dans
   `ls` après quelques minutes.

### Étape 3.2 — Vérifier que le cluster fonctionne

```bash
vagrant status
```
→ les deux VMs doivent être `running`.

```bash
vagrant ssh k3s-server -c "sudo k3s kubectl get nodes"
```
→ doit afficher 2 nœuds (`k3s-server` et `k3s-agent`), tous les deux avec
le statut `Ready`. Si `k3s-agent` affiche `NotReady`, attendez 1-2 minutes
(l'installation peut encore être en cours) puis retapez la commande.

### Étape 3.3 (facultatif) — Piloter le cluster depuis votre machine hôte

Si vous avez installé `kubectl` en local, vous pouvez éviter de faire
`vagrant ssh` à chaque fois :
```bash
export KUBECONFIG=$(pwd)/k3s.yaml
kubectl get nodes
```
(Cette variable d'environnement `KUBECONFIG` n'est valable que dans le
terminal courant — à refaire si vous ouvrez un nouveau terminal.)

Si vous **n'avez pas** `kubectl` en local, ce n'est pas grave : toutes les
commandes `kubectl ...` de ce guide peuvent être exécutées à la place avec
`vagrant ssh k3s-server -c "sudo k3s kubectl ..."`.

### Dépannage Phase 3

| Symptôme | Cause probable | Solution |
|---|---|---|
| `VT-x is disabled in the BIOS` | Virtualisation matérielle désactivée | Activez VT-x (Intel) ou AMD-V dans le BIOS/UEFI de votre machine |
| `vagrant up` bloqué très longtemps sur le téléchargement de la box | Connexion lente, première fois | Patience — mise en cache ensuite dans `~/.vagrant.d/boxes` |
| `k3s-agent` reste `NotReady` indéfiniment | Le token n'a pas été bien transmis, ou pare-feu bloquant le port 6443 entre VMs | `vagrant ssh k3s-agent -c "sudo journalctl -u k3s-agent -n 50"` pour voir l'erreur exacte |
| `Vagrant was unable to mount VirtualBox shared folders` | Extension "Guest Additions" manquante dans la box | Rare avec `ubuntu/jammy64` ; sinon `vagrant plugin install vagrant-vbguest` |

---

## 6. PHASE 4 — Déploiement sur Kubernetes (K3s)

### Objectif
Migrer les 3 microservices + leurs bases vers le cluster K3s créé en
Phase 3, avec Secrets, ConfigMaps, persistance des données, et un Ingress.

**Pré-requis : la Phase 3 doit être terminée** (`vagrant status` → 2 VMs
`running`, `kubectl get nodes` → 2 nœuds `Ready`).

### Étape 4.1 — Construire les images Docker (si pas déjà fait en Phase 2)

```bash
docker compose build
```
Cette commande reconstruit les 3 images (`responsabilite-service:1.0.0`,
`commandes-service:1.0.0`, `paiement-service:1.0.0`) si elles n'existent pas
déjà localement. Vérifiez :
```bash
docker images | grep -E "responsabilite-service|commandes-service|paiement-service"
```

### Étape 4.2 — Exporter puis importer les images dans le cluster

Les VMs n'ont pas accès à un registre Docker externe (Docker Hub) : il faut
donc transférer les images manuellement.
```bash
chmod +x k8s/build-and-import.sh
./k8s/build-and-import.sh
```
Ce script fait tout automatiquement : il exporte chaque image en `.tar`
(à la racine du projet, donc visible depuis `/vagrant` dans les VMs), puis
se connecte à `k3s-server` **et** `k3s-agent` via `vagrant ssh -c` pour
importer chaque `.tar` dans containerd (`sudo k3s ctr images import ...`).
Comptez 1-2 minutes.

Vous devez voir à la fin : `Images importées avec succès sur k3s-server et k3s-agent.`

### Étape 4.3 — Appliquer les manifestes Kubernetes

**Option A — avec `kubectl` installé en local :**
```bash
export KUBECONFIG=$(pwd)/k3s.yaml
kubectl apply -f k8s/
```

**Option B — sans `kubectl` en local** (via la VM, en utilisant le dossier
partagé `/vagrant`) :
```bash
vagrant ssh k3s-server -c "sudo k3s kubectl apply -f /vagrant/k8s/"
```

Les deux options font exactement la même chose. La sortie doit lister la
création de chaque ressource :
```
namespace/restauration created
secret/responsabilite-db-secret created
configmap/responsabilite-db-config created
...
deployment.apps/responsabilite-service created
...
ingress.networking.k8s.io/restauration-ingress created
```

### Étape 4.4 — Observer le démarrage des Pods

```bash
kubectl get pods -n restauration -w
```
(ou avec l'option B : `vagrant ssh k3s-server -c "sudo k3s kubectl get pods -n restauration -w"`)

Le `-w` ("watch") rafraîchit l'affichage en direct. Vous devez voir passer
chaque Pod par les statuts `Pending` → `ContainerCreating` → `Running`.
Les Pods MySQL démarrent en premier (quelques secondes), les Pods
applicatifs prennent plus longtemps (WildFly + attente de la base, jusqu'à
40-60 secondes selon la config des probes). `Ctrl+C` pour arrêter le watch
une fois que tout est `Running` avec `1/1` prêt.

Si un Pod reste bloqué en `Pending` ou passe en `CrashLoopBackOff`, voir
la section dépannage ci-dessous.

### Étape 4.5 — Tester via l'Ingress

L'Ingress route le trafic selon le chemin, sur le nœud `k3s-server`
(IP `192.168.56.10`). Deux façons de tester :

**Avec l'en-tête HTTP `Host` directement (le plus simple, pas de config à faire) :**
```bash
curl -H "Host: restauration.local" http://192.168.56.10/responsabilite-service/api/plats
```

**Ou en modifiant votre fichier `hosts`** (pour naviguer aussi depuis un
navigateur web) — ajoutez cette ligne :
- Linux/Mac : dans `/etc/hosts`
- Windows : dans `C:\Windows\System32\drivers\etc\hosts`
```
192.168.56.10   restauration.local
```
Puis testez normalement :
```bash
curl http://restauration.local/responsabilite-service/api/plats
curl http://restauration.local/commandes-service/api/commandes
curl http://restauration.local/paiement-service/api/paiements
```

### Étape 4.6 — Vérifier la persistance des données

Pour valider l'exigence "persistance lors du redémarrage des Pods" :
```bash
# créez un plat via l'Ingress (adaptez la commande curl -X POST vue en Phase 1)
kubectl delete pod -n restauration -l app=responsabilite-db
# attendez que le pod redémarre (kubectl get pods -n restauration -w)
curl http://restauration.local/responsabilite-service/api/plats
# → le plat créé avant la suppression doit toujours être présent
```
C'est le `PersistentVolumeClaim` qui garantit ça : le volume de données
n'est pas supprimé quand le Pod redémarre, seulement le conteneur.

### Dépannage Phase 4

| Symptôme (`kubectl get pods -n restauration`) | Cause probable | Solution |
|---|---|---|
| `ErrImageNeverPull` | L'image n'a pas été importée sur le nœud où le Pod a été placé | Relancez `./k8s/build-and-import.sh` (il importe sur les 2 nœuds) |
| `Pending` indéfiniment | PVC non satisfait, ou pas assez de ressources sur les nœuds | `kubectl describe pod -n restauration <nom-du-pod>` pour voir l'événement exact |
| `CrashLoopBackOff` sur un service applicatif | La base de données correspondante n'est pas encore prête | `kubectl logs -n restauration <nom-du-pod>` pour voir l'erreur ; attendez, K8s réessaiera automatiquement |
| `curl: (7) Failed to connect` sur l'Ingress | Traefik pas encore démarré, ou mauvaise IP | Vérifiez `kubectl get pods -n kube-system \| grep traefik` |
| Modifier un manifeste ne se répercute pas | Il faut réappliquer après modification | `kubectl apply -f k8s/` (idempotent, ne casse rien de déjà en place) |

Commandes de diagnostic générales, à connaître par cœur :
```bash
kubectl get all -n restauration              # vue d'ensemble de toutes les ressources
kubectl describe pod -n restauration <nom>   # détail + événements d'un Pod (très utile en cas de blocage)
kubectl logs -n restauration <nom>           # logs applicatifs d'un Pod
kubectl exec -it -n restauration <nom> -- bash   # ouvrir un shell dans un Pod pour investiguer
```

---

## 7. Nettoyage complet (à la fin de vos tests, ou avant de "tout recommencer")

```bash
# Arrêter et supprimer les conteneurs Docker + leurs volumes
docker compose down -v

# Détruire complètement les VMs Vagrant (libère toute la RAM/disque utilisés)
vagrant destroy -f

# (optionnel) libérer de l'espace disque Docker
docker system prune -af
```
`vagrant destroy -f` supprime aussi le cluster K3s puisqu'il vivait dans
les VMs. Pour tout recréer, il suffit de refaire `vagrant up` puis les
étapes de la Phase 4 (les images Docker construites en Phase 1/2 restent
disponibles localement, pas besoin de refaire `docker compose build`, sauf
si vous avez modifié le code).

---

## 8. Récapitulatif — checklist dans l'ordre

- [ ] `docker --version`, `vagrant --version`, `vboxmanage --version` → tout est installé
- [ ] Phase 1 : chaque service démarre seul (`docker compose up --build` dans chacun des 3 dossiers), testé au `curl`
- [ ] Phase 2 : `docker compose up --build -d` à la racine, puis `./test-scenario.sh` passe sans erreur
- [ ] Phase 3 : `vagrant up`, `vagrant status` → 2 VMs `running`, `kubectl get nodes` → 2 nœuds `Ready`
- [ ] Phase 4 : `./k8s/build-and-import.sh`, `kubectl apply -f k8s/`, `kubectl get pods -n restauration` → tout `Running`, test via l'Ingress OK
- [ ] Captures d'écran de chaque étape prises au fur et à mesure (pour le rapport)
- [ ] Nettoyage fait avant de rendre la machine/VM si besoin (`docker compose down -v`, `vagrant destroy -f`)

---

## 9. Conseil pour le rapport et la présentation

Prenez une capture d'écran à chaque étape clé de ce guide (surtout les
sorties de `docker compose ps`, `./test-scenario.sh`, `kubectl get nodes`,
`kubectl get pods -n restauration`, et un test `curl` réussi via l'Ingress).
Ce sont exactement les preuves que votre encadrant attendra de voir dans le
rapport et pendant votre présentation de 15 minutes.
