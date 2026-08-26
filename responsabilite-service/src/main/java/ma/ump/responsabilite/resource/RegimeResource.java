package ma.ump.responsabilite.resource;

import jakarta.persistence.EntityManager;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ma.ump.responsabilite.entity.RegimeSpecial;
import ma.ump.responsabilite.util.JPAUtil;

import java.util.List;

@Path("/regimes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RegimeResource {

    @GET
    public List<RegimeSpecial> lister() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery("SELECT r FROM RegimeSpecial r", RegimeSpecial.class).getResultList();
        } finally {
            em.close();
        }
    }

    @POST
    public Response creer(RegimeSpecial regime) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(regime);
            em.getTransaction().commit();
            return Response.status(Response.Status.CREATED).entity(regime).build();
        } finally {
            em.close();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response supprimer(@PathParam("id") Long id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            RegimeSpecial r = em.find(RegimeSpecial.class, id);
            if (r == null) return Response.status(Response.Status.NOT_FOUND).build();
            em.getTransaction().begin();
            em.remove(r);
            em.getTransaction().commit();
            return Response.noContent().build();
        } finally {
            em.close();
        }
    }
}
