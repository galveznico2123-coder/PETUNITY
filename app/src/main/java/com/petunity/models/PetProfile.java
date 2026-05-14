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
    private String location;
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

    /**
     * Calculates a compatibility score between 0 and 100.
     * Diversity in scores is maintained to show a clear hierarchy.
     */
    public int calculateMatchScore(PetProfile other) {
        if (other == null) return 0;
        
        // Start with a base compatibility
        int score = 30; 

        // 1. Breed Match (Bonus +20)
        if (this.breed != null && other.getBreed() != null && this.breed.equalsIgnoreCase(other.getBreed())) {
            score += 20;
        }

        // 2. Size Match (Bonus +15)
        if (this.size != null && other.getSize() != null && this.size.equalsIgnoreCase(other.getSize())) {
            score += 15;
        }

        // 3. Vibe Tags Match (+10 per shared tag, max +30)
        int sharedTags = 0;
        if (this.vibeTags != null && other.getVibeTags() != null) {
            for (String tag : this.vibeTags) {
                if (other.getVibeTags().contains(tag)) {
                    sharedTags++;
                }
            }
        }
        score += Math.min(sharedTags * 10, 30);

        // 4. Age Proximity (+5 if same life stage)
        if (Math.abs(this.age - other.getAge()) <= 2) {
            score += 5;
        }

        // 5. Dealbreakers (Heavy deductions instead of filtering, to keep them in the list)
        if (this.requiresVaccinated && !other.isVaccinated()) {
            score -= 40;
        }
        if (other.isRequiresVaccinated() && !this.isVaccinated) {
            score -= 40;
        }
        if (this.afraidOfLargeBreeds && "Large".equalsIgnoreCase(other.getSize())) {
            score -= 30;
        }

        // Clamp between 0 and 100
        return Math.max(0, Math.min(score, 100));
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
