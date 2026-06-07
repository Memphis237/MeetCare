package org.example.meetcare.model;

public class DoctorAdminEntry {
    private final int userId;
    private final int doctorId;
    private final String prenom;
    private final String username;
    private final String email;
    private final String phone;
    private final String city;
    private final String address;
    private final String speciality;
    private final String doctorAddress;
    private final Integer experience;
    private final Double consultationPrice;
    private final String biography;
    private final String sector;
    private final boolean teleconsultation;
    private final boolean acceptsVitale;
    private final boolean publicProfileCompleted;
    private final AccountStatus accountStatus;

    public DoctorAdminEntry(int userId,
                            int doctorId,
                            String prenom,
                            String username,
                            String email,
                            String phone,
                            String city,
                            String address,
                            String speciality,
                            String doctorAddress,
                            Integer experience,
                            Double consultationPrice,
                            String biography,
                            String sector,
                            boolean teleconsultation,
                            boolean acceptsVitale,
                            boolean publicProfileCompleted,
                            AccountStatus accountStatus) {
        this.userId = userId;
        this.doctorId = doctorId;
        this.prenom = prenom;
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.city = city;
        this.address = address;
        this.speciality = speciality;
        this.doctorAddress = doctorAddress;
        this.experience = experience;
        this.consultationPrice = consultationPrice;
        this.biography = biography;
        this.sector = sector;
        this.teleconsultation = teleconsultation;
        this.acceptsVitale = acceptsVitale;
        this.publicProfileCompleted = publicProfileCompleted;
        this.accountStatus = accountStatus;
    }

    public int getUserId() {
        return userId;
    }

    public int getDoctorId() {
        return doctorId;
    }

    public String getPrenom() {
        return prenom;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getCity() {
        return city;
    }

    public String getAddress() {
        return address;
    }

    public String getSpeciality() {
        return speciality;
    }

    public String getDoctorAddress() {
        return doctorAddress;
    }

    public Integer getExperience() {
        return experience;
    }

    public Double getConsultationPrice() {
        return consultationPrice;
    }

    public String getBiography() {
        return biography;
    }

    public String getSector() {
        return sector;
    }

    public boolean isTeleconsultation() {
        return teleconsultation;
    }

    public boolean isAcceptsVitale() {
        return acceptsVitale;
    }

    public boolean isPublicProfileCompleted() {
        return publicProfileCompleted;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }
}
