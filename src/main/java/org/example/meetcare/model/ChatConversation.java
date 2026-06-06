package org.example.meetcare.model;

import java.time.LocalDateTime;

public class ChatConversation {
    private int idConversation;
    private Users otherUser;
    private String lastMessagePreview;
    private LocalDateTime lastMessageAt;
    private int unreadCount;
    private boolean typing;
    private LocalDateTime typingAt;

    public int getIdConversation() {
        return idConversation;
    }

    public void setIdConversation(int idConversation) {
        this.idConversation = idConversation;
    }

    public Users getOtherUser() {
        return otherUser;
    }

    public void setOtherUser(Users otherUser) {
        this.otherUser = otherUser;
    }

    public String getLastMessagePreview() {
        return lastMessagePreview;
    }

    public void setLastMessagePreview(String lastMessagePreview) {
        this.lastMessagePreview = lastMessagePreview;
    }

    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(LocalDateTime lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public boolean isTyping() {
        return typing;
    }

    public void setTyping(boolean typing) {
        this.typing = typing;
    }

    public LocalDateTime getTypingAt() {
        return typingAt;
    }

    public void setTypingAt(LocalDateTime typingAt) {
        this.typingAt = typingAt;
    }
}
