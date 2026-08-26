package ma.ump.commandes.client;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ma.ump.commandes.dto.PaiementResultDTO;

import java.util.HashMap;
import java.util.Map;

/** Appel REST Backend-to-Backend vers le service "Paiement". */
public class PaiementClient {

    private static final String BASE_URL =
            "http://paiement-service:8080/paiement-service/api";

    public static PaiementResultDTO creerPaiement(Long commandeId, Double montant, String modePaiement) {
        Client client = ClientBuilder.newClient();
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("commandeId", commandeId);
            body.put("montant", montant);
            body.put("modePaiement", modePaiement);

            WebTarget target = client.target(BASE_URL).path("/paiements");
            Response response = target.request()
                    .post(Entity.entity(body, MediaType.APPLICATION_JSON));

            if (response.getStatus() != 201) {
                return null;
            }
            return response.readEntity(PaiementResultDTO.class);
        } catch (Exception e) {
            return null;
        } finally {
            client.close();
        }
    }
}
