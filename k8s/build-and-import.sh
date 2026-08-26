#!/usr/bin/env bash
# =========================================================
# k8s/build-and-import.sh
# =========================================================
# Les VMs Vagrant n'ont pas accès à un registre Docker externe (Docker Hub).
# Ce script :
#   1. Exporte les 3 images construites en local vers des fichiers .tar,
#      déposés dans k3s-images/ à la racine du projet (= /vagrant/k3s-images
#      vu depuis les VMs, puisque /vagrant est le dossier partagé Vagrant).
#   2. Importe automatiquement ces .tar dans containerd (runtime de K3s) sur
#      LES DEUX nœuds (k3s-server ET k3s-agent), via "vagrant ssh -c".
#      C'est important : par défaut, le nœud "server" de K3s peut, lui aussi,
#      recevoir des Pods (il n'est pas taint comme "control-plane only"), donc
#      un Pod peut être ordonnancé sur n'importe quel des deux nœuds - les deux
#      doivent donc connaître les images.
#
# Utilisation (depuis la racine du projet, sur votre machine hôte) :
#   docker compose build
#   ./k8s/build-and-import.sh
#
# Puis :
#   export KUBECONFIG=./k3s.yaml
#   kubectl apply -f k8s/

set -e
cd "$(dirname "$0")/.."   # se replacer à la racine du projet, quel que soit le cwd

SERVICES=(responsabilite-service commandes-service paiement-service)
NODES=(k3s-server k3s-agent)

echo "== Étape 1/2 : export des images Docker vers des archives .tar =="
mkdir -p k3s-images
for svc in "${SERVICES[@]}"; do
  echo ">> docker save ${svc}:1.0.0 -o k3s-images/${svc}.tar"
  docker save "${svc}:1.0.0" -o "k3s-images/${svc}.tar"
done

echo ""
echo "== Étape 2/2 : import dans containerd sur chaque nœud K3s =="
for node in "${NODES[@]}"; do
  for svc in "${SERVICES[@]}"; do
    echo ">> [$node] import de ${svc}.tar"
    vagrant ssh "$node" -c "sudo k3s ctr images import /vagrant/k3s-images/${svc}.tar" || {
      echo "!! Échec sur $node pour ${svc} - vérifiez que 'vagrant up' a bien été exécuté."
      exit 1
    }
  done
done

echo ""
echo "Images importées avec succès sur k3s-server et k3s-agent."
echo "Vous pouvez maintenant lancer : export KUBECONFIG=./k3s.yaml && kubectl apply -f k8s/"
