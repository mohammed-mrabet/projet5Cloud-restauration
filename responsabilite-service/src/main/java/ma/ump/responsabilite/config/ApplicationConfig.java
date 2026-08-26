package ma.ump.responsabilite.config;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * Active JAX-RS sur ce module et fixe le préfixe commun de toutes les routes :
 * toutes les API de ce service sont donc accessibles sous /responsabilite-service/api/...
 */
@ApplicationPath("/api")
public class ApplicationConfig extends Application {
}
