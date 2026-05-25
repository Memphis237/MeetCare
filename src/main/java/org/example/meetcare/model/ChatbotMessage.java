package org.example.meetcare.model;

import java.time.LocalDateTime;

public class ChatbotMessage {
    private int idChatbotMessages;
    private String question;
    private String response;
    private String model;
    private LocalDateTime date;
    private int conversationId;
    private int userId;

    public int getIdChatbotMessages() {
        return idChatbotMessages;
    }

    public void setIdChatbotMessages(int idChatbotMessages) {
        this.idChatbotMessages = idChatbotMessages;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public int getConversationId() {
        return conversationId;
    }

    public void setConversationId(int conversationId) {
        this.conversationId = conversationId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }
}
