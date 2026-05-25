package org.example.meetcare.model;

public class Doctor {
    private int id;
    private String username;
    private String email;
    private String city;
    private String speciality;
    private String address;
    private String sector;
    private double price;
    private int experience;
    private String biography;
    private boolean acceptsVitale;
    private boolean teleconsultation;

    public Doctor(int id, String username, String email, String city, String speciality,
                  String address, String sector, double price, int experience, String biography,
                  boolean acceptsVitale, boolean teleconsultation) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.city = city;
        this.speciality = speciality;
        this.address = address;
        this.sector = sector;
        this.price = price;
        this.experience = experience;
        this.biography = biography;
        this.acceptsVitale = acceptsVitale;
        this.teleconsultation = teleconsultation;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getCity() { return city; }
    public String getSpeciality() { return speciality; }
    public String getAddress() { return address; }
    public String getSector() { return sector; }
    public double getPrice() { return price; }
    public int getExperience() { return experience; }
    public String getBiography() { return biography; }
    public boolean isAcceptsVitale() { return acceptsVitale; }
    public boolean isTeleconsultation() { return teleconsultation; }
}
