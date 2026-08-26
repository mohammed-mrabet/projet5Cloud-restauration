# unstick-agent.ps1
# ------------------------------------------------------------------
# Contourne un bug connu de VirtualBox avec les invites Ubuntu 22.04 :
# la VM se bloque au demarrage a "Loading essential drivers" sans
# jamais continuer. Une pause/reprise force VirtualBox a debloquer
# l'horloge virtuelle du CPU invite.
#
# UTILISATION :
#   1) Lance "vagrant up" (ou "vagrant up k3s-agent") dans un premier
#      terminal PowerShell, et laisse-le tourner.
#   2) Ouvre un SECOND terminal PowerShell, et lance ce script :
#        .\unstick-agent.ps1
#   3) Il tente jusqu'a 5 cycles pause/reprise espaces de quelques
#      secondes. Des que la VM redemarre reellement, "vagrant up"
#      reprend tout seul dans le premier terminal.
# ------------------------------------------------------------------

$VBoxManage = "C:\Program Files\Oracle\VirtualBox\VBoxManage.exe"
$VMName     = "k3s-agent"
$MaxCycles  = 5

Write-Host "Tentative de deblocage de la VM '$VMName' (pause/reprise automatique)..."

for ($i = 1; $i -le $MaxCycles; $i++) {
    Write-Host "  Cycle $i / $MaxCycles"

    $state = & $VBoxManage showvminfo $VMName --machinereadable 2>$null | Select-String '^VMState='
    Write-Host "    Etat actuel : $state"

    & $VBoxManage controlvm $VMName pause 2>$null
    Start-Sleep -Seconds 2
    & $VBoxManage controlvm $VMName resume 2>$null

    Start-Sleep -Seconds 6

    # Si le port SSH forwarde (2200) repond, la VM a probablement demarre :
    $ssh = Test-NetConnection -ComputerName 127.0.0.1 -Port 2200 -WarningAction SilentlyContinue
    if ($ssh.TcpTestSucceeded) {
        Write-Host "Port SSH (2200) repond : la VM semble avoir demarre. Retour a 'vagrant up'."
        exit 0
    }
}

Write-Host "Toujours pas de reponse SSH apres $MaxCycles cycles."
Write-Host "Ouvre la fenetre VirtualBox de '$VMName' et regarde ou elle en est."
