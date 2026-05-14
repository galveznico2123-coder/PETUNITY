package com.petunity.models;

import com.google.firebase.Timestamp;

public class User {
    private String id;
    private String name;
    private String profileImageUrl;
    private boolean online;
    private Timestamp lastActive;

    public User() {}

    public User(String id, String name, String profileImageUrl, boolean online) {
        this.id = id;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
        this.online = online;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }

    public Timestamp getLastActive() { return lastActive; }
    public void setLastActive(Timestamp lastActive) { this.lastActive = lastActive; }
}
