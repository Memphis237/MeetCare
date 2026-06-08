package org.example.meetcare.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.shape.Circle;
import org.example.meetcare.model.DoctorAdminEntry;
import org.example.meetcare.service.DoctorProfileService;
import org.example.meetcare.utils.AppSession;
import org.example.meetcare.utils.NavigationUtil;

import java.util.Locale;

public class DoctorPublicProfileController {
    private final DoctorProfileService doctorProfileService = new DoctorProfileService();

    private Integer doctorId;
    private DoctorAdminEntry profile;
    private boolean initialized;

    @FXML
    private Circle avatarCircle;

    @FXML
    private Label avatarInitialsLabel;

    @FXML
    private Label doctorNameLabel;

    @FXML
    private Label specialityLabel;

    @FXML
    private Label cityLabel;

    @FXML
    private Label addressLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private Label biographyLabel;

    @FXML
    private Label sectorLabel;

    @FXML
    private Label experienceLabel;

    @FXML
    private Label consultationPriceLabel;

    @FXML
    private Label acceptsVitaleLabel;

    @FXML
    private Label teleconsultationLabel;

    @FXML
    private Label cabinetAddressLabel;

    @FXML
    private Button bookAppointmentButton;

    @FXML
    private Button previewBackButton;

    @FXML
    private void initialize() {
        initialized = true;
        if (doctorId != null) {
            loadProfile();
        }
    }

    public void setDoctorId(int doctorId) {
        this.doctorId = doctorId;
        if (initialized) {
            loadProfile();
        }
    }

    @FXML
    private void handleBackToSearch(ActionEvent event) {
        String returnRoute = AppSession.getProfileReturnRoute();
        if ("settings".equals(returnRoute)) {
            NavigationUtil.openMedecinSettings(event);
            return;
        }
        NavigationUtil.openPatientSearch(event);
    }

    @FXML
    private void handleBookAppointment(ActionEvent event) {
        if (profile == null) {
            return;
        }

        NavigationUtil.openPatientAppointmentBooking(
                event,
                profile.getDoctorId(),
                displayName(profile),
                safeText(profile.getSpeciality(), "Medecine generale"),
                safeText(profile.getDoctorAddress(), safeText(profile.getAddress(), "")));
    }

    private void loadProfile() {
        try {
            if (doctorId == null || doctorId <= 0) {
                renderFallbackProfile();
                return;
            }

            profile = doctorProfileService.loadPublicProfileByDoctorId(doctorId);
            if (profile == null) {
                renderFallbackProfile();
                return;
            }

            renderProfile(profile);
        } catch (Exception ex) {
            renderFallbackProfile();
        }
    }

    private void renderProfile(DoctorAdminEntry profile) {
        if (profile == null) {
            renderFallbackProfile();
            return;
        }

        this.profile = profile;

        // Nom et infos de base
        String displayName = displayName(profile);
        if (doctorNameLabel != null)
            doctorNameLabel.setText(displayName);
        if (avatarInitialsLabel != null)
            avatarInitialsLabel.setText(initials(profile));

        // Spécialité
        String speciality = safeText(profile.getSpeciality(), "Médecine générale");
        if (specialityLabel != null)
            specialityLabel.setText(speciality);

        // Ville
        String city = safeText(profile.getCity(), "Ville non renseignée");
        if (cityLabel != null)
            cityLabel.setText(city);

        // Status
        String status = profile.isPublicProfileCompleted()
                ? "Profil public vérifié"
                : "Profil en cours de finalisation";
        if (statusLabel != null)
            statusLabel.setText(status);

        // Biographie - important!
        String biography = safeText(profile.getBiography(), "Aucune biographie renseignée pour le moment.");
        if (biographyLabel != null)
            biographyLabel.setText(biography);

        // Secteur
        String sector = safeText(profile.getSector(), "Secteur 1");
        if (sectorLabel != null)
            sectorLabel.setText(sector);

        // Expérience
        String experience = profile.getExperience() == null
                ? "Expérience non précisée"
                : profile.getExperience() + " ans d'expérience";
        if (experienceLabel != null)
            experienceLabel.setText(experience);

        // Prix consultation
        String price = profile.getConsultationPrice() == null
                ? "Tarif non renseigné"
                : String.format(Locale.FRANCE, "%.2f EUR", profile.getConsultationPrice());
        if (consultationPriceLabel != null)
            consultationPriceLabel.setText(price);

        // Adresse du cabinet (2 champs)
        String doctorAddress = safeText(profile.getDoctorAddress(), "Adresse non renseignée");
        if (addressLabel != null)
            addressLabel.setText(doctorAddress);
        if (cabinetAddressLabel != null)
            cabinetAddressLabel.setText(doctorAddress);

        // Carte Vitale
        String vitale = profile.isAcceptsVitale() ? "Oui, accepte" : "Non";
        if (acceptsVitaleLabel != null)
            acceptsVitaleLabel.setText(vitale);

        // Téléconsultation
        String teleCons = profile.isTeleconsultation() ? "Disponible" : "Non disponible";
        if (teleconsultationLabel != null)
            teleconsultationLabel.setText(teleCons);
    }

    private void renderFallbackProfile() {
        profile = null;

        if (doctorNameLabel != null)
            doctorNameLabel.setText("Profil public indisponible");
        if (avatarInitialsLabel != null)
            avatarInitialsLabel.setText("D");
        if (specialityLabel != null)
            specialityLabel.setText("Profil à compléter");
        if (cityLabel != null)
            cityLabel.setText("Ville non renseignée");
        if (addressLabel != null)
            addressLabel.setText("Adresse du cabinet non renseignée");
        if (cabinetAddressLabel != null)
            cabinetAddressLabel.setText("Adresse non renseignée");
        if (statusLabel != null)
            statusLabel.setText("Profil incomplet");
        if (biographyLabel != null)
            biographyLabel.setText("Le médecin n'a pas encore publié sa biographie.");
        if (sectorLabel != null)
            sectorLabel.setText("Secteur non renseigné");
        if (experienceLabel != null)
            experienceLabel.setText("Expérience non renseignée");
        if (consultationPriceLabel != null)
            consultationPriceLabel.setText("Tarif non renseigné");
        if (acceptsVitaleLabel != null)
            acceptsVitaleLabel.setText("Non");
        if (teleconsultationLabel != null)
            teleconsultationLabel.setText("Non disponible");
    }

    private String displayName(DoctorAdminEntry doctor) {
        String prenom = doctor.getPrenom() == null ? "" : doctor.getPrenom().trim();
        String username = doctor.getUsername() == null ? "" : doctor.getUsername().trim();
        if (prenom.isEmpty() && username.isEmpty()) {
            return "Dr. Medecin";
        }
        if (prenom.isEmpty()) {
            return "Dr. " + username;
        }
        if (username.isEmpty()) {
            return "Dr. " + prenom;
        }
        return "Dr. " + prenom + " " + username;
    }

    private String initials(DoctorAdminEntry doctor) {
        String prenom = doctor.getPrenom() == null ? "" : doctor.getPrenom().trim();
        String username = doctor.getUsername() == null ? "" : doctor.getUsername().trim();
        String source = !prenom.isBlank() ? prenom : username;
        if (source.isBlank()) {
            return "D";
        }
        String[] parts = source.split("\\s+");
        if (parts.length > 1) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase(Locale.ROOT);
        }
        return source.substring(0, 1).toUpperCase(Locale.ROOT);
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
