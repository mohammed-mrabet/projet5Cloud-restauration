package ma.ump.responsabilite.dto;

/**
 * Petit objet renvoyé par GET /api/plats/{id}/disponibilite.
 * C'est le "contrat" REST utilisé par le service "Commandes" pour
 * vérifier l'existence, la disponibilité et le prix d'un plat
 * avant de valider un panier (communication Backend-to-Backend).
 */
public class DisponibiliteDTO {

    private Long platId;
    private String nom;
    private Double prix;
    private Boolean disponible;

    public DisponibiliteDTO() {}

    public DisponibiliteDTO(Long platId, String nom, Double prix, Boolean disponible) {
        this.platId = platId;
        this.nom = nom;
        this.prix = prix;
        this.disponible = disponible;
    }

    public Long getPlatId() { return platId; }
    public void setPlatId(Long platId) { this.platId = platId; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public Double getPrix() { return prix; }
    public void setPrix(Double prix) { this.prix = prix; }

    public Boolean getDisponible() { return disponible; }
    public void setDisponible(Boolean disponible) { this.disponible = disponible; }
}
