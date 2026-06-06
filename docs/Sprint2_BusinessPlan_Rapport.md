# RAPPORT BUSINESS PLAN - SPRINT 2 - MeetCare

## 1. Introduction
Dans le cadre du Sprint 2, notre équipe a souhaité aller au-delà du développement technique de **MeetCare** et formaliser une vision produit plus complète. Ce business plan a pour but de montrer que le projet peut être présenté non seulement comme une application académique, mais aussi comme une solution crédible répondant à un besoin réel.

MeetCare est une application de bureau développée en **Java / JavaFX**, connectée à une base de données **MySQL**, qui vise à améliorer l’organisation des interactions entre patients, médecins et administrateurs. Après un premier sprint centré sur la mise en place du MVP, le Sprint 2 a permis d’élargir fortement le périmètre fonctionnel et de renforcer la cohérence globale du produit.

Ce rapport business plan présente le positionnement de MeetCare, le besoin auquel il répond, les cibles visées, la proposition de valeur, la logique économique du produit et les perspectives d’évolution.

## 2. Présentation du projet
MeetCare est une plateforme médicale de gestion et de suivi qui permet :
- au patient de rechercher un médecin, réserver un rendez-vous, suivre ses documents et ses factures, et communiquer plus facilement ;
- au médecin de gérer ses disponibilités, son agenda, ses patients, ses documents et sa facturation ;
- à l’administrateur de superviser l’activité globale de la plateforme.

L’idée du projet est de proposer une solution centralisée inspirée des usages actuels de plateformes comme Doctolib, tout en y ajoutant une logique de suivi, de gestion documentaire et de supervision interne.

## 3. Problème identifié
Le parcours médical comporte encore plusieurs difficultés récurrentes :
- la prise de rendez-vous peut être longue ou peu lisible ;
- les patients manquent souvent de visibilité sur leurs documents et leurs informations de suivi ;
- les médecins ont besoin d’outils simples pour gérer les consultations, les créneaux et les échanges ;
- les structures ont besoin d’une vue globale sur les utilisateurs, les rendez-vous et les activités.

Ces difficultés montrent qu’il existe un besoin de centralisation, de lisibilité et de fluidité dans la gestion du parcours patient-médecin.

## 4. Solution proposée
MeetCare répond à ce besoin par une plateforme multi-profils organisée autour de trois espaces principaux.

### 4.1 Espace patient
L’espace patient permet :
- la recherche de médecins ;
- la consultation de profils ;
- la prise de rendez-vous ;
- la gestion des rendez-vous ;
- la consultation de documents médicaux ;
- la consultation des factures ;
- l’accès à une messagerie ;
- l’usage d’un assistant IA.

### 4.2 Espace médecin
L’espace médecin permet :
- la gestion des disponibilités ;
- l’utilisation d’un agenda ;
- la gestion du portefeuille patient ;
- l’envoi de documents ;
- la création de factures ;
- la consultation de statistiques ;
- l’édition d’un profil public ;
- l’accès à la messagerie et à l’assistant IA.

### 4.3 Espace administrateur
L’espace administrateur permet :
- le suivi des utilisateurs ;
- le suivi des médecins ;
- la supervision des rendez-vous ;
- le suivi des documents ;
- le suivi des factures ;
- la visualisation de statistiques ;
- la gestion globale de l’activité.

## 5. Proposition de valeur
La proposition de valeur de MeetCare repose sur quatre idées fortes :

### 5.1 Centralisation
MeetCare regroupe dans un même outil les rendez-vous, les documents, les factures, les messages et les disponibilités.

### 5.2 Simplicité
L’application cherche à proposer une interface claire et structurée, avec des espaces distincts selon les rôles des utilisateurs.

### 5.3 Organisation
Le produit offre une meilleure visibilité pour chaque acteur :
- le patient suit son parcours ;
- le médecin organise son activité ;
- l’administrateur supervise la plateforme.

### 5.4 Crédibilité métier
En intégrant la logique des disponibilités, des rendez-vous, des documents et de la facturation, MeetCare se rapproche d’un produit médical réaliste.

En une phrase, la proposition de valeur est la suivante :

**MeetCare simplifie la gestion du parcours médical en reliant patients, médecins et administration sur une plateforme unique, organisée et cohérente.**

## 6. Étude de marché simplifiée
Le besoin auquel répond MeetCare existe déjà dans le marché des solutions de santé numérique. Des plateformes comme **Doctolib**, **Maiia** ou certains outils métiers pour cabinets montrent que les professionnels de santé et les patients recherchent :
- des outils de réservation efficaces ;
- un suivi mieux structuré ;
- des échanges plus fluides ;
- une interface simple d’utilisation.

MeetCare ne prétend pas concurrencer immédiatement ces solutions à grande échelle. En revanche, dans la logique du projet, il se positionne comme une solution crédible, cohérente et démontrable, capable d’illustrer une vraie réflexion produit.

## 7. Clients cibles
Les clients cibles de MeetCare peuvent être distingués en trois catégories.

### 7.1 Patients
Les patients constituent la base utilisateur de la plateforme. Ils utilisent l’application gratuitement pour :
- trouver un médecin ;
- réserver un créneau ;
- consulter leurs informations utiles.

### 7.2 Médecins
Les médecins représentent la cible économique principale. Ils utilisent la plateforme pour :
- gérer leurs disponibilités ;
- suivre leurs consultations ;
- envoyer des documents ;
- facturer ;
- communiquer avec leurs patients.

### 7.3 Structures ou gestionnaires
Des structures comme des cabinets ou centres médicaux peuvent aussi être intéressés par un espace de supervision, d’où la présence de l’espace administrateur.

## 8. Modèle économique
Dans une logique business, le modèle économique le plus cohérent pour MeetCare serait :

### Gratuit pour les patients
Le patient accède gratuitement au service afin de favoriser l’adoption et la fréquentation de la plateforme.

### Abonnement pour les médecins
Les médecins paient un abonnement mensuel leur donnant accès à :
- l’agenda ;
- la gestion des disponibilités ;
- les dossiers patients ;
- la messagerie ;
- les documents ;
- la facturation ;
- les statistiques.

### Exemple de tarification
- Formule standard : **39 €/mois**
- Formule avancée : **79 €/mois**

Une autre possibilité serait de proposer une licence pour une structure médicale, mais pour la soutenance, le modèle **gratuit patient + abonnement médecin** reste le plus simple à défendre.

## 9. Ressources nécessaires
Le développement et la pérennité de MeetCare supposent plusieurs ressources.

### Ressources humaines
- Product Owner ;
- Scrum Master ;
- développeurs ;
- responsable technique / lead developer.

### Ressources techniques
- application Java / JavaFX ;
- base de données MySQL ;
- dépôt GitHub ;
- architecture DAO / contrôleurs / vues ;
- documentation projet.

### Ressources organisationnelles
- backlog ;
- gestion des sprints ;
- répartition des rôles ;
- suivi budgétaire.

## 10. Coûts du projet
Dans le cadre pédagogique, un budget fictif a été défini pour mesurer la valeur du travail produit.

### Coût Sprint 1
- 23 000 €

### Coût Sprint 2
| Rôle | Jours | Tarif journalier | Coût |
|---|---:|---:|---:|
| Product Owner | 3 | 1000 € | 3000 € |
| Scrum Master | 10 | 800 € | 8000 € |
| Lead Developer | 10 | 550 € | 5500 € |
| Développeur | 10 | 550 € | 5500 € |

**Total Sprint 2 : 22 000 €**

### Coût cumulé
- Budget initial : 100 000 €
- Sprint 1 : 23 000 €
- Sprint 2 : 22 000 €
- Total consommé : 45 000 €
- Budget restant estimé : 55 000 €

## 11. Avancement produit après Sprint 2
À la fin du Sprint 2, MeetCare présente déjà un niveau d’avancement significatif.

Les acquis majeurs sont :
- un espace patient largement fonctionnel ;
- un espace médecin riche en fonctionnalités ;
- un espace administrateur structuré ;
- une base de données plus complète ;
- une gestion des disponibilités et rendez-vous plus réaliste ;
- l’ajout de la messagerie, de l’assistant IA, des documents et de la facturation.

Le produit est encore perfectible, mais il a acquis une vraie cohérence d’ensemble.

## 12. Risques identifiés
Comme tout projet, MeetCare présente plusieurs risques :
- complexité croissante du produit ;
- difficulté d’intégration des branches en équipe ;
- erreurs techniques après fusion ;
- manque de tests automatisés ;
- dette technique potentielle si la stabilisation est repoussée.

Ces risques restent maîtrisables à condition de prioriser la stabilisation et la qualité dans le Sprint 3.

## 13. Perspectives
Les perspectives pour la suite du projet sont claires :
- finaliser les fonctionnalités déjà entamées ;
- améliorer la robustesse de la messagerie ;
- renforcer la stabilité générale du produit ;
- améliorer l’expérience utilisateur ;
- développer davantage les tests ;
- préparer une soutenance convaincante autour d’un produit crédible.

## 14. Conclusion
Le Sprint 2 marque une étape importante dans la transformation de MeetCare.

Le projet est passé :
- d’un MVP principalement orienté patient ;
- à une plateforme multi-profils plus riche et plus proche d’un produit réel.

Le business plan montre que MeetCare peut être défendu comme un projet crédible car :
- le besoin est réel ;
- les cibles sont claires ;
- la proposition de valeur est cohérente ;
- le modèle économique est défendable ;
- le coût du projet reste maîtrisé dans le cadre fixé.

En résumé, MeetCare constitue désormais une base sérieuse pour la poursuite du développement au Sprint 3 et pour une présentation produit convaincante.
