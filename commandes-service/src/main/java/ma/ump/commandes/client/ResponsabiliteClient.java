package ma.ump.commandes.client;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import ma.ump.commandes.dto.DisponibiliteDTO;

/**
 * Appel REST Backend-to-Backend vers le service "Responsabilité",
 * via le nom de service Docker/Kubernetes (résolution DNS interne au réseau).
 * Aucune dépendance Maven supplémentaire : l'API Client de JAX-RS fait partie
 * de Jakarta EE et est fournie par WildFly au runtime.
 */
public class ResponsabiliteClient {

    // "responsabilite-service" est résolu par Docker (réseau "restauration-net")
    // ou par Kubernetes (Service du même nom) - le code ne change pas entre les deux !
    private static final String BASE_URL =
            "http://responsabilite-service:8080/responsabilite-service/api";

    /**
     * Interroge le catalogue pour vérifier qu'un plat existe et récupérer son prix.
     * @return null si le plat est introuvable (404) ou si le service est inaccessible.
     */
    public static DisponibiliteDTO verifierPlat(Long platId) {
        Client client = ClientBuilder.newClient();
        try {
            WebTarget target = client.target(BASE_URL).path("/plats/" + platId + "/disponibilite");
            Response response = target.request().get();
            if (response.getStatus() != 200) {
                return null;
            }
            return response.readEntity(DisponibiliteDTO.class);
        } catch (Exception e) {
            // En production : logger l'erreur (service indisponible, timeout, ...)
            return null;
        } finally {
            client.close();
        }
    }
}
