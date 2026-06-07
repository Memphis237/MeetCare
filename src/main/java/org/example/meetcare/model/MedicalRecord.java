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
    private Integer sharedDoctorId;
    private String sharedDoctorName;
    private String patientName;
    private boolean shareConsent;
    private String prescriptionDiagnosis;
    private String prescriptionTreatment;
    private String prescriptionInstructions;
    private String prescriptionValidity;

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

    public Integer getSharedDoctorId() {
        return sharedDoctorId;
    }

    public void setSharedDoctorId(Integer sharedDoctorId) {
        this.sharedDoctorId = sharedDoctorId;
    }

    public String getSharedDoctorName() {
        return sharedDoctorName;
    }

    public void setSharedDoctorName(String sharedDoctorName) {
        this.sharedDoctorName = sharedDoctorName;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public boolean isShareConsent() {
        return shareConsent;
    }

    public void setShareConsent(boolean shareConsent) {
        this.shareConsent = shareConsent;
    }

    public String getPrescriptionDiagnosis() {
        return prescriptionDiagnosis;
    }

    public void setPrescriptionDiagnosis(String prescriptionDiagnosis) {
        this.prescriptionDiagnosis = prescriptionDiagnosis;
    }

    public String getPrescriptionTreatment() {
        return prescriptionTreatment;
    }

    public void setPrescriptionTreatment(String prescriptionTreatment) {
        this.prescriptionTreatment = prescriptionTreatment;
    }

    public String getPrescriptionInstructions() {
        return prescriptionInstructions;
    }

    public void setPrescriptionInstructions(String prescriptionInstructions) {
        this.prescriptionInstructions = prescriptionInstructions;
    }

    public String getPrescriptionValidity() {
        return prescriptionValidity;
    }

    public void setPrescriptionValidity(String prescriptionValidity) {
        this.prescriptionValidity = prescriptionValidity;
    }
}
