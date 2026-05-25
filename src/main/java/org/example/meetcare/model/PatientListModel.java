package org.example.meetcare.model;

import java.time.LocalDate;

public class PatientListModel {
    private int id;
    private String nom;
    private String email;
    private String telephone;
    private int nbRendezVous;
    private LocalDate dernierRDV;
    private String description; // Historique médical, notes du médecin...

    public PatientListModel(int id, String nom, String email, String telephone, int nbRendezVous, LocalDate dernierRDV, String description) {
        this.id = id;
        this.nom = nom;
        this.email = email;
        this.telephone = telephone;
        this.nbRendezVous = nbRendezVous;
        this.dernierRDV = dernierRDV;
        this.description = description;
    }

    // Getters
    public int getId() { return id; }
    public String getNom() { return nom; }
    public String getEmail() { return email; }
    public String getTelephone() { return telephone; }
    public int getNbRendezVous() { return nbRendezVous; }
    public LocalDate getDernierRDV() { return dernierRDV; }
    public String getDescription() { return description; }
}
