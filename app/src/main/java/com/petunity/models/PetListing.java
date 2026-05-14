package com.petunity.models;

import com.google.firebase.Timestamp;
import java.io.Serializable;

public class PetListing implements Serializable {
    public String name;
    public String breed;
    public String location;
    public String timeAgo;
    public String status; 
    public String ownerName;
    public String userId; 
    public String imageUrl;
    public String linkedPostId; 
    public Timestamp timestamp;
    public String description; // Added for detailed report
    public String reward;      // Added for lost pet reports
    public String contactPhone; // Added for direct contact

    public PetListing() {} 

    public PetListing(String name, String breed, String location, String timeAgo, String status, String ownerName, String userId, String imageUrl) {
        this.name = name;
        this.breed = breed;
        this.location = location;
        this.timeAgo = timeAgo;
        this.status = status;
        this.ownerName = ownerName;
        this.userId = userId;
        this.imageUrl = imageUrl;
        this.timestamp = Timestamp.now();
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
    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getReward() { return reward; }
    public void setReward(String reward) { this.reward = reward; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
}
