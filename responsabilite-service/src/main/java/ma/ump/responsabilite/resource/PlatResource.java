package ma.ump.responsabilite.resource;

import jakarta.persistence.EntityManager;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ma.ump.responsabilite.dto.DisponibiliteDTO;
import ma.ump.responsabilite.entity.Plat;
import ma.ump.responsabilite.util.JPAUtil;

import java.util.List;

/**
 * CRUD sur les plats + endpoint clé /disponibilite, interrogé en interne
 * par le service "Commandes" (appel REST Backend-to-Backend, cf. consigne
 * Phase 2 - Projet 5 : "Le Service Commandes peut interroger le Catalogue
 * pour vérifier la disponibilité et les prix").
 */
@Path("/plats")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PlatResource {

    @GET
    public List<Plat> lister() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery("SELECT p FROM Plat p", Plat.class).getResultList();
        } finally {
            em.close();
        }
    }

    @GET
    @Path("/{id}")
    public Response trouver(@PathParam("id") Long id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Plat p = em.find(Plat.class, id);
            if (p == null) return Response.status(Response.Status.NOT_FOUND).build();
            return Response.ok(p).build();
        } finally {
            em.close();
        }
    }

    /**
     * Endpoint consommé par le service "Commandes" lors de la création d'un panier.
     * Renvoie 404 si le plat n'existe pas, sinon son prix et sa disponibilité actuelle.
     */
    @GET
    @Path("/{id}/disponibilite")
    public Response verifierDisponibilite(@PathParam("id") Long id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Plat plat = em.find(Plat.class, id);
            if (plat == null) return Response.status(Response.Status.NOT_FOUND).build();
            DisponibiliteDTO dto = new DisponibiliteDTO(
                    plat.getId(), plat.getNom(), plat.getPrix(), plat.getDisponible());
            return Response.ok(dto).build();
        } finally {
            em.close();
        }
    }

    @POST
    public Response creer(Plat plat) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(plat);
            em.getTransaction().commit();
            return Response.status(Response.Status.CREATED).entity(plat).build();
        } finally {
            em.close();
        }
    }

    @PUT
    @Path("/{id}")
    public Response modifier(@PathParam("id") Long id, Plat donnees) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Plat p = em.find(Plat.class, id);
            if (p == null) return Response.status(Response.Status.NOT_FOUND).build();
            em.getTransaction().begin();
            p.setNom(donnees.getNom());
            p.setDescription(donnees.getDescription());
            p.setCategorie(donnees.getCategorie());
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
            Plat p = em.find(Plat.class, id);
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
