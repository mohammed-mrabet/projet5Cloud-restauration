package ma.ump.paiement.resource;

import jakarta.persistence.EntityManager;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ma.ump.paiement.dto.PaiementRequestDTO;
import ma.ump.paiement.entity.*;
import ma.ump.paiement.util.JPAUtil;

import java.util.List;
import java.util.UUID;

@Path("/paiements")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PaiementResource {

    @GET
    public List<Paiement> lister() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery("SELECT p FROM Paiement p", Paiement.class).getResultList();
        } finally {
            em.close();
        }
    }

    @GET
    @Path("/{id}")
    public Response trouver(@PathParam("id") Long id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Paiement p = em.find(Paiement.class, id);
            if (p == null) return Response.status(Response.Status.NOT_FOUND).build();
            return Response.ok(p).build();
        } finally {
            em.close();
        }
    }

    @GET
    @Path("/commande/{commandeId}")
    public List<Paiement> parCommande(@PathParam("commandeId") Long commandeId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT p FROM Paiement p WHERE p.commandeId = :cid", Paiement.class)
                    .setParameter("cid", commandeId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Traite un paiement (appelé en interne par le service "Commandes").
     * Le traitement est ici SIMULÉ (toujours accepté) : en production, on
     * appellerait un vrai prestataire de paiement (PSP) externe à cet endroit.
     * Une facture est générée automatiquement dès que le paiement est validé.
     */
    @POST
    public Response creer(PaiementRequestDTO requete) {
        Paiement paiement = new Paiement();
        paiement.setCommandeId(requete.getCommandeId());
        paiement.setMontant(requete.getMontant());
        paiement.setModePaiement(requete.getModePaiement());
        paiement.setStatut(StatutPaiement.VALIDE); // simulation : toujours accepté

        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(paiement);

            Facture facture = new Facture();
            facture.setPaiementId(paiement.getId());
            facture.setNumeroFacture("FAC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            facture.setMontantTTC(paiement.getMontant());
            em.persist(facture);

            em.getTransaction().commit();
            return Response.status(Response.Status.CREATED).entity(paiement).build();
        } finally {
            em.close();
        }
    }
}
