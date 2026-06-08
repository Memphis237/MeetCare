package org.example.meetcare.service;

import javafx.scene.Node;
import javafx.stage.FileChooser;
import org.example.meetcare.dao.UsersDao;
import org.example.meetcare.model.Users;
import org.example.meetcare.utils.PasswordUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class UserProfileService {
    private final UsersDao usersDao = new UsersDao();

    public Users updatePersonalInfo(Users currentUser, String prenom, String username, String email, String phone,
                                    String address, String city, String codePostal, String country) {
        if (currentUser == null) {
            throw new IllegalArgumentException("Aucun utilisateur connecté.");
        }

        currentUser.setPrenom(requireText(prenom, "Le prénom est obligatoire."));
        currentUser.setUsername(requireText(username, "Le nom d'utilisateur est obligatoire."));
        currentUser.setEmail(requireText(email, "L'email est obligatoire."));
        currentUser.setPhone(phone);
        currentUser.setAddress(requireText(address, "L'adresse est obligatoire."));
        currentUser.setCity(requireText(city, "La ville est obligatoire."));
        currentUser.setCodePostal(requireText(codePostal, "Le code postal est obligatoire."));
        currentUser.setCountry(requireText(country, "Le pays est obligatoire."));

        usersDao.updateProfile(currentUser);
        return usersDao.findById(currentUser.getIdUsers()).orElse(currentUser);
    }

    public Users changePassword(Users currentUser, String currentPassword, String newPassword, String confirmPassword) {
        if (currentUser == null) {
            throw new IllegalArgumentException("Aucun utilisateur connecté.");
        }
        if (isBlank(currentPassword) || isBlank(newPassword) || isBlank(confirmPassword)) {
            throw new IllegalArgumentException("Tous les champs de mot de passe sont obligatoires.");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("Les nouveaux mots de passe ne correspondent pas.");
        }

        Users freshUser = usersDao.findById(currentUser.getIdUsers())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));
        if (!PasswordUtil.matches(currentPassword, freshUser.getPassword())) {
            throw new IllegalArgumentException("Le mot de passe actuel est incorrect.");
        }

        String hashedPassword = PasswordUtil.hash(newPassword);
        usersDao.updatePassword(currentUser.getIdUsers(), hashedPassword);
        Users updatedUser = usersDao.findById(currentUser.getIdUsers())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));
        updatedUser.setPassword(hashedPassword);
        return updatedUser;
    }

    public Users updateAvatar(Users currentUser, Node owner) {
        if (currentUser == null) {
            throw new IllegalArgumentException("Aucun utilisateur connecté.");
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une photo de profil");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );

        File selectedFile = fileChooser.showOpenDialog(owner == null ? null : owner.getScene().getWindow());
        if (selectedFile == null) {
            return currentUser;
        }

        String savedPath = saveAvatarFile(currentUser, selectedFile);
        usersDao.updateAvatar(currentUser.getIdUsers(), savedPath);

        Users updatedUser = usersDao.findById(currentUser.getIdUsers())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));
        updatedUser.setAvatar(savedPath);
        return updatedUser;
    }

    public Users removeAvatar(Users currentUser) {
        if (currentUser == null) {
            throw new IllegalArgumentException("Aucun utilisateur connecté.");
        }

        deleteAvatarFile(currentUser.getAvatar());
        usersDao.updateAvatar(currentUser.getIdUsers(), null);

        Users updatedUser = usersDao.findById(currentUser.getIdUsers())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));
        updatedUser.setAvatar(null);
        return updatedUser;
    }

    public void deleteAccount(Users currentUser) {
        if (currentUser == null) {
            throw new IllegalArgumentException("Aucun utilisateur connectÃ©.");
        }

        removeAvatar(currentUser);
        usersDao.deleteById(currentUser.getIdUsers());
    }

    private String saveAvatarFile(Users user, File sourceFile) {
        try {
            Path avatarsDir = Paths.get(System.getProperty("user.dir"), "uploads", "avatars");
            Files.createDirectories(avatarsDir);

            String extension = getExtension(sourceFile.getName());
            String safeExtension = extension.isBlank() ? ".png" : extension;
            String fileName = "user-" + user.getIdUsers() + "-" + System.currentTimeMillis() + safeExtension;
            Path targetPath = avatarsDir.resolve(fileName).toAbsolutePath().normalize();
            Files.copy(sourceFile.toPath(), targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return targetPath.toString();
        } catch (IOException ex) {
            throw new IllegalStateException("Impossible d'enregistrer la photo de profil.", ex);
        }
    }

    private String getExtension(String fileName) {
        int dotIndex = fileName == null ? -1 : fileName.lastIndexOf('.');
        if (dotIndex < 0) {
            return "";
        }
        return fileName.substring(dotIndex);
    }

    private void deleteAvatarFile(String avatarPath) {
        if (isBlank(avatarPath)) {
            return;
        }

        try {
            Files.deleteIfExists(Paths.get(avatarPath));
        } catch (IOException ignored) {
            // If the file cannot be deleted, keep the DB state consistent anyway.
        }
    }

    private String requireText(String value, String message) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
