package org.example.meetcare.model;

import java.time.LocalDateTime;

public class MedicalRecord {
    private int id;
    private String title;
    private String description;
    private String documentType;
    private String filePath;
    private LocalDateTime date;
    private Integer idDoctors;
    private int idUsers;

    public MedicalRecord() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public Integer getIdDoctors() {
        return idDoctors;
    }

    public void setIdDoctors(Integer idDoctors) {
        this.idDoctors = idDoctors;
    }

    public int getIdUsers() {
        return idUsers;
    }

    public void setIdUsers(int idUsers) {
        this.idUsers = idUsers;
    }
}
