package ma.ump.responsabilite.resource;

import jakarta.persistence.EntityManager;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ma.ump.responsabilite.entity.Produit;
import ma.ump.responsabilite.util.JPAUtil;

import java.util.List;

/**
 * CRUD basique sur les produits de base entrant dans la composition des plats.
 */
@Path("/produits")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProduitResource {

    @GET
    public List<Produit> lister() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery("SELECT p FROM Produit p", Produit.class).getResultList();
        } finally {
            em.close();
        }
    }

    @GET
    @Path("/{id}")
    public Response trouver(@PathParam("id") Long id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Produit p = em.find(Produit.class, id);
            if (p == null) return Response.status(Response.Status.NOT_FOUND).build();
            return Response.ok(p).build();
        } finally {
            em.close();
        }
    }

    @POST
    public Response creer(Produit produit) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(produit);
            em.getTransaction().commit();
            return Response.status(Response.Status.CREATED).entity(produit).build();
        } finally {
            em.close();
        }
    }

    @PUT
    @Path("/{id}")
    public Response modifier(@PathParam("id") Long id, Produit donnees) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Produit p = em.find(Produit.class, id);
            if (p == null) return Response.status(Response.Status.NOT_FOUND).build();
            em.getTransaction().begin();
            p.setNom(donnees.getNom());
            p.setDescription(donnees.getDescription());
            p.setPrix(donnees.getPrix());
            p.setDisponible(donnees.getDisponible());
            em.getTransaction().commit();
            return Response.ok(p).build();
        } finally {
            em.close();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response supprimer(@PathParam("id") Long id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Produit p = em.find(Produit.class, id);
            if (p == null) return Response.status(Response.Status.NOT_FOUND).build();
            em.getTransaction().begin();
            em.remove(p);
            em.getTransaction().commit();
            return Response.noContent().build();
        } finally {
            em.close();
        }
    }
}
