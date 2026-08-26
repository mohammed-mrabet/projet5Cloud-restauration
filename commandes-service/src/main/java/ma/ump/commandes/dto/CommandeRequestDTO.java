package ma.ump.commandes.dto;

import ma.ump.commandes.entity.TypeCommande;
import java.util.List;

/** Corps JSON attendu par POST /api/commandes. */
public class CommandeRequestDTO {
    private Long etudiantId;
    private TypeCommande type;
    private List<LigneRequestDTO> lignes;

    public Long getEtudiantId() { return etudiantId; }
    public void setEtudiantId(Long etudiantId) { this.etudiantId = etudiantId; }

    public TypeCommande getType() { return type; }
    public void setType(TypeCommande type) { this.type = type; }

    public List<LigneRequestDTO> getLignes() { return lignes; }
    public void setLignes(List<LigneRequestDTO> lignes) { this.lignes = lignes; }
}
