package org.example.meetcare.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ChatMessage {
    private int idMessage;
    private int conversationId;
    private int senderId;
    private String content;
    private String messageType;
    private boolean read;
    private LocalDateTime sentAt;
    private final List<ChatMessageAttachment> attachments = new ArrayList<>();

    public int getIdMessage() {
        return idMessage;
    }

    public void setIdMessage(int idMessage) {
        this.idMessage = idMessage;
    }

    public int getConversationId() {
        return conversationId;
    }

    public void setConversationId(int conversationId) {
        this.conversationId = conversationId;
    }

    public int getSenderId() {
        return senderId;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public List<ChatMessageAttachment> getAttachments() {
        return attachments;
    }
}
