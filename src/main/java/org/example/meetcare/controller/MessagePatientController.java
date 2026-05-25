package org.example.meetcare.controller;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import org.example.meetcare.dao.ChatbotMessageDao;
import org.example.meetcare.dao.UsersDao;
import org.example.meetcare.model.ChatbotMessage;
import org.example.meetcare.model.Users;
import org.example.meetcare.service.GroqChatService;
import org.example.meetcare.utils.AppSession;
import org.example.meetcare.utils.LogoutUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MessagePatientController {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final ChatbotMessageDao chatbotMessageDao = new ChatbotMessageDao();
    private final GroqChatService groqChatService = new GroqChatService();
    private final List<ChatbotMessage> history = new ArrayList<>();

    private Users currentUser;
    private int conversationId;

    @FXML
    private VBox messagesContainer;

    @FXML
    private TextField messageInput;

    @FXML
    private Button sendButton;

    @FXML
    private Label chatTitleLabel;

    @FXML
    private Label chatSubtitleLabel;

    @FXML
    private Label assistantStatusLabel;

    @FXML
    private Circle assistantAvatarCircle;

    @FXML
    private Label assistantAvatarLabel;

    @FXML
    private Circle assistantHeaderCircle;

    @FXML
    private Label assistantHeaderLabel;

    @FXML
    private void initialize() {
        currentUser = refreshCurrentUser();
        if (currentUser == null) {
            chatTitleLabel.setText("Assistant IA");
            chatSubtitleLabel.setText("Connexion requise");
            assistantStatusLabel.setText("Hors ligne");
            addSystemMessage("Connecte-toi pour utiliser l'assistant IA.");
            setInputEnabled(false);
            return;
        }

        conversationId = chatbotMessageDao.ensureConversationForUser(currentUser.getIdUsers());
        configureAssistantHeader(currentUser);
        loadHistory();

        if (history.isEmpty()) {
            addSystemMessage("Bonjour " + buildDisplayName(currentUser) + ", comment puis-je vous aider aujourd'hui ?");
        }
    }

    @FXML
    private void handleSendMessage() {
        sendCurrentMessage();
    }

    @FXML
    private void handleSendMessageViaClick() {
        sendCurrentMessage();
    }

    @FXML
    private void handleMessages(MouseEvent event) {
        if (messageInput != null) {
            messageInput.requestFocus();
        }
        scrollToBottom();
    }

    @FXML
    private void handleLogout(MouseEvent event) {
        LogoutUtil.logout((Node) event.getSource());
    }

    private void sendCurrentMessage() {
        if (currentUser == null) {
            addSystemMessage("Tu dois etre connecte pour envoyer un message.");
            return;
        }

        String question = messageInput.getText() == null ? "" : messageInput.getText().trim();
        if (question.isBlank()) {
            return;
        }

        messageInput.clear();
        appendUserBubble(question, LocalDateTime.now());
        setInputEnabled(false);

        List<ChatbotMessage> historySnapshot = new ArrayList<>(history);
        Task<ChatbotMessage> task = new Task<>() {
            @Override
            protected ChatbotMessage call() {
                String response;
                try {
                    response = groqChatService.generateReply(question, historySnapshot);
                } catch (Exception ex) {
                    response = "Je ne peux pas joindre l'assistant IA pour le moment. " + rootCauseMessage(ex);
                }

                return chatbotMessageDao.saveExchange(
                        conversationId,
                        currentUser.getIdUsers(),
                        question,
                        response,
                        groqChatService.getModelName()
                );
            }
        };

        task.setOnSucceeded(event -> {
            ChatbotMessage exchange = task.getValue();
            history.add(exchange);
            appendAssistantBubble(exchange.getResponse(), exchange.getDate() == null ? LocalDateTime.now() : exchange.getDate());
            setInputEnabled(true);
            messageInput.requestFocus();
        });

        task.setOnFailed(event -> {
            addSystemMessage("Erreur lors de l'enregistrement du message IA.");
            setInputEnabled(true);
        });

        Thread thread = new Thread(task, "groq-chat-request");
        thread.setDaemon(true);
        thread.start();
    }

    private void loadHistory() {
        history.clear();
        history.addAll(chatbotMessageDao.findMessagesByConversationAndUser(conversationId, currentUser.getIdUsers()));
        messagesContainer.getChildren().clear();

        if (history.isEmpty()) {
            return;
        }

        for (ChatbotMessage exchange : history) {
            appendUserBubble(exchange.getQuestion(), exchange.getDate());
            appendAssistantBubble(exchange.getResponse(), exchange.getDate());
        }

        scrollToBottom();
    }

    private void configureAssistantHeader(Users user) {
        String displayName = buildDisplayName(user);
        chatTitleLabel.setText("Assistant IA");
        chatSubtitleLabel.setText(displayName + " - conversation persistante");
        assistantStatusLabel.setText("En ligne");
        assistantAvatarLabel.setText("AI");
        assistantHeaderLabel.setText("AI");
        assistantAvatarCircle.setStyle("-fx-fill: #CBE3F8;");
        assistantHeaderCircle.setStyle("-fx-fill: #CBE3F8;");
    }

    private void appendUserBubble(String text, LocalDateTime timestamp) {
        HBox wrapper = new HBox();
        wrapper.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        VBox bubble = new VBox(4);
        bubble.setMaxWidth(420);
        bubble.setStyle("-fx-alignment: center-right;");

        Label messageLabel = new Label(text);
        messageLabel.setWrapText(true);
        messageLabel.setStyle("-fx-background-color: #2B7FD4; -fx-text-fill: white; -fx-font-size: 13px; "
                + "-fx-background-radius: 14 14 4 14; -fx-padding: 12 16 12 16;");

        Label timeLabel = new Label(formatTime(timestamp));
        timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #94A3B8;");

        bubble.getChildren().addAll(messageLabel, timeLabel);
        wrapper.getChildren().add(bubble);
        messagesContainer.getChildren().add(wrapper);
        scrollToBottom();
    }

    private void appendAssistantBubble(String text, LocalDateTime timestamp) {
        HBox wrapper = new HBox(10);
        wrapper.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        StackPane avatar = new StackPane();
        Circle circle = new Circle(18);
        circle.setStyle("-fx-fill: #CBE3F8;");
        Label initials = new Label("AI");
        initials.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #2B7FD4;");
        avatar.getChildren().addAll(circle, initials);

        VBox bubble = new VBox(4);
        bubble.setMaxWidth(420);

        Label messageLabel = new Label(text);
        messageLabel.setWrapText(true);
        messageLabel.setStyle("-fx-background-color: white; -fx-text-fill: #1A2C42; -fx-font-size: 13px; "
                + "-fx-background-radius: 14 14 14 4; -fx-padding: 12 16 12 16; "
                + "-fx-effect: dropshadow(gaussian, #d8e4f0, 4, 0, 0, 1);");

        Label timeLabel = new Label(formatTime(timestamp));
        timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #94A3B8;");

        bubble.getChildren().addAll(messageLabel, timeLabel);
        wrapper.getChildren().addAll(avatar, bubble);
        messagesContainer.getChildren().add(wrapper);
        scrollToBottom();
    }

    private void addSystemMessage(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle("-fx-background-color: #E8F1FB; -fx-text-fill: #1A2C42; -fx-font-size: 13px; "
                + "-fx-background-radius: 12; -fx-padding: 10 14 10 14;");
        HBox wrapper = new HBox(label);
        wrapper.setAlignment(javafx.geometry.Pos.CENTER);
        messagesContainer.getChildren().add(wrapper);
        scrollToBottom();
    }

    private void setInputEnabled(boolean enabled) {
        messageInput.setDisable(!enabled);
        sendButton.setDisable(!enabled);
    }

    private void scrollToBottom() {
        Platform.runLater(() -> {
            ScrollPane scrollPane = findParentScrollPane(messagesContainer);
            if (scrollPane != null) {
                scrollPane.setVvalue(1.0);
            }
        });
    }

    private ScrollPane findParentScrollPane(Node node) {
        Node current = node;
        while (current != null) {
            current = current.getParent();
            if (current instanceof ScrollPane scrollPane) {
                return scrollPane;
            }
        }
        return null;
    }

    private Users refreshCurrentUser() {
        Users user = AppSession.getCurrentUser();
        if (user == null || user.getIdUsers() <= 0) {
            return user;
        }

        Users freshUser = new UsersDao().findById(user.getIdUsers()).orElse(user);
        AppSession.setCurrentUser(freshUser);
        return freshUser;
    }

    private String buildDisplayName(Users user) {
        String prenom = user.getPrenom() == null ? "" : user.getPrenom().trim();
        String username = user.getUsername() == null ? "" : user.getUsername().trim();

        if (prenom.isEmpty() && username.isEmpty()) {
            return "Patient";
        }
        if (prenom.isEmpty()) {
            return username;
        }
        if (username.isEmpty()) {
            return prenom;
        }
        return prenom + " " + username;
    }

    private String formatTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(TIME_FORMATTER);
    }

    private String rootCauseMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        if (message == null || message.isBlank()) {
            return current.getClass().getSimpleName();
        }
        return message;
    }
}
