package ma.ump.responsabilite.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

/**
 * Fournit un EntityManager pour chaque appel REST.
 * On utilise une persistence-unit RESOURCE_LOCAL (pas de datasource JNDI
 * côté WildFly à configurer) : plus simple à containeriser pour un projet
 * pédagogique. L'EntityManagerFactory est créée une seule fois (coûteux),
 * réutilisée pour toute la durée de vie de l'application.
 */
public class JPAUtil {

    private static final EntityManagerFactory EMF =
            Persistence.createEntityManagerFactory("responsabilitePU");

    private JPAUtil() {}

    public static EntityManager getEntityManager() {
        return EMF.createEntityManager();
    }
}
