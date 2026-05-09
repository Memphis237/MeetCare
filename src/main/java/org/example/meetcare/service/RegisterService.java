package org.example.meetcare.service;

import org.example.meetcare.dao.UsersDao;
import org.example.meetcare.model.Role;
import org.example.meetcare.model.Users;
import org.mindrot.jbcrypt.BCrypt;

public class RegisterService {
    private final UsersDao usersDao = new UsersDao();

    public Users register(Users user, String confirmPassword) {
        validateRegistration(user, confirmPassword);

        user.setPassword(hashPassword(user.getPassword()));
        user.setRole(Role.USER);
        user.setOnline(false);
        user.setIdUsers(usersDao.insert(user));
        return user;
    }

    public Users login(String identifier, String password) {
        validateLogin(identifier, password);
        Users user = usersDao.findByIdentifier(identifier)
                .orElseThrow(() -> new IllegalArgumentException("Identifiants incorrects."));

        if (isBcryptHash(user.getPassword())) {
            if (!BCrypt.checkpw(password, user.getPassword())) {
                throw new IllegalArgumentException("Identifiants incorrects.");
            }
            return user;
        }

        if (user.getPassword() != null && user.getPassword().equals(hashPasswordLegacy(password))) {
            user.setPassword(hashPassword(password));
            return user;
        }

        throw new IllegalArgumentException("Identifiants incorrects.");
    }

    private void validateRegistration(Users user, String confirmPassword) {
        if (user == null) {
            throw new IllegalArgumentException("Le formulaire d'inscription est invalide.");
        }
        if (isBlank(user.getUsername()) || isBlank(user.getEmail()) || isBlank(user.getPassword())) {
            throw new IllegalArgumentException("Le nom d'utilisateur, l'email et le mot de passe sont obligatoires.");
        }
        if (isBlank(confirmPassword)) {
            throw new IllegalArgumentException("La confirmation du mot de passe est obligatoire.");
        }
        if (!user.getPassword().equals(confirmPassword)) {
            throw new IllegalArgumentException("Les mots de passe ne correspondent pas.");
        }
        if (usersDao.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Cet email est déjà utilisé.");
        }
        if (usersDao.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Ce nom d'utilisateur est déjà utilisé.");
        }
    }

    private void validateLogin(String identifier, String password) {
        if (isBlank(identifier) || isBlank(password)) {
            throw new IllegalArgumentException("Le nom d'utilisateur et le mot de passe sont obligatoires.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    private String hashPasswordLegacy(String password) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hashed);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("Impossible de chiffrer le mot de passe.", e);
        }
    }

    private boolean isBcryptHash(String hash) {
        return hash != null && (hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$"));
    }
}
