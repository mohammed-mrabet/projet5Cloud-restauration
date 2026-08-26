package ma.ump.paiement.resource;

import jakarta.persistence.EntityManager;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ma.ump.paiement.entity.Facture;
import ma.ump.paiement.util.JPAUtil;

import java.util.List;

@Path("/factures")
@Produces(MediaType.APPLICATION_JSON)
public class FactureResource {

    @GET
    public List<Facture> lister() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery("SELECT f FROM Facture f", Facture.class).getResultList();
        } finally {
            em.close();
        }
    }

    @GET
    @Path("/{id}")
    public Response trouver(@PathParam("id") Long id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Facture f = em.find(Facture.class, id);
            if (f == null) return Response.status(Response.Status.NOT_FOUND).build();
            return Response.ok(f).build();
        } finally {
            em.close();
        }
    }
}
