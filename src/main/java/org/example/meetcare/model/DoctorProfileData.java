package org.example.meetcare.model;

import java.util.List;
import java.util.Locale;

public class DoctorProfileData {
    private final int doctorId;
    private final String name;
    private final String email;
    private final String speciality;
    private final String city;
    private final String address;
    private final String sector;
    private final String price;
    private final boolean acceptsVitale;
    private final boolean teleconsultation;
    private final String rating;
    private final int experience;
    private final String biography;
    private final List<AvailabilitySlot> availableSlots;

    public DoctorProfileData(int doctorId, String name, String email, String speciality, String city, String address,
                             String sector, String price, boolean acceptsVitale, boolean teleconsultation,
                             String rating, int experience, String biography, List<AvailabilitySlot> availableSlots) {
        this.doctorId = doctorId;
        this.name = name;
        this.email = email;
        this.speciality = speciality;
        this.city = city;
        this.address = address;
        this.sector = sector;
        this.price = price;
        this.acceptsVitale = acceptsVitale;
        this.teleconsultation = teleconsultation;
        this.rating = rating;
        this.experience = experience;
        this.biography = biography;
        this.availableSlots = availableSlots;
    }

    public int getDoctorId() { return doctorId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getSpeciality() { return speciality; }
    public String getCity() { return city; }
    public String getAddress() { return address; }
    public String getSector() { return sector; }
    public String getPrice() { return price; }
    public boolean isAcceptsVitale() { return acceptsVitale; }
    public boolean isTeleconsultation() { return teleconsultation; }
    public String getRating() { return rating; }
    public int getExperience() { return experience; }
    public String getBiography() { return biography; }
    public List<AvailabilitySlot> getAvailableSlots() { return availableSlots; }

    public String getAvatarInitial() {
        String cleanName = name == null ? "" : name.replace("Dr. ", "").trim();
        if (cleanName.isEmpty()) {
            return "D";
        }
        return cleanName.substring(0, 1).toUpperCase(Locale.ROOT);
    }
}
