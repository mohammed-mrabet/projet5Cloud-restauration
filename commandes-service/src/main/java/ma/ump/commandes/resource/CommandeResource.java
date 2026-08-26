package ma.ump.commandes.resource;

import jakarta.persistence.EntityManager;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ma.ump.commandes.client.PaiementClient;
import ma.ump.commandes.client.ResponsabiliteClient;
import ma.ump.commandes.dto.*;
import ma.ump.commandes.entity.*;
import ma.ump.commandes.util.JPAUtil;

import java.util.ArrayList;
import java.util.List;

@Path("/commandes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CommandeResource {

    @GET
    public List<Commande> lister() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery("SELECT c FROM Commande c", Commande.class).getResultList();
        } finally {
            em.close();
        }
    }

    @GET
    @Path("/{id}")
    public Response trouver(@PathParam("id") Long id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Commande c = em.find(Commande.class, id);
            if (c == null) return Response.status(Response.Status.NOT_FOUND).build();
            return Response.ok(c).build();
        } finally {
            em.close();
        }
    }

    /**
     * Crée une commande après avoir vérifié CHAQUE ligne auprès du service
     * "Responsabilité" (appel REST interne). C'est l'enjeu Docker du Projet 5 :
     * "Le Service Commandes peut interroger le Catalogue pour vérifier la
     * disponibilité et les prix au moment de la création du panier."
     */
    @POST
    public Response creer(CommandeRequestDTO requete) {
        List<LigneCommande> lignes = new ArrayList<>();
        double total = 0.0;

        for (LigneRequestDTO ligneReq : requete.getLignes()) {
            DisponibiliteDTO dispo = ResponsabiliteClient.verifierPlat(ligneReq.getPlatId());

            if (dispo == null || !Boolean.TRUE.equals(dispo.getDisponible())) {
                return Response.status(Response.Status.CONFLICT)
                        .entity("Le plat " + ligneReq.getPlatId() + " est indisponible ou introuvable.")
                        .build();
            }

            LigneCommande ligne = new LigneCommande();
            ligne.setPlatId(dispo.getPlatId());
            ligne.setNomPlat(dispo.getNom());
            ligne.setPrixUnitaire(dispo.getPrix());
            ligne.setQuantite(ligneReq.getQuantite());
            lignes.add(ligne);

            total += dispo.getPrix() * ligneReq.getQuantite();
        }

        Commande commande = new Commande();
        commande.setEtudiantId(requete.getEtudiantId());
        commande.setType(requete.getType());
        commande.setMontantTotal(total);

        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(commande);
            for (LigneCommande ligne : lignes) {
                ligne.setCommande(commande);
                em.persist(ligne);
            }
            em.getTransaction().commit();
            commande.setLignes(lignes);
            return Response.status(Response.Status.CREATED).entity(commande).build();
        } finally {
            em.close();
        }
    }

    @PUT
    @Path("/{id}/statut")
    public Response changerStatut(@PathParam("id") Long id, StatutDTO statutDTO) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Commande c = em.find(Commande.class, id);
            if (c == null) return Response.status(Response.Status.NOT_FOUND).build();
            em.getTransaction().begin();
            c.setStatut(statutDTO.getStatut());
            em.getTransaction().commit();
            return Response.ok(c).build();
        } finally {
            em.close();
        }
    }

    /**
     * Déclenche le paiement de la commande en appelant le service "Paiement"
     * (deuxième communication REST interne du Projet 5).
     */
    @POST
    @Path("/{id}/payer")
    public Response payer(@PathParam("id") Long id, PaiementRequestDTO paiementReq) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Commande c = em.find(Commande.class, id);
            if (c == null) return Response.status(Response.Status.NOT_FOUND).build();
            if (Boolean.TRUE.equals(c.getPayee())) {
                return Response.status(Response.Status.CONFLICT).entity("Commande déjà payée.").build();
            }

            PaiementResultDTO resultat = PaiementClient.creerPaiement(
                    c.getId(), c.getMontantTotal(), paiementReq.getModePaiement());

            if (resultat == null || !"VALIDE".equals(resultat.getStatut())) {
                return Response.status(Response.Status.BAD_GATEWAY)
                        .entity("Le paiement a échoué.").build();
            }

            em.getTransaction().begin();
            c.setPayee(true);
            em.getTransaction().commit();
            return Response.ok(c).build();
        } finally {
            em.close();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response annuler(@PathParam("id") Long id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Commande c = em.find(Commande.class, id);
            if (c == null) return Response.status(Response.Status.NOT_FOUND).build();
            em.getTransaction().begin();
            c.setStatut(StatutCommande.ANNULEE);
            em.getTransaction().commit();
            return Response.ok(c).build();
        } finally {
            em.close();
        }
    }
}
