package ma.ump.commandes.dto;

/** Copie locale de la réponse renvoyée par POST /api/paiements du service "Paiement". */
public class PaiementResultDTO {
    private Long id;
    private Long commandeId;
    private Double montant;
    private String statut; // EN_ATTENTE, VALIDE, ECHEC, REMBOURSE

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCommandeId() { return commandeId; }
    public void setCommandeId(Long commandeId) { this.commandeId = commandeId; }

    public Double getMontant() { return montant; }
    public void setMontant(Double montant) { this.montant = montant; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
}
