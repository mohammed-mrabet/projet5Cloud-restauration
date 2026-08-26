package ma.ump.responsabilite.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;

/**
 * Planification d'un menu pour un restaurant universitaire donné,
 * à une date donnée (un menu regroupe plusieurs plats proposés ce jour-là).
 */
@Entity
@Table(name = "menu")
public class Menu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String restaurant; // nom/identifiant du restaurant universitaire

    @Column(name = "date_menu", nullable = false)
    private LocalDate dateMenu;

    @ManyToMany(fetch = FetchType.EAGER)
    // EAGER pour la même raison que Commande.lignes : l'EntityManager est
    // fermé avant la sérialisation JSON de la réponse (voir MenuResource).
    @JoinTable(
        name = "menu_plat",
        joinColumns = @JoinColumn(name = "menu_id"),
        inverseJoinColumns = @JoinColumn(name = "plat_id")
    )
    private List<Plat> plats;

    public Menu() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRestaurant() { return restaurant; }
    public void setRestaurant(String restaurant) { this.restaurant = restaurant; }

    public LocalDate getDateMenu() { return dateMenu; }
    public void setDateMenu(LocalDate dateMenu) { this.dateMenu = dateMenu; }

    public List<Plat> getPlats() { return plats; }
    public void setPlats(List<Plat> plats) { this.plats = plats; }
}
