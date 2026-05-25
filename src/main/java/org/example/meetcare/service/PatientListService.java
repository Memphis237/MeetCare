package org.example.meetcare.service;

import org.example.meetcare.dao.PatientListDao;
import org.example.meetcare.model.PatientListModel;
import java.util.List;
import java.util.stream.Collectors;
public class PatientListService {

    private final PatientListDao patientDao = new PatientListDao();

    /**
     * Récupère la liste de tous les patients associés à un médecin spécifique.
     * Trie automatiquement les patients par ordre alphabétique (Nom).
     */
    public List<PatientListModel> getPatientsParDocteur(int idDoctor) {
        List<PatientListModel> liste = patientDao.findPatientsByDoctor(idDoctor);
        
        // Règle métier : On trie les patients par nom pour un affichage plus agréable
        return liste.stream()
                .sorted((p1, p2) -> p1.getNom().compareToIgnoreCase(p2.getNom()))
                .collect(Collectors.toList());
    }

    /**
     * Filtre une liste de patients existante selon un mot-clé (Nom ou Email).
     * Utile pour la barre de recherche du contrôleur.
     */
    public List<PatientListModel> filtrerPatients(List<PatientListModel> listeComplete, String texteRecherche) {
        if (texteRecherche == null || texteRecherche.trim().isEmpty()) {
            return listeComplete;
        }

        String filtre = texteRecherche.toLowerCase().trim();

        return listeComplete.stream()
                .filter(p -> p.getNom().toLowerCase().contains(filtre) || 
                             p.getEmail().toLowerCase().contains(filtre))
                .collect(Collectors.toList());
    }
}
