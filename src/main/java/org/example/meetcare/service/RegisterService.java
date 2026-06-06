package org.example.meetcare.service;

import org.example.meetcare.dao.UsersDao;
import org.example.meetcare.dao.DoctorDao;
import org.example.meetcare.model.AccountStatus;
import org.example.meetcare.model.Role;
import org.example.meetcare.model.Users;
import org.example.meetcare.utils.PasswordUtil;

public class RegisterService {
    private final UsersDao usersDao = new UsersDao();
    private final DoctorDao doctorDao = new DoctorDao();

    public Users register(Users user, String confirmPassword) {
        validateRegistration(user, confirmPassword);

        user.setPassword(PasswordUtil.hash(user.getPassword()));
        if (user.getRole() == null) {
            user.setRole(Role.USER);
        }
        user.setAccountStatus(user.getRole() == Role.MEDECIN ? AccountStatus.PENDING : AccountStatus.VERIFIED);
        user.setOnline(false);
        user.setAvatar(null);
        user.setIdUsers(usersDao.insert(user));
        if (user.getRole() == Role.MEDECIN) {
            doctorDao.ensureDoctorIdForUser(user.getIdUsers());
        }
        return user;
    }

    public Users login(String identifier, String password) {
        validateLogin(identifier, password);
        Users user = usersDao.findByIdentifier(identifier)
                .orElseThrow(() -> new IllegalArgumentException("Identifiants incorrects."));

        if (PasswordUtil.matches(password, user.getPassword())) {
            if (!PasswordUtil.isBcryptHash(user.getPassword())) {
                user.setPassword(PasswordUtil.hash(password));
            }
            return user;
        }

        throw new IllegalArgumentException("Identifiants incorrects.");
    }

    private void validateRegistration(Users user, String confirmPassword) {
        if (user == null) {
            throw new IllegalArgumentException("Le formulaire d'inscription est invalide.");
        }
        if (isBlank(user.getPrenom())
                || isBlank(user.getUsername())
                || isBlank(user.getEmail())
                || isBlank(user.getPassword())
                || isBlank(user.getAddress())
                || isBlank(user.getCity())
                || isBlank(user.getCodePostal())
                || isBlank(user.getCountry())) {
            throw new IllegalArgumentException("Tous les champs d'identité et d'adresse sont obligatoires.");
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
}
