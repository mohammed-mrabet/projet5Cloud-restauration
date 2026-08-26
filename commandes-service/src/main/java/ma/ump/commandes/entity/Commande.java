package ma.ump.commandes.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Une commande passée par un étudiant : sur place, à emporter, ou réservation
 * anticipée. Le montant total et les lignes sont calculés au moment de la
 * création, après vérification auprès du service "Responsabilité".
 */
@Entity
@Table(name = "commande")
public class Commande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "etudiant_id", nullable = false)
    private Long etudiantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeCommande type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutCommande statut = StatutCommande.EN_ATTENTE;

    @Column(name = "date_commande", nullable = false)
    private LocalDateTime dateCommande = LocalDateTime.now();

    @Column(name = "montant_total")
    private Double montantTotal = 0.0;

    @Column(nullable = false)
    private Boolean payee = false;

    @OneToMany(mappedBy = "commande", cascade = CascadeType.ALL, orphanRemoval = true,
               fetch = FetchType.EAGER)
    // fetch = EAGER (au lieu du LAZY par défaut) : indispensable ici car
    // l'EntityManager est fermé (finally { em.close(); }) AVANT que
    // RESTEasy/JSON-B ne sérialise la réponse. Une collection LAZY encore
    // non initialisée à ce moment provoque un LazyInitializationException,
    // que RESTEasy remonte comme "Unable to serialize property 'lignes'".
    private List<LigneCommande> lignes;

    public Commande() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEtudiantId() { return etudiantId; }
    public void setEtudiantId(Long etudiantId) { this.etudiantId = etudiantId; }

    public TypeCommande getType() { return type; }
    public void setType(TypeCommande type) { this.type = type; }

    public StatutCommande getStatut() { return statut; }
    public void setStatut(StatutCommande statut) { this.statut = statut; }

    public LocalDateTime getDateCommande() { return dateCommande; }
    public void setDateCommande(LocalDateTime dateCommande) { this.dateCommande = dateCommande; }

    public Double getMontantTotal() { return montantTotal; }
    public void setMontantTotal(Double montantTotal) { this.montantTotal = montantTotal; }

    public Boolean getPayee() { return payee; }
    public void setPayee(Boolean payee) { this.payee = payee; }

    public List<LigneCommande> getLignes() { return lignes; }
    public void setLignes(List<LigneCommande> lignes) { this.lignes = lignes; }
}
