package org.example.meetcare.service;

import javafx.scene.Node;
import org.example.meetcare.dao.DoctorAccountDao;
import org.example.meetcare.dao.UsersDao;
import org.example.meetcare.model.Users;
import org.example.meetcare.utils.AppSession;

public class DoctorAccountService {
    private final DoctorAccountDao doctorAccountDao = new DoctorAccountDao();
    private final UserProfileService userProfileService = new UserProfileService();
    private final UsersDao usersDao = new UsersDao();

    public DoctorAccountDao.DoctorPublicProfileRecord loadPublicProfile() {
        return doctorAccountDao.getDoctorPublicProfile();
    }

    public DoctorAccountDao.DoctorSettingsRecord loadSettings() {
        return doctorAccountDao.getDoctorSettings();
    }

    public Users updateDoctorAvatar(Node owner) {
        Users currentUser = requireCurrentUser();
        Users updatedUser = userProfileService.updateAvatar(currentUser, owner);
        AppSession.setCurrentUser(updatedUser);
        return updatedUser;
    }

    public Users updateDoctorPassword(String currentPassword, String newPassword, String confirmPassword) {
        Users currentUser = requireCurrentUser();
        Users updatedUser = userProfileService.changePassword(currentUser, currentPassword, newPassword, confirmPassword);
        AppSession.setCurrentUser(updatedUser);
        return updatedUser;
    }

    public DoctorAccountDao.DoctorPublicProfileRecord savePublicProfile(DoctorAccountDao.DoctorPublicProfileForm form) {
        doctorAccountDao.updateDoctorPublicProfile(form);
        refreshCurrentUser();
        return loadPublicProfile();
    }

    public DoctorAccountDao.DoctorSettingsRecord saveContactSettings(String email, String phone) {
        doctorAccountDao.updateDoctorContactSettings(email, phone);
        refreshCurrentUser();
        return loadSettings();
    }

    public DoctorAccountDao.DoctorSettingsRecord saveNotificationSettings(boolean notifyEmail, boolean notifySms,
                                                                          boolean notifyNewAppointments, boolean notifyCancellations,
                                                                          boolean notifyNewMessages) {
        doctorAccountDao.updateNotificationSettings(notifyEmail, notifySms, notifyNewAppointments, notifyCancellations, notifyNewMessages);
        refreshCurrentUser();
        return loadSettings();
    }

    public void deactivateAccount() {
        doctorAccountDao.updateAccountStatus("INACTIVE");
        refreshCurrentUser();
    }

    private Users requireCurrentUser() {
        Users currentUser = AppSession.getCurrentUser();
        if (currentUser == null || currentUser.getIdUsers() <= 0) {
            throw new IllegalArgumentException("Aucun utilisateur connecte.");
        }
        return currentUser;
    }

    private void refreshCurrentUser() {
        Users currentUser = AppSession.getCurrentUser();
        if (currentUser == null || currentUser.getIdUsers() <= 0) {
            return;
        }
        AppSession.setCurrentUser(usersDao.findById(currentUser.getIdUsers()).orElse(currentUser));
    }
}
