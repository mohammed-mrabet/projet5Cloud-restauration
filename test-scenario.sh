#!/usr/bin/env bash
# =========================================================
# test-scenario.sh
# =========================================================
# Scénario de test de bout en bout pour la Phase 2 (compose.yaml maître).
# Démontre les 2 communications REST internes exigées par le sujet :
#   1. commandes-service  -> responsabilite-service (vérif. dispo/prix)
#   2. commandes-service  -> paiement-service        (traitement paiement)
#
# Pré-requis : le compose.yaml maître doit être démarré :
#   docker compose up --build -d
#
# Utilisation :
#   chmod +x test-scenario.sh
#   ./test-scenario.sh
#
# Nécessite "jq" pour un affichage lisible (sinon le JSON brut est affiché).

set -e

RESPO="http://localhost:8081/responsabilite-service/api"
CMD="http://localhost:8082/commandes-service/api"
PAY="http://localhost:8083/paiement-service/api"

has_jq() { command -v jq >/dev/null 2>&1; }
pp() { if has_jq; then jq .; else cat; fi; }

echo "=========================================================="
echo "1. Création d'un régime spécial (végétarien) - service Responsabilité"
echo "=========================================================="
REGIME=$(curl -s -X POST "$RESPO/regimes" \
  -H "Content-Type: application/json" \
  -d '{"nom":"VEGETARIEN","description":"Sans viande ni poisson"}')
echo "$REGIME" | pp

echo ""
echo "=========================================================="
echo "2. Création de 2 plats - service Responsabilité"
echo "=========================================================="
PLAT1=$(curl -s -X POST "$RESPO/plats" \
  -H "Content-Type: application/json" \
  -d '{"nom":"Tajine de poulet","description":"Tajine traditionnel","categorie":"PLAT_PRINCIPAL","prix":25,"disponible":true}')
echo "$PLAT1" | pp
PLAT1_ID=$(echo "$PLAT1" | (has_jq && jq -r '.id' || grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*'))

PLAT2=$(curl -s -X POST "$RESPO/plats" \
  -H "Content-Type: application/json" \
  -d '{"nom":"Salade composée","description":"Salade fraîche du jour","categorie":"ENTREE","prix":10,"disponible":true}')
echo "$PLAT2" | pp
PLAT2_ID=$(echo "$PLAT2" | (has_jq && jq -r '.id' || grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*'))

echo ""
echo "Plat 1 (Tajine) id=$PLAT1_ID ; Plat 2 (Salade) id=$PLAT2_ID"

echo ""
echo "=========================================================="
echo "3. Vérification directe de la disponibilité (endpoint interne)"
echo "=========================================================="
curl -s "$RESPO/plats/$PLAT1_ID/disponibilite" | pp

echo ""
echo "=========================================================="
echo "4. Création d'une commande - service Commandes"
echo "   (déclenche en interne 2 appels GET vers responsabilite-service)"
echo "=========================================================="
COMMANDE=$(curl -s -X POST "$CMD/commandes" \
  -H "Content-Type: application/json" \
  -d "{\"etudiantId\":1,\"type\":\"SUR_PLACE\",\"lignes\":[{\"platId\":$PLAT1_ID,\"quantite\":1},{\"platId\":$PLAT2_ID,\"quantite\":2}]}")
echo "$COMMANDE" | pp
COMMANDE_ID=$(echo "$COMMANDE" | (has_jq && jq -r '.id' || grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*'))
echo ""
echo "-> Commande créée id=$COMMANDE_ID, montant attendu = 25 + 2*10 = 45"

echo ""
echo "=========================================================="
echo "5. Test du garde-fou : commande sur un plat inexistant (doit échouer en 409)"
echo "=========================================================="
curl -s -o /dev/null -w "Code HTTP retourné : %{http_code} (409 attendu)\n" \
  -X POST "$CMD/commandes" \
  -H "Content-Type: application/json" \
  -d '{"etudiantId":1,"type":"SUR_PLACE","lignes":[{"platId":999999,"quantite":1}]}'

echo ""
echo "=========================================================="
echo "6. Paiement de la commande - déclenche l'appel interne vers paiement-service"
echo "=========================================================="
PAIEMENT=$(curl -s -X POST "$CMD/commandes/$COMMANDE_ID/payer" \
  -H "Content-Type: application/json" \
  -d '{"modePaiement":"CARTE"}')
echo "$PAIEMENT" | pp

echo ""
echo "=========================================================="
echo "7. Vérification de la facture générée automatiquement - service Paiement"
echo "=========================================================="
curl -s "$PAY/factures" | pp

echo ""
echo "=========================================================="
echo "Scénario terminé avec succès."
echo "=========================================================="
