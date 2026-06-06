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
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import org.example.meetcare.dao.ChatbotMessageDao;
import org.example.meetcare.dao.MessagingDao;
import org.example.meetcare.dao.UsersDao;
import org.example.meetcare.model.ChatbotMessage;
import org.example.meetcare.model.Users;
import org.example.meetcare.service.GroqChatService;
import org.example.meetcare.utils.AppSession;
import org.example.meetcare.utils.ChatNavigationState;
import org.example.meetcare.utils.LogoutUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public abstract class AbstractChatController {
    protected static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private static final MessagingDao.ChatContact ASSISTANT_CONTACT = new MessagingDao.ChatContact(
            -1, "Assistant IA", "Reponses automatiques", true, "AI", null
    );

    private final ChatbotMessageDao chatbotMessageDao = new ChatbotMessageDao();
    private final MessagingDao messagingDao = new MessagingDao();
    private final GroqChatService groqChatService = new GroqChatService();
    private final List<ChatbotMessage> aiHistory = new ArrayList<>();
    private List<MessagingDao.ChatContact> humanContacts = List.of();

    protected Users currentUser;
    protected int conversationId;
    protected MessagingDao.ChatContact selectedContact;
    protected boolean assistantMode = true;

    @FXML protected VBox messagesContainer;
    @FXML protected TextField messageInput;
    @FXML protected Button sendButton;
    @FXML protected Label chatTitleLabel;
    @FXML protected Label chatSubtitleLabel;
    @FXML protected Label assistantStatusLabel;
    @FXML protected Circle assistantAvatarCircle;
    @FXML protected Label assistantAvatarLabel;
    @FXML protected Circle assistantHeaderCircle;
    @FXML protected Label assistantHeaderLabel;
    @FXML protected VBox contactsContainer;
    @FXML protected ScrollPane messagesScrollPane;
    @FXML protected TextField contactsSearchField;

    protected void initializeChat() {
        currentUser = refreshCurrentUser();
        if (currentUser == null) {
            chatTitleLabel.setText("Assistant IA");
            chatSubtitleLabel.setText("Connexion requise");
            if (assistantStatusLabel != null) {
                assistantStatusLabel.setText("Hors ligne");
            }
            addSystemMessage("Connecte-toi pour utiliser la messagerie.");
            setInputEnabled(false);
            return;
        }

        if (contactsSearchField != null) {
            contactsSearchField.textProperty().addListener((obs, oldValue, newValue) -> renderContacts());
        }

        humanContacts = loadHumanContacts();
        renderContacts();

        if (ChatNavigationState.consumeAssistantRequested()) {
            selectAssistantConversation();
        } else if (!humanContacts.isEmpty()) {
            selectHumanConversation(humanContacts.get(0));
        } else {
            selectAssistantConversation();
        }
    }

    protected abstract List<MessagingDao.ChatContact> loadHumanContacts();

    @FXML
    protected void handleSendMessage() {
        sendCurrentMessage();
    }

    @FXML
    protected void handleSendMessageViaClick() {
        sendCurrentMessage();
    }

    @FXML
    protected void handleMessages(MouseEvent event) {
        selectAssistantConversation();
    }

    @FXML
    protected void handleLogout(MouseEvent event) {
        LogoutUtil.logout((Node) event.getSource());
    }

    protected void sendCurrentMessage() {
        if (currentUser == null) {
            addSystemMessage("Tu dois etre connecte pour envoyer un message.");
            return;
        }

        String text = messageInput.getText() == null ? "" : messageInput.getText().trim();
        if (text.isBlank()) {
            return;
        }

        messageInput.clear();

        if (assistantMode) {
            sendAssistantMessage(text);
        } else {
            sendHumanMessage(text);
        }
    }

    protected void renderContacts() {
        if (contactsContainer == null) {
            return;
        }

        String query = contactsSearchField == null || contactsSearchField.getText() == null
                ? ""
                : contactsSearchField.getText().trim().toLowerCase(Locale.FRENCH);

        contactsContainer.getChildren().clear();
        contactsContainer.getChildren().add(buildContactRow(ASSISTANT_CONTACT, true));

        for (MessagingDao.ChatContact contact : humanContacts) {
            if (!query.isBlank()
                    && !contact.displayName().toLowerCase(Locale.FRENCH).contains(query)
                    && !contact.subtitle().toLowerCase(Locale.FRENCH).contains(query)) {
                continue;
            }
            contactsContainer.getChildren().add(buildContactRow(contact, false));
        }
    }

    protected void selectAssistantConversation() {
        assistantMode = true;
        selectedContact = ASSISTANT_CONTACT;
        conversationId = chatbotMessageDao.ensureConversationForUser(currentUser.getIdUsers());
        aiHistory.clear();
        aiHistory.addAll(chatbotMessageDao.findMessagesByConversationAndUser(conversationId, currentUser.getIdUsers()));
        configureHeaderForContact(ASSISTANT_CONTACT, true);
        messagesContainer.getChildren().clear();

        if (aiHistory.isEmpty()) {
            addSystemMessage(getWelcomeMessage(currentUser));
        } else {
            for (ChatbotMessage exchange : aiHistory) {
                appendUserBubble(exchange.getQuestion(), exchange.getDate());
                appendAssistantBubble(exchange.getResponse(), exchange.getDate());
            }
        }

        setInputEnabled(true);
        renderContacts();
    }

    protected void selectHumanConversation(MessagingDao.ChatContact contact) {
        assistantMode = false;
        selectedContact = contact;
        conversationId = messagingDao.ensureDirectConversation(currentUser.getIdUsers(), contact.userId());
        messagingDao.markConversationRead(conversationId, currentUser.getIdUsers());
        configureHeaderForContact(contact, false);
        loadHumanConversation();
        setInputEnabled(true);
        renderContacts();
    }

    protected void loadHumanConversation() {
        messagesContainer.getChildren().clear();
        List<MessagingDao.DirectMessage> messages = messagingDao.getConversationMessages(conversationId);
        if (messages.isEmpty()) {
            addSystemMessage("Aucun message pour le moment. Commence la conversation.");
            return;
        }

        for (MessagingDao.DirectMessage message : messages) {
            if (message.senderUserId() == currentUser.getIdUsers()) {
                appendUserBubble(message.content(), message.sentAt());
            } else {
                appendContactBubble(message.content(), message.sentAt(), selectedContact.initials());
            }
        }
    }

    protected void sendAssistantMessage(String question) {
        appendUserBubble(question, LocalDateTime.now());
        setInputEnabled(false);

        List<ChatbotMessage> historySnapshot = new ArrayList<>(aiHistory);
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
            aiHistory.add(exchange);
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

    protected void sendHumanMessage(String text) {
        if (selectedContact == null || assistantMode) {
            addSystemMessage("Choisis d'abord un medecin ou un patient pour envoyer un message.");
            return;
        }
        MessagingDao.DirectMessage message = messagingDao.sendMessage(conversationId, currentUser.getIdUsers(), text);
        appendUserBubble(message.content(), message.sentAt());
        messageInput.requestFocus();
    }

    protected void configureHeaderForContact(MessagingDao.ChatContact contact, boolean isAssistant) {
        chatTitleLabel.setText(contact.displayName());
        chatSubtitleLabel.setText(isAssistant
                ? buildDisplayName(currentUser) + " - conversation persistante"
                : contact.subtitle());
        if (messageInput != null) {
            messageInput.setPromptText(isAssistant
                    ? "Posez votre question a l'assistant IA..."
                    : "Ecrire a " + contact.displayName() + "...");
        }
        if (assistantStatusLabel != null) {
            assistantStatusLabel.setText(contact.online() ? "En ligne" : "Hors ligne");
        }
        if (assistantAvatarLabel != null) {
            assistantAvatarLabel.setText(contact.initials());
        }
        assistantHeaderLabel.setText(contact.initials());
        String fill = isAssistant ? "#CBE3F8" : "#D8D0F0";
        if (assistantAvatarCircle != null) {
            assistantAvatarCircle.setStyle("-fx-fill: " + fill + ";");
        }
        assistantHeaderCircle.setStyle("-fx-fill: " + fill + ";");
    }

    protected HBox buildContactRow(MessagingDao.ChatContact contact, boolean isAssistant) {
        HBox row = new HBox(12);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        boolean active = selectedContact != null && selectedContact.userId() == contact.userId();
        row.setStyle((active
                ? "-fx-background-color: #E8F1FB; -fx-border-color: #D0E4F5;"
                : "-fx-background-color: transparent; -fx-border-color: #EEF3FA;")
                + " -fx-border-width: 0 0 1 0; -fx-padding: 16 18 16 18; -fx-cursor: hand;");

        StackPane avatar = new StackPane();
        Circle circle = new Circle(22);
        circle.setStyle("-fx-fill: " + (isAssistant ? "#C8E6C9" : "#C0D8EE") + ";");
        Label initials = new Label(contact.initials());
        initials.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: "
                + (isAssistant ? "#3DAA70" : "#2B7FD4") + ";");
        avatar.getChildren().addAll(circle, initials);

        VBox textBox = new VBox(4);
        Label name = new Label(contact.displayName());
        name.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1A2C42;");
        Label subtitle = new Label(isAssistant ? "En ligne" : contact.subtitle());
        subtitle.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (contact.online() ? "#3DAA70" : "#94A3B8") + ";");
        textBox.getChildren().addAll(name, subtitle);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        row.getChildren().addAll(avatar, textBox);
        row.setOnMouseClicked(event -> {
            if (isAssistant) {
                selectAssistantConversation();
            } else {
                selectHumanConversation(contact);
            }
        });
        return row;
    }

    protected void appendUserBubble(String text, LocalDateTime timestamp) {
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

    protected void appendAssistantBubble(String text, LocalDateTime timestamp) {
        appendContactBubble(text, timestamp, "AI");
    }

    protected void appendContactBubble(String text, LocalDateTime timestamp, String initialsText) {
        HBox wrapper = new HBox(10);
        wrapper.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        StackPane avatar = new StackPane();
        Circle circle = new Circle(18);
        circle.setStyle("-fx-fill: " + ("AI".equalsIgnoreCase(initialsText) ? "#CBE3F8" : "#D8D0F0") + ";");
        Label initials = new Label(initialsText);
        initials.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: "
                + ("AI".equalsIgnoreCase(initialsText) ? "#2B7FD4" : "#6B5CE7") + ";");
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

    protected void addSystemMessage(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle("-fx-background-color: #E8F1FB; -fx-text-fill: #1A2C42; -fx-font-size: 13px; "
                + "-fx-background-radius: 12; -fx-padding: 10 14 10 14;");
        HBox wrapper = new HBox(label);
        wrapper.setAlignment(javafx.geometry.Pos.CENTER);
        messagesContainer.getChildren().add(wrapper);
        scrollToBottom();
    }

    protected void setInputEnabled(boolean enabled) {
        if (messageInput != null) {
            messageInput.setDisable(!enabled);
        }
        if (sendButton != null) {
            sendButton.setDisable(!enabled);
        }
    }

    protected void scrollToBottom() {
        Platform.runLater(() -> {
            ScrollPane scrollPane = messagesScrollPane != null ? messagesScrollPane : findParentScrollPane(messagesContainer);
            if (scrollPane != null) {
                scrollPane.setVvalue(1.0);
            }
        });
    }

    protected ScrollPane findParentScrollPane(Node node) {
        Node current = node;
        while (current != null) {
            current = current.getParent();
            if (current instanceof ScrollPane scrollPane) {
                return scrollPane;
            }
        }
        return null;
    }

    protected Users refreshCurrentUser() {
        Users user = AppSession.getCurrentUser();
        if (user == null || user.getIdUsers() <= 0) {
            return user;
        }

        Users freshUser = new UsersDao().findById(user.getIdUsers()).orElse(user);
        AppSession.setCurrentUser(freshUser);
        return freshUser;
    }

    protected String buildDisplayName(Users user) {
        String prenom = user.getPrenom() == null ? "" : user.getPrenom().trim();
        String username = user.getUsername() == null ? "" : user.getUsername().trim();

        if (prenom.isEmpty() && username.isEmpty()) {
            return "Utilisateur";
        }
        if (prenom.isEmpty()) {
            return username;
        }
        if (username.isEmpty()) {
            return prenom;
        }
        return prenom + " " + username;
    }

    protected String getWelcomeMessage(Users user) {
        return "Bonjour " + buildDisplayName(user) + ", comment puis-je vous aider aujourd'hui ?";
    }

    protected String formatTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(TIME_FORMATTER);
    }

    protected String rootCauseMessage(Throwable throwable) {
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
