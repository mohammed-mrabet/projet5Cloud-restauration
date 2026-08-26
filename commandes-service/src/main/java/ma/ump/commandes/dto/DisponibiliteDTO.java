package ma.ump.commandes.dto;

/**
 * Copie locale du contrat exposé par GET /api/plats/{id}/disponibilite
 * du service "Responsabilité". En architecture microservices, chaque
 * service définit son propre DTO pour ce dont il a besoin chez les autres :
 * cela évite de partager une librairie commune et garde les services
 * vraiment indépendants (vous pouvez faire évoluer Responsabilité sans
 * casser Commandes, tant que ce sous-ensemble de champs reste stable).
 */
public class DisponibiliteDTO {
    private Long platId;
    private String nom;
    private Double prix;
    private Boolean disponible;

    public Long getPlatId() { return platId; }
    public void setPlatId(Long platId) { this.platId = platId; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public Double getPrix() { return prix; }
    public void setPrix(Double prix) { this.prix = prix; }

    public Boolean getDisponible() { return disponible; }
    public void setDisponible(Boolean disponible) { this.disponible = disponible; }
}
