package com.petunity.activities;
public class PetListing {
    private String name;
    private String breed;
    private String location;
    private String timeAgo;
    private boolean isLost;

    public PetListing(String name, String breed, String location, String timeAgo, boolean isLost) {
        this.name = name;
        this.breed = breed;
        this.location = location;
        this.timeAgo = timeAgo;
        this.isLost = isLost;
    }

    public String getName() { return name; }
    public String getBreed() { return breed; }
    public String getLocation() { return location; }
    public String getTimeAgo() { return timeAgo; }
    public boolean isLost() { return isLost; }
}
