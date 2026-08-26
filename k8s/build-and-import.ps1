# =========================================================
# k8s/build-and-import.ps1
# =========================================================
# Equivalent PowerShell de build-and-import.sh, pour executer directement
# sur Windows sans Git Bash ni WSL.
#
# Utilisation (depuis la racine du projet, dans PowerShell) :
#   .\k8s\build-and-import.ps1

$ErrorActionPreference = "Stop"

# Se replacer a la racine du projet, quel que soit le dossier d'appel
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$services = @("responsabilite-service", "commandes-service", "paiement-service")
$nodes    = @("k3s-server", "k3s-agent")

Write-Host "== Etape 1/2 : export des images Docker vers des archives .tar =="
New-Item -ItemType Directory -Force -Path "k3s-images" | Out-Null

foreach ($svc in $services) {
    Write-Host ">> docker save ${svc}:1.0.0 -o k3s-images\$svc.tar"
    docker save "${svc}:1.0.0" -o "k3s-images\$svc.tar"
}

Write-Host ""
Write-Host "== Etape 2/2 : import dans containerd sur chaque noeud K3s =="

foreach ($node in $nodes) {
    foreach ($svc in $services) {
        Write-Host ">> [$node] import de $svc.tar"
        vagrant ssh $node -c "sudo k3s ctr images import /vagrant/k3s-images/$svc.tar"
        if ($LASTEXITCODE -ne 0) {
            Write-Host "!! Echec sur $node pour $svc - verifiez que 'vagrant up' a bien ete execute." -ForegroundColor Red
            exit 1
        }
    }
}

Write-Host ""
Write-Host "Images importees avec succes sur k3s-server et k3s-agent." -ForegroundColor Green
Write-Host "Vous pouvez maintenant lancer :"
Write-Host '  $env:KUBECONFIG = ".\k3s.yaml"; kubectl apply -f k8s\'
