package com.petunity.models;

import java.io.Serializable;

public class PetListing implements Serializable {
    public String name;
    public String breed;
    public String location;
    public String timeAgo;
    public String status; // Using String "lost" or "found"
    public String ownerName;
    public String userId; // Unified field name
    public String imageUrl;
    public String linkedPostId; // Added to link with Alert posts

    public PetListing() {} // Required for Firestore

    public PetListing(String name, String breed, String location, String timeAgo, String status, String ownerName, String userId, String imageUrl) {
        this.name = name;
        this.breed = breed;
        this.location = location;
        this.timeAgo = timeAgo;
        this.status = status;
        this.ownerName = ownerName;
        this.userId = userId;
        this.imageUrl = imageUrl;
    }

    public String getName() { return name; }
    public String getBreed() { return breed; }
    public String getLocation() { return location; }
    public String getTimeAgo() { return timeAgo; }
    public String getStatus() { return status; }
    public String getOwnerName() { return ownerName; }
    public String getUserId() { return userId; }
    public String getOwnerId() { return userId; }
    public String getImageUrl() { return imageUrl; }
    public String getAvatarUrl() { return imageUrl; }
    public String getLinkedPostId() { return linkedPostId; }
    public void setLinkedPostId(String linkedPostId) { this.linkedPostId = linkedPostId; }
}
