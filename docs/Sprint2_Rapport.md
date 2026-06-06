# RAPPORT SPRINT 2 - MeetCare

## 1. Présentation du sprint
Le Sprint 2 de **MeetCare** avait pour objectif de transformer le MVP du Sprint 1 en une application beaucoup plus complète, en ajoutant une vraie logique métier, une connexion renforcée à la base de données et des espaces différenciés pour les trois profils de la plateforme : **patient**, **médecin** et **administrateur**.

Après un Sprint 1 centré surtout sur l’interface patient, l’équipe s’est concentrée sur :
- l’extension fonctionnelle de l’application ;
- la mise en place d’une logique plus réaliste de gestion des rendez-vous ;
- l’ajout d’une couche base de données plus riche ;
- l’ouverture du produit à l’espace médecin et à l’espace administrateur ;
- l’ajout de fonctionnalités comme la messagerie, les documents, la facturation et les disponibilités.

## 2. Objectifs du Sprint 2
Les objectifs principaux du Sprint 2 étaient :
- rendre l’espace patient réellement exploitable ;
- développer l’espace médecin avec ses fonctionnalités métier ;
- développer un espace administrateur de supervision ;
- enrichir la base de données pour supporter les nouveaux flux ;
- mettre en place la gestion des disponibilités et des rendez-vous ;
- connecter les documents, la facturation et la messagerie à une logique cohérente.

## 3. Travaux réalisés

### 3.1 Espace patient
Pendant ce sprint, l’espace patient a été fortement enrichi.

Fonctionnalités mises en place :
- tableau de bord patient dynamique ;
- recherche de médecins ;
- affichage des profils médecins ;
- réservation de rendez-vous selon les disponibilités ;
- consultation et gestion des rendez-vous ;
- accès aux documents médicaux ;
- accès aux factures ;
- page de paramètres ;
- messagerie patient ;
- accès à l’assistant IA.

Améliorations apportées :
- connexion de plusieurs écrans à la base de données ;
- cohérence visuelle renforcée entre les pages ;
- navigation par sidebar unifiée ;
- meilleure prise en compte du patient connecté.

### 3.2 Espace médecin
Le Sprint 2 a permis de construire une grande partie de l’espace médecin.

Fonctionnalités réalisées :
- page d’accueil médecin ;
- agenda médecin ;
- gestion des disponibilités ;
- liste des patients ;
- consultation du dossier patient ;
- envoi de documents ;
- gestion des factures ;
- statistiques médecin ;
- profil public médecin ;
- paramètres médecin ;
- messagerie médecin ;
- accès à l’assistant IA.

Logique métier ajoutée :
- le médecin peut définir ses créneaux de disponibilité ;
- les patients réservent selon les créneaux réellement ouverts ;
- les documents envoyés sont rattachés aux patients concernés ;
- les éléments de facturation sont reliés aux consultations et aux patients.

### 3.3 Espace administrateur
Le Sprint 2 a aussi permis de mettre en place un véritable espace administrateur.

Pages et fonctionnalités ajoutées :
- dashboard administrateur ;
- gestion des utilisateurs ;
- gestion des médecins ;
- supervision des rendez-vous ;
- supervision des documents ;
- supervision des factures ;
- statistiques administrateur ;
- liste d’attente ;
- notifications ;
- activité ;
- paramètres administrateur.

L’administrateur peut ainsi superviser le fonctionnement global de la plateforme à partir d’un espace dédié.

## 4. Base de données et backend
Le Sprint 2 a marqué une forte progression côté backend et base de données.

Évolutions majeures :
- enrichissement du schéma SQL ;
- ajout de tables liées aux conversations et participants ;
- ajout de la gestion des créneaux de disponibilités ;
- amélioration de la gestion des rendez-vous ;
- ajout de la logique documents médicaux ;
- ajout de la logique factures ;
- meilleure exploitation des rôles utilisateurs.

Exemples de données et structures utilisées :
- `users`
- `doctors`
- `appointments`
- `doctor_availability_slots`
- `medical_records`
- `invoices`
- `messages`
- `chatbot_messages`
- `conversation_participants`

## 5. Réalisations techniques importantes
Par rapport au Sprint 1, les avancées techniques du Sprint 2 sont significatives.

Principaux points :
- meilleure architecture JavaFX avec plusieurs contrôleurs spécialisés ;
- utilisation renforcée du modèle DAO ;
- navigation entre écrans plus structurée ;
- logique de session utilisateur plus claire ;
- séparation plus nette entre interface, données et logique ;
- montée en complexité maîtrisée sur la base de données ;
- fusion de plusieurs branches de travail en équipe.

## 6. Difficultés rencontrées
Le Sprint 2 a été plus complexe que le Sprint 1.

Difficultés principales :
- augmentation importante du nombre de pages ;
- gestion de la cohérence visuelle entre patient, médecin et admin ;
- synchronisation du travail d’équipe via Git et branches multiples ;
- conflits de fusion entre branches ;
- erreurs de compilation après fusion ;
- stabilisation de la navigation entre écrans ;
- connexion des nouvelles fonctionnalités à la base de données.

## 7. Solutions apportées
Pour répondre à ces difficultés, plusieurs solutions ont été mises en place :
- harmonisation des sidebars ;
- réorganisation de la navigation ;
- correction des conflits Git ;
- recompilation propre du projet ;
- correction du schéma SQL ;
- sécurisation de certains contrôleurs pour éviter les blocages au chargement ;
- séparation plus nette entre messagerie humaine et assistant IA ;
- réinitialisation contrôlée de certains comptes de test.

## 8. Résultats du sprint
À l’issue du Sprint 2, MeetCare dispose désormais :
- d’un espace patient plus fonctionnel ;
- d’un espace médecin largement construit ;
- d’un espace administrateur opérationnel ;
- d’une base de données plus riche ;
- d’une logique plus proche d’une vraie plateforme médicale.

Le produit est donc passé :
- d’un MVP centré sur le patient ;
- à une plateforme multi-profils beaucoup plus réaliste.

## 9. Budget du Sprint 2
Le budget du Sprint 2 peut être présenté sur la même logique que le Sprint 1.

### Proposition de tableau budgétaire
| Rôle | Jours | Tarif journalier | Coût |
|---|---:|---:|---:|
| Product Owner | 3 | 1000 € | 3000 € |
| Scrum Master | 10 | 800 € | 8000 € |
| Lead Developer | 10 | 550 € | 5500 € |
| Développeur | 10 | 550 € | 5500 € |

**Total estimé Sprint 2 : 22 000 €**

### Suivi budgétaire cumulé
- Budget initial : 100 000 €
- Budget Sprint 1 : 23 000 €
- Budget Sprint 2 estimé : 22 000 €
- Budget cumulé : 45 000 €
- Budget restant estimé : 55 000 €

Si vous avez un autre découpage réel des jours, ce tableau peut être ajusté rapidement.

## 10. Rétrospective du sprint

### Points positifs
- forte progression fonctionnelle ;
- meilleure crédibilité du produit ;
- enrichissement du backend ;
- structuration plus complète du projet ;
- meilleure couverture des profils utilisateurs ;
- bonne dynamique d’équipe sur les branches fonctionnelles.

### Points à améliorer
- mieux synchroniser les branches en amont ;
- réduire les conflits de fusion ;
- améliorer encore la stabilité après intégration ;
- ajouter davantage de tests automatiques ;
- mieux documenter certaines décisions techniques.

## 11. Projection Sprint 3
Pour le Sprint 3, l’équipe prévoit :
- finaliser la stabilisation des fonctionnalités existantes ;
- améliorer les tests ;
- renforcer la messagerie et l’assistant IA ;
- améliorer les notifications ;
- finaliser certains détails UX/UI ;
- poursuivre la fiabilisation du backend ;
- préparer une soutenance claire, crédible et démontrable.

## 12. Conclusion
Le Sprint 2 constitue une étape majeure dans l’évolution de MeetCare.

Il a permis :
- d’élargir fortement le périmètre fonctionnel ;
- de donner une vraie profondeur au produit ;
- de connecter davantage l’application à une logique métier réaliste ;
- de préparer une base solide pour le Sprint 3.

MeetCare n’est plus seulement une interface prometteuse : c’est désormais une application structurée, multi-profils et bien plus proche d’un produit complet.
