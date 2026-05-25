package org.example.meetcare.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.example.meetcare.model.PatientListModel;
import org.example.meetcare.utils.DatabaseConnection;

public class PatientListDao {
    public List<PatientListModel> findPatientsByDoctor(int idDoctor) {
        List<PatientListModel> liste = new ArrayList<>();
       // Requête SQL adaptée à tes vraies tables : users, appointments et medical_records
        String sql = "SELECT u.id_users, u.username, u.prenom, u.email, u.phone, " +
                     "m.description AS description_medicale, " +
                     "COUNT(a.id_appointments) AS nb_rdv, " +
                     "MAX(a.appointment_date) AS dernier_rdv " +
                     "FROM users u " +
                     "JOIN appointments a ON u.id_users = a.id_users " +
                     "LEFT JOIN medical_records m ON u.id_users = m.id_users AND m.id_doctors = ? " +
                     "WHERE a.id_doctors = ? " +
                     "GROUP BY u.id_users, m.description";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, idDoctor);
            pstmt.setInt(2, idDoctor);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id_users");
                    
                    // On concatène le Prénom et le Nom pour ton affichage IHM
                    String nomComplet = rs.getString("prenom") + " " + rs.getString("username");
                    String email = rs.getString("email");
                    String tel = rs.getString("phone");
                    String desc = rs.getString("description_medicale");
                    int nbRdv = rs.getInt("nb_rdv");
                    
                    LocalDate dernierRdv = null;
                    if (rs.getDate("dernier_rdv") != null) {
                        dernierRdv = rs.getDate("dernier_rdv").toLocalDate();
                    }
                  PatientListModel patientList = new PatientListModel( id,  nomComplet,  email, tel, nbRdv, dernierRdv, desc);
                    // Création de l'objet Patient pour le Service et la Vue
                    liste.add(patientList);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur SQL dans PatientDao : " + e.getMessage());
            e.printStackTrace();
        }
        return liste;
    }
}
