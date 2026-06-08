package org.example.meetcare.service;

import javafx.scene.Node;
import org.example.meetcare.dao.DoctorDao;
import org.example.meetcare.dao.UsersDao;
import org.example.meetcare.model.Doctor;
import org.example.meetcare.model.DoctorAdminEntry;
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

    public DoctorAdminEntry loadPublicProfile(Users currentUser) {
        if (currentUser == null) {
            throw new IllegalArgumentException("Aucun utilisateur connecte.");
        }
        try {
            return doctorDao.findPublicProfileByUserId(currentUser.getIdUsers())
                    .orElseGet(() -> buildPublicProfileFallback(currentUser));
        } catch (RuntimeException ex) {
            return buildPublicProfileFallback(currentUser);
        }
    }

    public DoctorAdminEntry loadPublicProfileByDoctorId(int doctorId) {
        try {
            return doctorDao.findPublicProfileByDoctorId(doctorId)
                    .or(() -> doctorDao.findUserIdByDoctorId(doctorId)
                            .flatMap(userId -> {
                                try {
                                    return doctorDao.findPublicProfileByUserId(userId);
                                } catch (RuntimeException ex) {
                                    return java.util.Optional.empty();
                                }
                            }))
                    .orElseGet(() -> buildPublicProfileFallback(doctorId));
        } catch (RuntimeException ex) {
            return buildPublicProfileFallback(doctorId);
        }
    }

    public DoctorAdminEntry updatePublicProfile(Users currentUser, String speciality, String address, String sector,
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
        int doctorId = doctorDao.ensureDoctorIdForUser(currentUser.getIdUsers());
        doctorDao.markPublicProfileCompleted(doctorId);
        return loadPublicProfile(currentUser);
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

    private DoctorAdminEntry buildPublicProfileFallback(Users currentUser) {
        Doctor doctor = doctorDao.findByUserId(currentUser.getIdUsers()).orElse(null);
        if (doctor == null) {
            return buildPublicProfileFallback(currentUser.getIdUsers());
        }
        return new DoctorAdminEntry(
                currentUser.getIdUsers(),
                doctor.getId(),
                currentUser.getPrenom(),
                currentUser.getUsername(),
                currentUser.getEmail(),
                currentUser.getPhone(),
                currentUser.getCity(),
                currentUser.getAddress(),
                doctor.getSpeciality(),
                doctor.getAddress(),
                doctor.getExperience(),
                doctor.getPrice(),
                doctor.getBiography(),
                doctor.getSector(),
                doctor.isTeleconsultation(),
                doctor.isAcceptsVitale(),
                doctor.isPublicProfileCompleted(),
                currentUser.getAccountStatus()
        );
    }

    private DoctorAdminEntry buildPublicProfileFallback(int doctorId) {
        return new DoctorAdminEntry(
                0,
                doctorId,
                "Medecin",
                "",
                "",
                "",
                "",
                "",
                "Medecine generale",
                "",
                null,
                null,
                "",
                "Secteur 1",
                false,
                false,
                false,
                org.example.meetcare.model.AccountStatus.PENDING
        );
    }
}
