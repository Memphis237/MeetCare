package org.example.meetcare.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.LocalTime;

public class Creneau {

    // 1. Les variables sont privées et cachées
    private int id;
    private LocalDate date;
    private boolean disponible;
    private int idDoctor; // Ajout de l'ID du médecin pour lier le créneau à un médecin spécifique

    // Déclaration des variables pour stocker l'heure
    private LocalTime heureDebut;
    private LocalTime heureFin;

    // CONSTRUCTEUR
    // CONSTRUCTEUR
    public Creneau(int id,LocalDate date, LocalTime heureDebut, LocalTime heureFin, boolean disponible, int idDoctor) {
        this.date = date;
        this.heureDebut = heureDebut;
        this.heureFin = heureFin;
        this.disponible = disponible;
        this.id= id;
        this.idDoctor = 1; // Par défaut, on peut l'initialiser à 1 ou le passer en paramètre du constructeur
    }

    // 2. LES GETTERS (Pour LIRE les valeurs depuis l'extérieur)
    // Ils retournent le même type que la variable et ne prennent pas de paramètre.
    public int getId() {
        return id;
    }

    public int getIdDoctor() {
        return idDoctor;
    }
   
    public LocalDate getDate() {
        return date;
    }

    public boolean isDisponible() { // Pour les bouléens, "get" devient souvent "is"
        return disponible;
    }

    // 3. LES SETTERS (Pour MODIFIER les valeurs depuis l'extérieur)
    // Ils sont toujours "void" (ne retournent rien) et prennent la nouvelle valeur en paramètre.
    public void setId(int id) {
        this.id = id;
    }

    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }

    // Permet de récupérer l'objet LocalTime complet de l'heure de début
    public LocalTime getHeureDebut() {
        return heureDebut;
    }

    // Permet de modifier l'heure de début si besoin
    public void setHeureDebut(LocalTime heureDebut) {
        this.heureDebut = heureDebut;
    }

    // Permet de récupérer l'objet LocalTime complet de l'heure de fin
    public LocalTime getHeureFin() {
        return heureFin;
    }

    // Permet de modifier l'heure de fin si besoin
    public void setHeureFin(LocalTime heureFin) {
        this.heureFin = heureFin;
    }
    
    // Fonction pour obtenir l'heure de début au format "HH:mm" (ex: 09:15)
public String getHeureDebutFormatee() {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
    return this.heureDebut.format(formatter);
}

// Fonction pour obtenir l'heure de fin au format "HH:mm" (ex: 10:00)
public String getHeureFinFormatee() {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
    return this.heureFin.format(formatter);
}

// Fonction très pratique pour afficher l'intervalle complet sur l'interface
public String getPlageHoraire() {
    return getHeureDebutFormatee() + " - " + getHeureFinFormatee();
} 

}


