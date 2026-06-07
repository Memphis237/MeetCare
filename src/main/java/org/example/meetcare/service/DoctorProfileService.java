package org.example.meetcare.service;

import javafx.scene.Node;
import org.example.meetcare.dao.DoctorDao;
import org.example.meetcare.dao.UsersDao;
import org.example.meetcare.model.Doctor;
import org.example.meetcare.model.Users;
import org.example.meetcare.utils.PasswordUtil;

public class DoctorProfileService {
    private final UserProfileService userProfileService = new UserProfileService();
    private final UsersDao usersDao = new UsersDao();
    private final DoctorDao doctorDao = new DoctorDao();

    public Users updatePersonalInfo(Users currentUser, String prenom, String username, String email, String phone,
                                    String address, String city, String codePostal, String country) {
        return userProfileService.updatePersonalInfo(currentUser, prenom, username, email, phone, address, city, codePostal, country);
    }

    public Users changePassword(Users currentUser, String currentPassword, String newPassword, String confirmPassword) {
        return userProfileService.changePassword(currentUser, currentPassword, newPassword, confirmPassword);
    }

    public Users updateAvatar(Users currentUser, Node owner) {
        return userProfileService.updateAvatar(currentUser, owner);
    }

    public Users removeAvatar(Users currentUser) {
        return userProfileService.removeAvatar(currentUser);
    }

    public Doctor updateProfessionalInfo(Users currentUser, String speciality, String address, String sector,
                                         String consultationPrice, String experience, String biography,
                                         boolean acceptsVitale, boolean teleconsultation) {
        if (currentUser == null) {
            throw new IllegalArgumentException("Aucun utilisateur connecte.");
        }
        doctorDao.updateProfile(
                currentUser.getIdUsers(),
                speciality,
                address,
                sector,
                consultationPrice,
                experience,
                biography,
                acceptsVitale,
                teleconsultation
        );
        return doctorDao.findByUserId(currentUser.getIdUsers())
                .orElseThrow(() -> new IllegalStateException("Profil medecin introuvable."));
    }

    public void deleteAccount(Users currentUser) {
        if (currentUser == null) {
            throw new IllegalArgumentException("Aucun utilisateur connecte.");
        }
        userProfileService.removeAvatar(currentUser);
        usersDao.deleteById(currentUser.getIdUsers());
    }

    public Doctor loadDoctorProfile(int userId) {
        return doctorDao.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Profil medecin introuvable."));
    }
}
