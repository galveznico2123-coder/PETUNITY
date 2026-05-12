package com.petunity.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;

public class Message {
    private String id;
    private String senderId;
    private String receiverId;
    private String conversationId;
    private String text;
    private Object timestamp; // Changed to Object for safety
    private boolean isSentByMe;

    public Message() {}

    public Message(String senderId, String receiverId, String text, long timestamp) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.text = text;
        this.timestamp = timestamp;
        this.conversationId = senderId.compareTo(receiverId) < 0 
                ? senderId + "_" + receiverId 
                : receiverId + "_" + senderId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }
    
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public Object getTimestamp() { return timestamp; }
    public void setTimestamp(Object timestamp) { this.timestamp = timestamp; }

    @Exclude
    public long getTimestampAsLong() {
        if (timestamp instanceof Long) {
            return (Long) timestamp;
        } else if (timestamp instanceof Timestamp) {
            return ((Timestamp) timestamp).toDate().getTime();
        }
        return System.currentTimeMillis();
    }

    @Exclude
    public boolean isSentByMe() { return isSentByMe; }
    @Exclude
    public void setSentByMe(boolean sentByMe) { isSentByMe = sentByMe; }
}
