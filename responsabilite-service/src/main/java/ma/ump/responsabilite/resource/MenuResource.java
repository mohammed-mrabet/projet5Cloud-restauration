package ma.ump.responsabilite.resource;

import jakarta.persistence.EntityManager;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ma.ump.responsabilite.entity.Menu;
import ma.ump.responsabilite.util.JPAUtil;

import java.time.LocalDate;
import java.util.List;

@Path("/menus")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MenuResource {

    @GET
    public List<Menu> lister() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery("SELECT m FROM Menu m", Menu.class).getResultList();
        } finally {
            em.close();
        }
    }

    @GET
    @Path("/{id}")
    public Response trouver(@PathParam("id") Long id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Menu m = em.find(Menu.class, id);
            if (m == null) return Response.status(Response.Status.NOT_FOUND).build();
            return Response.ok(m).build();
        } finally {
            em.close();
        }
    }

    /** Planification du menu d'un restaurant pour une date donnée (format ISO yyyy-MM-dd). */
    @GET
    @Path("/jour/{date}/restaurant/{nom}")
    public List<Menu> parJourEtRestaurant(@PathParam("date") String date,
                                           @PathParam("nom") String nom) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT m FROM Menu m WHERE m.dateMenu = :d AND m.restaurant = :r", Menu.class)
                    .setParameter("d", LocalDate.parse(date))
                    .setParameter("r", nom)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    @POST
    public Response creer(Menu menu) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(menu);
            em.getTransaction().commit();
            return Response.status(Response.Status.CREATED).entity(menu).build();
        } finally {
            em.close();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response supprimer(@PathParam("id") Long id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Menu m = em.find(Menu.class, id);
            if (m == null) return Response.status(Response.Status.NOT_FOUND).build();
            em.getTransaction().begin();
            em.remove(m);
            em.getTransaction().commit();
            return Response.noContent().build();
        } finally {
            em.close();
        }
    }
}
