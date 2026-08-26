package ma.ump.responsabilite.entity;

import jakarta.persistence.*;

/**
 * Régime alimentaire spécial (végétarien, sans gluten, halal, ...)
 * qu'un plat peut respecter.
 */
@Entity
@Table(name = "regime_special")
public class RegimeSpecial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nom; // ex: VEGETARIEN, SANS_GLUTEN, HALAL, SANS_LACTOSE

    private String description;

    public RegimeSpecial() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
