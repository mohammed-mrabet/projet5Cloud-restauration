package ma.ump.commandes.entity;

import jakarta.persistence.*;
import jakarta.json.bind.annotation.JsonbTransient;

/**
 * Ligne d'une commande : un plat (référencé par son id dans le service
 * "Responsabilité"), une quantité, et une copie du nom/prix tels qu'ils
 * étaient au moment de la commande (snapshot - évite qu'un changement
 * de prix ultérieur ne modifie une commande déjà passée).
 */
@Entity
@Table(name = "ligne_commande")
public class LigneCommande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plat_id", nullable = false)
    private Long platId;

    @Column(name = "nom_plat")
    private String nomPlat;

    @Column(nullable = false)
    private Integer quantite;

    @Column(name = "prix_unitaire", nullable = false)
    private Double prixUnitaire;

    @ManyToOne
    @JoinColumn(name = "commande_id")
    @JsonbTransient // évite une boucle infinie de sérialisation JSON (commande <-> lignes)
    private Commande commande;

    public LigneCommande() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPlatId() { return platId; }
    public void setPlatId(Long platId) { this.platId = platId; }

    public String getNomPlat() { return nomPlat; }
    public void setNomPlat(String nomPlat) { this.nomPlat = nomPlat; }

    public Integer getQuantite() { return quantite; }
    public void setQuantite(Integer quantite) { this.quantite = quantite; }

    public Double getPrixUnitaire() { return prixUnitaire; }
    public void setPrixUnitaire(Double prixUnitaire) { this.prixUnitaire = prixUnitaire; }

    public Commande getCommande() { return commande; }
    public void setCommande(Commande commande) { this.commande = commande; }
}
