package org.example.meetcare.model;

import org.example.meetcare.model.Creneau;
import java.util.List;

public class DisponibiliteModel {
    private int total;
    private int disponibles;
    private int occupes;
    private int taux;

    public DisponibiliteModel(List<Creneau> creneaux) {
        this.total = creneaux.size();
        this.disponibles = (int) creneaux.stream().filter(Creneau::isDisponible).count();
        this.occupes = this.total - this.disponibles;
        this.taux = this.total > 0 ? (this.occupes * 100) / this.total : 0;
    }

    public int getTotal() { return total; }
    public int getDisponibles() { return disponibles; }
    public int getOccupes() { return occupes; }
    public int getTaux() { return taux; }
}