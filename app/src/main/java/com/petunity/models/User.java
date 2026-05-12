package com.petunity.models;

public class User {
    private String id;
    private String name;
    private String profileImageUrl;
    private boolean online;

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
}
