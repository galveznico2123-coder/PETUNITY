package com.petunity.models;

import android.net.Uri;
import com.google.firebase.Timestamp;
import java.io.Serializable;
import java.util.List;

public class Post implements Serializable {
    private String id;
    private String userId;
    private String userName;
    private String userProfileImageUrl;
    private String userMembershipLevel; // Added field for rank badge
    private String timeLabel; 
    private Timestamp timestamp;
    private String title;
    private String content;
    private String imageUrl; 
    private Uri imageUri;    
    private boolean isSelf;
    private boolean urgent; 
    private String sourcePostId; 
    private boolean isPrivate; 
    private List<String> participants; 
    private boolean saved;

    public Post() {} 

    public Post(String userName, String timeLabel, String title, String content) {
        this.userName = userName;
        this.timeLabel = timeLabel;
        this.title = title;
        this.content = content;
        this.timestamp = Timestamp.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getUserProfileImageUrl() { return userProfileImageUrl; }
    public void setUserProfileImageUrl(String userProfileImageUrl) { this.userProfileImageUrl = userProfileImageUrl; }
    public String getUserMembershipLevel() { return userMembershipLevel; }
    public void setUserMembershipLevel(String userMembershipLevel) { this.userMembershipLevel = userMembershipLevel; }
    public String getTimeLabel() { return timeLabel; }
    public void setTimeLabel(String timeLabel) { this.timeLabel = timeLabel; }
    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Uri getImageUri() { return imageUri; }
    public void setImageUri(Uri imageUri) { this.imageUri = imageUri; }
    public boolean isSelf() { return isSelf; }
    public void setSelf(boolean self) { isSelf = self; }
    public boolean isUrgent() { return urgent; }
    public void setUrgent(boolean urgent) { this.urgent = urgent; }
    public String getSourcePostId() { return sourcePostId; }
    public void setSourcePostId(String sourcePostId) { this.sourcePostId = sourcePostId; }
    public boolean isPrivate() { return isPrivate; }
    public void setPrivate(boolean isPrivate) { this.isPrivate = isPrivate; }
    public List<String> getParticipants() { return participants; }
    public void setParticipants(List<String> participants) { this.participants = participants; }
    public boolean isSaved() { return saved; }
    public void setSaved(boolean saved) { this.saved = saved; }
}
