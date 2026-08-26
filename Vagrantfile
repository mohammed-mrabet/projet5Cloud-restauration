# -*- mode: ruby -*-
# vi: set ft=ruby :
#
# k3s-server : RAM remontee a 3072 Mo (au lieu de 2048). Le control-plane
# K3s a lui seul (apiserver + etcd embarque/kine + scheduler + controller
# manager + kubelet + containerd, tout dans un seul processus "k3s server")
# consomme deja ~750 Mo au repos ; en ajoutant les Pods applicatifs
# programmes sur ce noeud, 2048 Mo etait insuffisant et provoquait du
# swap (donc des timeouts d'API server). Necessite que Docker Desktop
# soit ferme sur l'hote pour liberer assez de RAM physique.
#
# --nictype virtio : la carte reseau emulee par defaut (e1000) plantait en
# boucle ("Tx Unit Hang" / "Reset adapter") a cause de la combinaison avec
# --paravirtprovider hyperv (necessaire ici car Hyper-V est actif sur l'hote
# via Docker Desktop/WSL2). virtio-net est nativement supporte par Ubuntu
# 22.04 et ne souffre pas de ce bug.

Vagrant.configure("2") do |config|

  # Image de base utilisée pour les 2 machines virtuelles.
  config.vm.box = "bento/ubuntu-22.04"
  config.vm.boot_timeout = 600

  # ---------- Nœud maître (control-plane) ----------
  config.vm.define "k3s-server" do |server|
    server.vm.hostname = "k3s-server"
    # IP fixe sur un réseau privé (accessible depuis la machine hôte).
    server.vm.network "private_network", ip: "192.168.56.10"
    server.vm.boot_timeout = 600

    server.vm.provider "virtualbox" do |vb|
      vb.name = "k3s-server"
      vb.memory = 3072
      vb.cpus = 2
      vb.gui = true
      # Options qui désactivent des périphériques inutiles ici (USB, audio)
      # pour économiser un peu de ressources.
      vb.customize ["modifyvm", :id, "--paravirtprovider", "hyperv"]
      vb.customize ["modifyvm", :id, "--nictype1", "virtio"]
      vb.customize ["modifyvm", :id, "--nictype2", "virtio"]
      vb.customize ["modifyvm", :id, "--usb", "off"]
      vb.customize ["modifyvm", :id, "--usbehci", "off"]
      vb.customize ["modifyvm", :id, "--usbxhci", "off"]
      vb.customize ["modifyvm", :id, "--audio", "none"]
    end

    # Script shell exécuté au premier démarrage de la VM : installe K3s
    # en mode "server" (control-plane du cluster Kubernetes).
    server.vm.provision "shell", inline: <<-SHELL
      set -e
      apt-get update -y
      apt-get install -y curl apt-transport-https ca-certificates

      # Installation officielle de K3s (script fourni par Rancher).
      curl -sfL https://get.k3s.io | \
        INSTALL_K3S_EXEC="--write-kubeconfig-mode 644 --tls-san 192.168.56.10 --node-ip=192.168.56.10 --flannel-iface=eth1" sh -

      # On copie le token de jonction et le kubeconfig dans le dossier partagé
      # /vagrant, pour que le nœud agent (et la machine hôte) puissent les lire.
      cp /var/lib/rancher/k3s/server/node-token /vagrant/node-token
      cp /etc/rancher/k3s/k3s.yaml /vagrant/k3s.yaml
      sed -i 's/127.0.0.1/192.168.56.10/' /vagrant/k3s.yaml

      echo ">>> K3s server prêt. kubeconfig disponible dans ./k3s.yaml sur l'hôte."
    SHELL
  end

  # ---------- Nœud worker ----------
  config.vm.define "k3s-agent" do |agent|
    agent.vm.hostname = "k3s-agent"
    # IP fixe elle aussi, différente de celle du nœud maître.
    agent.vm.network "private_network", ip: "192.168.56.11"
    agent.vm.boot_timeout = 600

    agent.vm.provider "virtualbox" do |vb|
      vb.name = "k3s-agent"
      vb.memory = 2048
      vb.cpus = 2
      vb.gui = true
      vb.customize ["modifyvm", :id, "--paravirtprovider", "hyperv"]
      vb.customize ["modifyvm", :id, "--nictype1", "virtio"]
      vb.customize ["modifyvm", :id, "--nictype2", "virtio"]
      vb.customize ["modifyvm", :id, "--usb", "off"]
      vb.customize ["modifyvm", :id, "--usbehci", "off"]
      vb.customize ["modifyvm", :id, "--usbxhci", "off"]
      vb.customize ["modifyvm", :id, "--audio", "none"]
    end

    # Script shell exécuté au premier démarrage de la VM : installe K3s
    # en mode "agent" (nœud worker) et le fait rejoindre le cluster.
    agent.vm.provision "shell", inline: <<-SHELL
      set -e
      apt-get update -y
      apt-get install -y curl

      # Le nœud agent doit attendre que le nœud server ait bien créé
      # son fichier de token avant de pouvoir rejoindre le cluster.
      echo ">>> Attente du token de jonction K3s..."
      until [ -f /vagrant/node-token ]; do sleep 5; done

      # Récupère le token, puis installe K3s en mode agent en le pointant
      # vers l'IP du nœud server.
      K3S_TOKEN=$(cat /vagrant/node-token)
      curl -sfL https://get.k3s.io | K3S_URL="https://192.168.56.10:6443" K3S_TOKEN="$K3S_TOKEN" INSTALL_K3S_EXEC="--node-ip=192.168.56.11 --flannel-iface=eth1" sh -

      echo ">>> K3s agent a rejoint le cluster."
    SHELL
  end
end
