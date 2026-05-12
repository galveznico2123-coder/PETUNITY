package com.petunity.activities;

public class Conversation {
    private String name;
    private String lastMessage;
    private String time;
    private String avatarUrl;

    public Conversation(String name, String lastMessage, String time, String avatarUrl) {
        this.name = name;
        this.lastMessage = lastMessage;
        this.time = time;
        this.avatarUrl = avatarUrl;
    }

    public String getName() { return name; }
    public String getLastMessage() { return lastMessage; }
    public String getTime() { return time; }
    public String getAvatarUrl() { return avatarUrl; }
}
