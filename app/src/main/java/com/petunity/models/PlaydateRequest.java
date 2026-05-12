package com.petunity.models;

import java.io.Serializable;

public class PlaydateRequest implements Serializable {
    private String id;
    private String senderId;
    private String receiverId;
    private String senderPetName;
    private String receiverPetName;
    private String suggestedTime;
    private String suggestedLocation;
    private String status; // Pending, Accepted, Declined
    private long timestamp;

    public PlaydateRequest() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }
    public String getSenderPetName() { return senderPetName; }
    public void setSenderPetName(String senderPetName) { this.senderPetName = senderPetName; }
    public String getReceiverPetName() { return receiverPetName; }
    public void setReceiverPetName(String receiverPetName) { this.receiverPetName = receiverPetName; }
    public String getSuggestedTime() { return suggestedTime; }
    public void setSuggestedTime(String suggestedTime) { this.suggestedTime = suggestedTime; }
    public String getSuggestedLocation() { return suggestedLocation; }
    public void setSuggestedLocation(String suggestedLocation) { this.suggestedLocation = suggestedLocation; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
