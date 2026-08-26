package ma.ump.paiement.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class JPAUtil {

    private static final EntityManagerFactory EMF =
            Persistence.createEntityManagerFactory("paiementPU");

    private JPAUtil() {}

    public static EntityManager getEntityManager() {
        return EMF.createEntityManager();
    }
}
