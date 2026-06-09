# MeetCare

Application JavaFX Maven pour la gestion de la plateforme MeetCare.

## Prérequis

- JDK 20 ou plus récent
- Maven, ou le Maven Wrapper fourni dans le dépôt
- Git
- Une base MySQL/MariaDB pour lancer l'application complète

## Télécharger depuis GitHub

```bash
git clone https://github.com/<ton-compte>/<ton-repo>.git
cd MeetCare
```

Si tu utilises un zip GitHub, décompresse-le puis ouvre le dossier du projet.

## Configuration de la base de données

L'application lit ses paramètres via un fichier `.env` ou des propriétés système.

Exemple minimal de `.env` :

```env
MEETCARE_DB_URL=jdbc:mysql://localhost:3306/meetcare
MEETCARE_DB_USER=root
MEETCARE_DB_PASSWORD=
```

## Compiler le projet

Avec le wrapper :

```bash
./mvnw clean compile
```

Sous Windows :

```bat
mvnw.cmd clean compile
```

Avec Maven installé localement :

```bash
mvn clean compile
```

## Lancer l'application

```bash
./mvnw javafx:run
```

Sous Windows :

```bat
mvnw.cmd javafx:run
```

## Lancer les tests

Les tests unitaires et d'intégration utilisent une base H2 en mémoire, donc ils ne dépendent pas de MySQL.

```bash
./mvnw test
```

Sous Windows :

```bat
mvnw.cmd test
```

## Ce que couvrent les tests

- utilitaires de mot de passe
- conversions d'énums métier
- helpers purs des contrôleurs
- DAO principaux sur une base H2 de test
- services métier autour des inscriptions et factures
- logique des tableaux de bord admin

## Notes


- Le wrapper livré ici retombe sur `mvn` si le jar officiel du wrapper n'est pas présent.
- Si tu veux le comportement Maven Wrapper complet, il suffit d'ajouter plus tard `./mvnw -N wrapper:wrapper` dans un environnement avec accès réseau.
