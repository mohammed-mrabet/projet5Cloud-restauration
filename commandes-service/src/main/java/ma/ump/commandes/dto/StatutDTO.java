package ma.ump.commandes.dto;

import ma.ump.commandes.entity.StatutCommande;

/** Corps JSON attendu par PUT /api/commandes/{id}/statut. */
public class StatutDTO {
    private StatutCommande statut;

    public StatutCommande getStatut() { return statut; }
    public void setStatut(StatutCommande statut) { this.statut = statut; }
}
