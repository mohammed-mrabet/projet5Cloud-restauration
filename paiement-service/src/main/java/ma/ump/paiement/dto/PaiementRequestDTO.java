package ma.ump.paiement.dto;

import ma.ump.paiement.entity.ModePaiement;

/** Corps JSON attendu par POST /api/paiements (envoyé par le service "Commandes"). */
public class PaiementRequestDTO {
    private Long commandeId;
    private Double montant;
    private ModePaiement modePaiement;

    public Long getCommandeId() { return commandeId; }
    public void setCommandeId(Long commandeId) { this.commandeId = commandeId; }

    public Double getMontant() { return montant; }
    public void setMontant(Double montant) { this.montant = montant; }

    public ModePaiement getModePaiement() { return modePaiement; }
    public void setModePaiement(ModePaiement modePaiement) { this.modePaiement = modePaiement; }
}
