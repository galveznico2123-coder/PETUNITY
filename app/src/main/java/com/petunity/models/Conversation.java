package com.petunity.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import com.petunity.utils.PetTimeUtils;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Conversation {
    private String otherUserId;
    private String name;
    private String lastMessage;
    private String time;
    private String avatarUrl;
    private List<String> participants;
    private Object lastTimestamp;
    private Map<String, String> names;
    private String lastSenderId;
    private String lastSenderName;
    private boolean online;
    private Map<String, Boolean> readStatus;

    public Conversation() {}

    public Map<String, Boolean> getReadStatus() { return readStatus; }
    public void setReadStatus(Map<String, Boolean> readStatus) { this.readStatus = readStatus; }

    public boolean isUnread(String currentUserId) {
        if (readStatus == null || !readStatus.containsKey(currentUserId)) {
            // If we're not the last sender, it might be unread
            return lastSenderId != null && !lastSenderId.equals(currentUserId);
        }
        return Boolean.FALSE.equals(readStatus.get(currentUserId));
    }

    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }

    public String getOtherUserId() { return otherUserId; }
    public void setOtherUserId(String otherUserId) { this.otherUserId = otherUserId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public String getTime() {
        if (time != null) return time;
        Timestamp ts = getLastTimestampAsDate();
        if (ts != null) {
            return PetTimeUtils.getTimeAgo(ts.toDate());
        }
        return "";
    }
    public void setTime(String time) { this.time = time; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public List<String> getParticipants() { return participants; }
    public void setParticipants(List<String> participants) { this.participants = participants; }

    public Object getLastTimestamp() { return lastTimestamp; }
    public void setLastTimestamp(Object lastTimestamp) { this.lastTimestamp = lastTimestamp; }

    public Map<String, String> getNames() { return names; }
    public void setNames(Map<String, String> names) { this.names = names; }

    public String getLastSenderId() { return lastSenderId; }
    public void setLastSenderId(String lastSenderId) { this.lastSenderId = lastSenderId; }

    public String getLastSenderName() { return lastSenderName; }
    public void setLastSenderName(String lastSenderName) { this.lastSenderName = lastSenderName; }

    @Exclude
    public Timestamp getLastTimestampAsDate() {
        if (lastTimestamp instanceof Timestamp) {
            return (Timestamp) lastTimestamp;
        } else if (lastTimestamp instanceof Long) {
            return new Timestamp(new Date((Long) lastTimestamp));
        }
        return null;
    }
}
