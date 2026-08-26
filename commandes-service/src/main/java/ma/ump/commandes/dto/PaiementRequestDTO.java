package ma.ump.commandes.dto;

/** Corps JSON attendu par POST /api/commandes/{id}/payer. */
public class PaiementRequestDTO {
    private String modePaiement; // CARTE, ESPECES, MOBILE

    public String getModePaiement() { return modePaiement; }
    public void setModePaiement(String modePaiement) { this.modePaiement = modePaiement; }
}
