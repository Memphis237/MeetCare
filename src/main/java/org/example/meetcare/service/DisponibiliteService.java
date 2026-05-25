package org.example.meetcare.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.example.meetcare.dao.CreneauDao;
import org.example.meetcare.model.Creneau;

public class DisponibiliteService {

    private final CreneauDao creneauDao = new CreneauDao();

    public List<Creneau> getCreneauxParDate(LocalDate date) {
        List<Creneau> creneaux = creneauDao.findByDate(date);
        
        // Filtrer les créneaux passés si c'est aujourd'hui
        if (date.equals(LocalDate.now())) {
            LocalTime heureActuelle = LocalTime.now();
            creneaux.removeIf(c -> c.getHeureDebut().isBefore(heureActuelle));
        }
        
        return creneaux;
    }

    public void ajouterCreneau(int id, LocalDate date, LocalTime heureDebut, LocalTime heureFin, boolean disponible, int idDoctor) {
        Creneau nouveau = new Creneau(id, date, heureDebut, heureFin, disponible, idDoctor);
        creneauDao.save(nouveau); 
    }
    
    public void supprimerCreneau(Creneau creneau) {
        creneauDao.delete(creneau.getId()); 
    }

    // --- Calculs mis à jour : On passe la liste active en paramètre pour éviter les bugs d'affichage ---
    
    public int getTotal(List<Creneau> liste) { 
        if (liste == null) return 0;
        return liste.size(); 
    }
    
    public int getDisponibles(List<Creneau> liste) { 
        if (liste == null) return 0;
        return (int) liste.stream().filter(Creneau::isDisponible).count(); 
    }
    
    public int getOccupes(List<Creneau> liste) { 
        return getTotal(liste) - getDisponibles(liste); 
    }
    
    public int getTaux(List<Creneau> liste) { 
        int total = getTotal(liste);
        return total > 0 ? (getOccupes(liste) * 100) / total : 0; 
    }
}