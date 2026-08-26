# --- Etape 1 : compilation du projet avec Maven ---
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# On copie d'abord juste le pom.xml pour profiter du cache Docker :
# si le pom.xml ne change pas, Maven ne retélécharge pas les dépendances.
COPY pom.xml .
RUN mvn -B dependency:go-offline

# On copie le code source et on build le .war (les tests sont sautés).
COPY src ./src
RUN mvn -B clean package -DskipTests

# --- Etape 2 : image finale avec juste WildFly + notre .war ---
FROM quay.io/wildfly/wildfly:31.0.1.Final-jdk17

# Récupère le driver MySQL déjà téléchargé par Maven pendant l'étape "build".
COPY --from=build /root/.m2/repository/com/mysql/mysql-connector-j/8.3.0/mysql-connector-j-8.3.0.jar /tmp/mysql-connector-j.jar
COPY configure-mysql.cli /tmp/configure-mysql.cli

# Configure la datasource MySQL "PaiementDS" (mode hors-ligne, standalone.xml
# édité directement). Indispensable : sans ça WildFly utilise sa base H2
# de démonstration au lieu de MySQL (voir explication dans le README).
RUN /opt/jboss/wildfly/bin/jboss-cli.sh --file=/tmp/configure-mysql.cli

# On copie le .war généré à l'étape 1 dans le dossier de déploiement de WildFly.
COPY --from=build /app/target/paiement-service.war \
     /opt/jboss/wildfly/standalone/deployments/

# Port sur lequel WildFly écoute.
EXPOSE 8080

# -b 0.0.0.0 : nécessaire pour que WildFly accepte les connexions venant
# d'autres conteneurs (par défaut il n'écoute que sur localhost).
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0"]
