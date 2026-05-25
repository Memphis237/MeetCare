package org.example.meetcare.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.example.meetcare.model.Creneau;
import org.example.meetcare.utils.DatabaseConnection;

public class CreneauDao {

    // 1. Enregistrer un nouveau créneau en base de données
   public boolean save(Creneau creneau) {
    // 1. Syntaxe SQL corrigée (5 colonnes = 5 points d'interrogation, pas de WHERE)
    String sql = "INSERT INTO availabilities (availability_date, start_time, end_time, is_available, id_doctors) VALUES (?, ?, ?, ?, ?)";
    
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
        
        // 2. Remplissage des 5 paramètres dans le bon ordre
        pstmt.setDate(1, Date.valueOf(creneau.getDate()));
        pstmt.setTime(2, Time.valueOf(creneau.getHeureDebut()));
        pstmt.setTime(3, Time.valueOf(creneau.getHeureFin()));
        pstmt.setBoolean(4, creneau.isDisponible());
        pstmt.setInt(5, creneau.getIdDoctor()); // Ajout indispensable du médecin !

        int affectedRows = pstmt.executeUpdate();
        System.out.println("Nombre de lignes affectées : " + affectedRows);
        
        if (affectedRows > 0) {
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    // Récupère l'id_availabilities généré par MySQL
                    creneau.setId(generatedKeys.getInt(1));
                }
                System.out.println("Créneau ajouté avec succès en BDD. ID généré : " + creneau.getId());
            }
            return true;
        }
    } catch (SQLException e) {
        System.out.println("❌ Erreur SQL lors de l'ajout du créneau : " + e.getMessage());
        e.printStackTrace();
    }
    return false;
}

    // 2. Récupérer tous les créneaux d'un jour précis
    public List<Creneau> findByDate(LocalDate dateRecherchee) {
        List<Creneau> liste = new ArrayList<>();
        
        // Requête SQL exacte basée sur la structure de ta table
        String sql = "SELECT id_availabilities, availability_date, start_time, end_time, is_available, id_doctors " +
                     "FROM availabilities WHERE availability_date = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            // On injecte la date sélectionnée dans le filtre SQL
            pstmt.setDate(1, Date.valueOf(dateRecherchee));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    // Extraction des données de la ligne MySQL
                    int id = rs.getInt("id_availabilities");
                    LocalDate date = rs.getDate("availability_date").toLocalDate();
                    java.time.LocalTime debut = rs.getTime("start_time").toLocalTime();
                    java.time.LocalTime fin = rs.getTime("end_time").toLocalTime();
                    boolean disponible = rs.getBoolean("is_available");
                    int idDoctor = rs.getInt("id_doctors");

                    // Création de l'objet modèle Creneau
                    Creneau c = new Creneau(id, date, debut, fin, disponible, idDoctor);
                    
                    // Ajout à la liste finale
                    liste.add(c);
                }
            }
            
            System.out.println("🚀 [DAO] " + liste.size() + " créneau(x) récupéré(s) pour la date : " + dateRecherchee);

        } catch (SQLException e) {
            System.out.println("❌ Erreur lors de la récupération des créneaux : " + e.getMessage());
            e.printStackTrace();
        }
        
        return liste;
    }
    // 3. Supprimer un créneau par son ID
    public boolean delete(int id) {
        String sql = "DELETE FROM availabilities WHERE id_availabilities = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;            
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("❌ Erreur SQL lors de la suppression du créneau : " + e.getMessage());
        }
        return false;
    }
}