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
                safeText(profile.getDoctorAddress(), safeText(profile.getAddress(), ""))
        );
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

        if (doctorNameLabel != null) {
            doctorNameLabel.setText(displayName(profile));
        }
        if (specialityLabel != null) {
            specialityLabel.setText(safeText(profile.getSpeciality(), "Medecine generale"));
        }
        if (cityLabel != null) {
            cityLabel.setText(safeText(profile.getCity(), "Ville non renseignee"));
        }
        if (addressLabel != null) {
            addressLabel.setText(safeText(profile.getDoctorAddress(), "Adresse du cabinet non renseignee"));
        }
        if (statusLabel != null) {
            statusLabel.setText(profile.isPublicProfileCompleted()
                    ? "Profil public verifie"
                    : "Profil en cours de finalisation");
        }
        if (biographyLabel != null) {
            biographyLabel.setText(safeText(profile.getBiography(), "Aucune biographie renseignee pour le moment."));
        }
        if (sectorLabel != null) {
            sectorLabel.setText(safeText(profile.getSector(), "Secteur 1"));
        }
        if (experienceLabel != null) {
            experienceLabel.setText(profile.getExperience() == null
                    ? "Experience non precisee"
                    : profile.getExperience() + " ans d'experience");
        }
        if (consultationPriceLabel != null) {
            consultationPriceLabel.setText(profile.getConsultationPrice() == null
                    ? "Tarif non renseigne"
                    : String.format(Locale.FRANCE, "%.2f EUR", profile.getConsultationPrice()));
        }
        if (acceptsVitaleLabel != null) {
            acceptsVitaleLabel.setText(profile.isAcceptsVitale() ? "Oui, accepte" : "Non");
        }
        if (teleconsultationLabel != null) {
            teleconsultationLabel.setText(profile.isTeleconsultation() ? "Disponible" : "Non disponible");
        }
        if (avatarInitialsLabel != null) {
            avatarInitialsLabel.setText(initials(profile));
        }
    }

    private void renderFallbackProfile() {
        profile = null;

        if (doctorNameLabel != null) {
            doctorNameLabel.setText("Profil public indisponible");
        }
        if (specialityLabel != null) {
            specialityLabel.setText("Profil a completer");
        }
        if (cityLabel != null) {
            cityLabel.setText("Ville non renseignee");
        }
        if (addressLabel != null) {
            addressLabel.setText("Adresse du cabinet non renseignee");
        }
        if (statusLabel != null) {
            statusLabel.setText("Profil incomplet");
        }
        if (biographyLabel != null) {
            biographyLabel.setText("Le medecin n'a pas encore publie sa biographie.");
        }
        if (sectorLabel != null) {
            sectorLabel.setText("Secteur non renseigne");
        }
        if (experienceLabel != null) {
            experienceLabel.setText("Experience non renseignee");
        }
        if (consultationPriceLabel != null) {
            consultationPriceLabel.setText("Tarif non renseigne");
        }
        if (acceptsVitaleLabel != null) {
            acceptsVitaleLabel.setText("Non");
        }
        if (teleconsultationLabel != null) {
            teleconsultationLabel.setText("Non disponible");
        }
        if (avatarInitialsLabel != null) {
            avatarInitialsLabel.setText("D");
        }
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
