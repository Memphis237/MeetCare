package org.example.meetcare.utils;

import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import org.example.meetcare.dao.UsersDao;
import org.example.meetcare.model.Users;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class AvatarUtil {
    private static final String DEFAULT_FILL = "#B8D4EE";
    private static final String DEFAULT_TEXT_FILL = "#2B7FD4";

    private AvatarUtil() {
    }

    public static void applyAvatar(Circle circle, Label initialsLabel, Users user) {
        if (circle == null || initialsLabel == null) {
            return;
        }

        Users freshUser = refreshUser(user);
        String avatarPath = freshUser == null ? null : freshUser.getAvatar();
        if (!isBlank(avatarPath)) {
            File avatarFile = resolveAvatarFile(avatarPath);
            if (avatarFile.exists()) {
                Image image = loadImage(avatarFile);
                if (image != null && !image.isError()) {
                    circle.setFill(new ImagePattern(image));
                    initialsLabel.setVisible(false);
                    initialsLabel.setManaged(false);
                    return;
                }
            }
        }

        circle.setFill(Paint.valueOf(DEFAULT_FILL));
        initialsLabel.setVisible(true);
        initialsLabel.setManaged(true);
        initialsLabel.setText(buildInitials(freshUser));
        initialsLabel.setTextFill(Paint.valueOf(DEFAULT_TEXT_FILL));
    }

    public static Users refreshUser(Users user) {
        if (user == null || user.getIdUsers() <= 0) {
            return user;
        }

        return new UsersDao().findById(user.getIdUsers()).orElse(user);
    }

    public static String buildInitials(Users user) {
        if (user == null) {
            return "U";
        }

        String first = firstLetter(user.getPrenom());
        String second = firstLetter(user.getUsername());

        if (first.isBlank() && second.isBlank()) {
            return "U";
        }
        if (first.isBlank()) {
            return second;
        }
        if (second.isBlank()) {
            return first;
        }
        return first + second;
    }

    private static String firstLetter(String value) {
        if (isBlank(value)) {
            return "";
        }
        return value.trim().substring(0, 1).toUpperCase();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static File resolveAvatarFile(String avatarPath) {
        Path path = Paths.get(avatarPath);
        if (!path.isAbsolute()) {
            return Paths.get(System.getProperty("user.dir"), avatarPath).toFile();
        }
        return path.toFile();
    }

    private static Image loadImage(File avatarFile) {
        try (FileInputStream inputStream = new FileInputStream(avatarFile)) {
            return new Image(inputStream);
        } catch (IOException e) {
            return null;
        }
    }
}
