package com.petunity.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PetProfile implements Serializable {
    private String id;
    private String ownerId;
    private String ownerName;
    private String name;
    private String breed;
    private String location; // Added location field
    private int age;
    private double weight;
    private String size; 
    private String imageUrl;
    private List<String> vibeTags = new ArrayList<>();
    private boolean afraidOfLargeBreeds;
    private boolean requiresVaccinated;
    private boolean isVaccinated;

    public PetProfile() {}

    public PetProfile(String name, String breed, int age, String size) {
        this.name = name;
        this.breed = breed;
        this.age = age;
        this.size = size;
    }

    public int calculateMatchScore(PetProfile other) {
        if (other == null) return 0;
        if (this.requiresVaccinated && !other.isVaccinated) return 0;
        if (other.requiresVaccinated && !this.isVaccinated) return 0;
        if (this.afraidOfLargeBreeds && "Large".equals(other.getSize())) return 10;
        if (other.afraidOfLargeBreeds && "Large".equals(this.getSize())) return 10;

        int score = 50; 
        for (String tag : this.vibeTags) {
            if (other.vibeTags.contains(tag)) {
                score += 15;
            }
        }
        if (this.vibeTags.contains("High Energy") && other.vibeTags.contains("Playful")) score += 10;
        return Math.min(score, 100);
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBreed() { return breed; }
    public void setBreed(String breed) { this.breed = breed; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public List<String> getVibeTags() { return vibeTags; }
    public void setVibeTags(List<String> vibeTags) { this.vibeTags = vibeTags; }
    public boolean isAfraidOfLargeBreeds() { return afraidOfLargeBreeds; }
    public void setAfraidOfLargeBreeds(boolean afraidOfLargeBreeds) { this.afraidOfLargeBreeds = afraidOfLargeBreeds; }
    public boolean isRequiresVaccinated() { return requiresVaccinated; }
    public void setRequiresVaccinated(boolean requiresVaccinated) { this.requiresVaccinated = requiresVaccinated; }
    public boolean isVaccinated() { return isVaccinated; }
    public void setVaccinated(boolean vaccinated) { isVaccinated = vaccinated; }
}
