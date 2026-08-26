package ma.ump.responsabilite.entity;

import jakarta.persistence.*;

/**
 * Un plat proposé au menu (ex: "Tajine de poulet").
 * C'est CETTE entité que le service "Commandes" interrogera via REST
 * pour vérifier la disponibilité et récupérer le prix au moment
 * de la création d'un panier (voir PlatResource#verifierDisponibilite).
 */
@Entity
@Table(name = "plat")
public class Plat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    private String description;

    @Column(nullable = false)
    private String categorie; // ENTREE, PLAT_PRINCIPAL, DESSERT, BOISSON

    @Column(nullable = false)
    private Double prix;

    @Column(nullable = false)
    private Boolean disponible = true;

    @ManyToOne
    @JoinColumn(name = "regime_id")
    private RegimeSpecial regimeSpecial; // null si plat "classique"

    public Plat() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }

    public Double getPrix() { return prix; }
    public void setPrix(Double prix) { this.prix = prix; }

    public Boolean getDisponible() { return disponible; }
    public void setDisponible(Boolean disponible) { this.disponible = disponible; }

    public RegimeSpecial getRegimeSpecial() { return regimeSpecial; }
    public void setRegimeSpecial(RegimeSpecial regimeSpecial) { this.regimeSpecial = regimeSpecial; }
}
