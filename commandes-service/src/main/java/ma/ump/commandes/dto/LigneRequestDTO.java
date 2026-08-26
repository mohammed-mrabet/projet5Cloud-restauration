package ma.ump.commandes.dto;

/** Une ligne du panier envoyée par le client lors de la création d'une commande. */
public class LigneRequestDTO {
    private Long platId;
    private Integer quantite;

    public Long getPlatId() { return platId; }
    public void setPlatId(Long platId) { this.platId = platId; }

    public Integer getQuantite() { return quantite; }
    public void setQuantite(Integer quantite) { this.quantite = quantite; }
}
