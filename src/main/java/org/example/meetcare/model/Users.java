package org.example.meetcare.model;

import java.time.LocalDateTime;

public class Users {
    private int idUsers;
    private String username;
    private String email;
    private String password;
    private String phone;
    private String avatar;
    private Role role;
    private String city;
    private String codePostal;
    private boolean online;
    private LocalDateTime lastSeen;
    private LocalDateTime date;

    public Users() {
        this.role = Role.USER;
    }

    public Users(int idUsers, String username, String email, String password, String phone, String avatar, Role role,
                 String city, String codePostal, boolean online, LocalDateTime lastSeen, LocalDateTime date) {
        this.idUsers = idUsers;
        this.username = username;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.avatar = avatar;
        this.role = role;
        this.city = city;
        this.codePostal = codePostal;
        this.online = online;
        this.lastSeen = lastSeen;
        this.date = date;
    }

    public int getIdUsers() {
        return idUsers;
    }

    public void setIdUsers(int idUsers) {
        this.idUsers = idUsers;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCodePostal() {
        return codePostal;
    }

    public void setCodePostal(String codePostal) {
        this.codePostal = codePostal;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public LocalDateTime getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(LocalDateTime lastSeen) {
        this.lastSeen = lastSeen;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }
}
