package org.example.meetcare.controller;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.meetcare.dao.ChatbotMessageDao;
import org.example.meetcare.dao.ChatConversationDao;
import org.example.meetcare.dao.ChatMessageDao;
import org.example.meetcare.dao.UsersDao;
import org.example.meetcare.model.ChatbotMessage;
import org.example.meetcare.model.ChatConversation;
import org.example.meetcare.model.ChatMessage;
import org.example.meetcare.model.ChatMessageAttachment;
import org.example.meetcare.model.Role;
import org.example.meetcare.model.Users;
import org.example.meetcare.service.GroqChatService;
import org.example.meetcare.utils.AppSession;
import org.example.meetcare.utils.AvatarUtil;
import org.example.meetcare.utils.LogoutUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public abstract class AbstractMessagingController {
    protected static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    protected static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final ChatConversationDao conversationDao = new ChatConversationDao();
    private final ChatMessageDao messageDao = new ChatMessageDao();
    private final ChatbotMessageDao chatbotMessageDao = new ChatbotMessageDao();
    private final GroqChatService groqChatService = new GroqChatService();
    private final UsersDao usersDao = new UsersDao();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "chat-realtime-poller");
        thread.setDaemon(true);
        return thread;
    });
    private final PauseTransition typingReset = new PauseTransition(Duration.seconds(2));
    private final List<File> selectedFiles = new ArrayList<>();

    protected Users currentUser;
    protected ChatConversation currentConversation;
    protected Users currentOtherUser;
    protected int assistantConversationId;
    protected boolean assistantMode;
    protected final List<ChatbotMessage> assistantHistory = new ArrayList<>();

    private String lastRenderedFingerprint = "";
    private boolean initializing;

    @FXML
    protected VBox conversationsContainer;

    @FXML
    protected TextField conversationSearchField;

    @FXML
    protected Label conversationsCountLabel;

    @FXML
    protected Label totalUnreadBadgeLabel;

    @FXML
    protected Label chatTitleLabel;

    @FXML
    protected Label chatSubtitleLabel;

    @FXML
    protected Label chatStatusLabel;

    @FXML
    protected Label typingLabel;

    @FXML
    protected Circle assistantHeaderCircle;

    @FXML
    protected Label assistantHeaderLabel;

    @FXML
    protected VBox messagesContainer;

    @FXML
    protected TextField messageInput;

    @FXML
    protected Button sendButton;

    @FXML
    protected Button attachButton;

    @FXML
    protected Label attachmentStatusLabel;

    @FXML
    protected ScrollPane messagesScrollPane;

    @FXML
    protected SidebarPatientController sidebarPatientController;

    @FXML
    protected SidebarMedecinController sidebarMedecinController;

    protected void initializeMessaging() {
        initializing = true;
        currentUser = refreshCurrentUser();
        configureTypingListener();
        configureSearchListener();
        configureShutdownHook();

        if (currentUser == null) {
            showDisconnectedState();
            initializing = false;
            return;
        }

        usersDao.updateOnlineStatus(currentUser.getIdUsers(), true);
        assistantConversationId = chatbotMessageDao.ensureConversationForUser(currentUser.getIdUsers());

        if (AppSession.isOpenAssistantConversation()) {
            AppSession.setOpenAssistantConversation(false);
            assistantMode = true;
            loadAssistantConversation();
        } else {
            loadConversations();
            if (currentConversation == null) {
                showEmptyConversationState();
            }
        }

        scheduler.scheduleAtFixedRate(() -> Platform.runLater(this::refreshRealtimeState), 2, 2, TimeUnit.SECONDS);
        initializing = false;
    }

    protected abstract Role getTargetRole();

    @FXML
    protected void handleSendMessage() {
        sendCurrentMessage();
    }

    @FXML
    protected void handleSendMessageViaClick() {
        sendCurrentMessage();
    }

    @FXML
    protected void handleAddConversation(ActionEvent event) {
        if (currentUser == null) {
            return;
        }

        try {
            Optional<Users> selectedUser = showUserSelectionDialog();
            selectedUser.ifPresent(user -> {
                int conversationId = conversationDao.ensureConversationBetween(currentUser.getIdUsers(),
                        user.getIdUsers());
                currentConversation = new ChatConversation();
                currentConversation.setIdConversation(conversationId);
                currentOtherUser = user;
                loadConversation(conversationId, user);
                loadConversations();
            });
        } catch (Exception ex) {
            showInfo("Impossible d'ouvrir la création de discussion: " + rootCauseMessage(ex));
        }
    }

    @FXML
    protected void handleConversationSearch() {
        loadConversations();
    }

    @FXML
    protected void handleLogout(MouseEvent event) {
        shutdownAndMarkOffline();
        LogoutUtil.logout((Node) event.getSource());
    }

    @FXML
    protected void handleMessages(MouseEvent event) {
        if (messageInput != null) {
            messageInput.requestFocus();
        }
    }

    @FXML
    protected void handleAssistantConversation(MouseEvent event) {
        loadAssistantConversation();
    }

    public void openAssistantConversationFromNavigation() {
        if (currentUser == null) {
            AppSession.setOpenAssistantConversation(true);
            return;
        }

        assistantMode = true;
        loadAssistantConversation();
    }

    protected void loadAssistantConversation() {
        assistantMode = true;
        currentConversation = null;
        currentOtherUser = null;
        loadAssistantHistory();
        updateHeader();
        loadConversations();
        messageInput.requestFocus();
    }

    protected void loadAssistantHistory() {
        assistantHistory.clear();
        assistantHistory.addAll(chatbotMessageDao.findMessagesByConversationAndUser(
                assistantConversationId,
                currentUser.getIdUsers()));

        messagesContainer.getChildren().clear();
        if (assistantHistory.isEmpty()) {
            messagesContainer.getChildren().add(createEmptyMessageState("Demande quelque chose à l'assistant IA."));
            scrollToBottom();
            return;
        }

        for (ChatbotMessage exchange : assistantHistory) {
            messagesContainer.getChildren().add(createAssistantQuestionNode(exchange));
            messagesContainer.getChildren().add(createAssistantResponseNode(exchange));
        }

        lastRenderedFingerprint = buildAssistantFingerprint(assistantHistory);
        scrollToBottom();
    }

    protected void loadConversations() {
        if (currentUser == null) {
            return;
        }

        String query = conversationSearchField == null ? "" : conversationSearchField.getText();
        List<ChatConversation> conversations = conversationDao.findConversationsForUser(currentUser.getIdUsers(), null,
                query);
        conversationsContainer.getChildren().clear();
        if (conversationsCountLabel != null) {
            conversationsCountLabel.setText("");
        }
        updateSidebarUnreadBadge(messageDao.countUnreadTotalForUser(currentUser.getIdUsers()));

        conversationsContainer.getChildren().add(createAssistantConversationRow());

        if (conversations.isEmpty()) {
            conversationsContainer.getChildren().add(createEmptyConversationRow("Aucune discussion disponible."));
            if (currentConversation == null && !assistantMode) {
                showEmptyConversationState();
            }
            return;
        }

        for (ChatConversation conversation : conversations) {
            conversationsContainer.getChildren().add(createConversationRow(conversation));
        }

        if (currentConversation != null) {
            conversations.stream()
                    .filter(conversation -> conversation.getIdConversation() == currentConversation.getIdConversation())
                    .findFirst()
                    .ifPresent(found -> {
                        currentConversation = found;
                        currentOtherUser = found.getOtherUser();
                        assistantMode = false;
                        updateHeader();
                    });
        } else if (!assistantMode) {
            ChatConversation firstConversation = conversations.get(0);
            currentConversation = firstConversation;
            currentOtherUser = firstConversation.getOtherUser();
            loadConversation(firstConversation.getIdConversation(), firstConversation.getOtherUser());
        }
    }

    protected void loadConversation(int conversationId, Users otherUser) {
        assistantMode = false;
        currentConversation = new ChatConversation();
        currentConversation.setIdConversation(conversationId);
        currentOtherUser = otherUser;

        if (currentUser == null) {
            return;
        }

        messageDao.markConversationAsRead(conversationId, currentUser.getIdUsers());
        conversationDao.markTyping(conversationId, currentUser.getIdUsers(), false);
        renderConversationMessages(conversationId);
        updateHeader();
        loadConversations();
        messageInput.requestFocus();
    }

    protected void renderConversationMessages(int conversationId) {
        List<ChatMessage> messages = messageDao.findMessages(conversationId);
        messagesContainer.getChildren().clear();

        if (messages.isEmpty()) {
            messagesContainer.getChildren()
                    .add(createEmptyMessageState("Écris un premier message pour démarrer la discussion."));
            scrollToBottom();
            return;
        }

        for (ChatMessage message : messages) {
            if (message.getSenderId() == currentUser.getIdUsers()) {
                messagesContainer.getChildren().add(createOutgoingMessageNode(message));
            } else {
                messagesContainer.getChildren().add(createIncomingMessageNode(message));
            }
        }

        lastRenderedFingerprint = buildFingerprint(messages);
        scrollToBottom();
    }

    protected void sendCurrentMessage() {
        if (currentUser == null) {
            showInfo("Sélectionne d'abord une conversation.");
            return;
        }

        String content = messageInput.getText() == null ? "" : messageInput.getText().trim();
        if (content.isBlank() && selectedFiles.isEmpty()) {
            return;
        }

        messageInput.clear();
        if (assistantMode) {
            sendAssistantMessage(content);
            return;
        }

        if (currentConversation == null) {
            showInfo("Sélectionne d'abord une conversation.");
            return;
        }

        conversationDao.markTyping(currentConversation.getIdConversation(), currentUser.getIdUsers(), false);

        ChatMessage savedMessage = messageDao.saveMessage(
                currentConversation.getIdConversation(),
                currentUser.getIdUsers(),
                content.isBlank() ? null : content,
                selectedFiles.isEmpty() ? "TEXT" : "MIXED");

        List<ChatMessageAttachment> attachments = persistSelectedFiles(savedMessage.getIdMessage(),
                currentConversation.getIdConversation());
        messageDao.saveAttachments(savedMessage.getIdMessage(), attachments);
        selectedFiles.clear();
        attachmentStatusLabel.setText("Aucune pièce jointe");

        renderConversationMessages(currentConversation.getIdConversation());
        loadConversations();
    }

    protected void sendAssistantMessage(String content) {
        List<ChatbotMessage> historySnapshot = new ArrayList<>(assistantHistory);
        ChatbotMessageDao dao = chatbotMessageDao;
        var task = new javafx.concurrent.Task<ChatbotMessage>() {
            @Override
            protected ChatbotMessage call() {
                String response;
                try {
                    response = groqChatService.generateReply(content, historySnapshot);
                } catch (Exception ex) {
                    response = "Je ne peux pas joindre l'assistant IA pour le moment. " + rootCauseMessage(ex);
                }
                return dao.saveExchange(
                        assistantConversationId,
                        currentUser.getIdUsers(),
                        content,
                        response,
                        groqChatService.getModelName());
            }
        };

        task.setOnSucceeded(event -> {
            ChatbotMessage exchange = task.getValue();
            assistantHistory.add(exchange);
            loadAssistantHistory();
            loadConversations();
        });
        task.setOnFailed(event -> showInfo("Erreur lors de l'envoi vers l'assistant IA."));
        Thread thread = new Thread(task, "assistant-chat-request");
        thread.setDaemon(true);
        thread.start();
    }

    protected void refreshRealtimeState() {
        if (currentUser == null) {
            return;
        }

        usersDao.updateOnlineStatus(currentUser.getIdUsers(), true);
        loadConversations();

        if (assistantMode) {
            List<ChatbotMessage> snapshot = chatbotMessageDao.findMessagesByConversationAndUser(
                    assistantConversationId,
                    currentUser.getIdUsers());
            String fingerprint = buildAssistantFingerprint(snapshot);
            if (!fingerprint.equals(lastRenderedFingerprint)) {
                assistantHistory.clear();
                assistantHistory.addAll(snapshot);
                loadAssistantHistory();
            }
            updateHeader();
            return;
        }

        if (currentConversation != null) {
            String fingerprint = buildFingerprint(messageDao.findMessages(currentConversation.getIdConversation()));
            if (!fingerprint.equals(lastRenderedFingerprint)) {
                renderConversationMessages(currentConversation.getIdConversation());
            }
            updateHeader();
        }
    }

    protected void updateHeader() {
        if (assistantMode) {
            chatTitleLabel.setText("Assistant IA");
            chatSubtitleLabel.setText("Conversation intelligente");
            chatStatusLabel.setText("Disponible");
            chatStatusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #25D366;");
            typingLabel.setText("");
            assistantHeaderLabel.setText("AI");
            assistantHeaderCircle.setFill(Color.web("#CBE3F8"));
            return;
        }

        if (currentOtherUser == null) {
            chatTitleLabel.setText("Messages");
            chatSubtitleLabel.setText("Sélectionne une conversation");
            chatStatusLabel.setText("");
            typingLabel.setText("");
            assistantHeaderLabel.setText("?");
            assistantHeaderCircle.setFill(Color.web("#CBE3F8"));
            return;
        }

        chatTitleLabel.setText(buildDisplayName(currentOtherUser));
        assistantHeaderLabel.setText(AvatarUtil.buildInitials(currentOtherUser));
        AvatarUtil.applyAvatar(assistantHeaderCircle, assistantHeaderLabel, currentOtherUser);

        if (currentOtherUser.isOnline()) {
            chatSubtitleLabel.setText("En ligne");
            chatSubtitleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #3DAA70;");
        } else if (currentOtherUser.getLastSeen() != null) {
            chatSubtitleLabel.setText("Vu pour la dernière fois " + formatDate(currentOtherUser.getLastSeen()));
            chatSubtitleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #8A96A8;");
        } else {
            chatSubtitleLabel.setText("Hors ligne");
            chatSubtitleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #8A96A8;");
        }

        boolean typing = currentConversation != null
                && conversationDao.isOtherUserTyping(currentConversation.getIdConversation(), currentUser.getIdUsers());
        typingLabel.setText(typing ? "En train d'écrire..." : "");
        typingLabel.setStyle("-fx-text-fill: #2B7FD4; -fx-font-size: 11px;");
        chatStatusLabel.setText("");
    }

    protected HBox createConversationRow(ChatConversation conversation) {
        Users user = conversation.getOtherUser();
        boolean selected = currentConversation != null
                && currentConversation.getIdConversation() == conversation.getIdConversation();

        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle(selected
                ? "-fx-padding: 14 14 14 14; -fx-background-color: #E8F1FB; -fx-border-color: #D0E4F5; -fx-border-width: 0 0 1 0; -fx-cursor: hand;"
                : "-fx-padding: 14 14 14 14; -fx-border-color: #EEF3FA; -fx-border-width: 0 0 1 0; -fx-cursor: hand;");
        row.setOnMouseClicked(event -> loadConversation(conversation.getIdConversation(), user));

        StackPane avatar = new StackPane();
        Circle circle = new Circle(22);
        circle.setFill(Color.web(user.isOnline() ? "#C7F0D8" : "#D8E4F0"));
        Label initials = new Label(AvatarUtil.buildInitials(user));
        initials.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #2B7FD4;");
        avatar.getChildren().addAll(circle, initials);

        VBox content = new VBox(2);
        content.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(content, Priority.ALWAYS);

        HBox titleRow = new HBox(6);
        Label nameLabel = new Label(buildDisplayName(user));
        nameLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1A2C42;");
        Label timeLabel = new Label(formatConversationTime(conversation.getLastMessageAt()));
        timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #90A4B8;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        titleRow.getChildren().addAll(nameLabel, spacer, timeLabel);

        HBox previewRow = new HBox(8);
        Label previewLabel = new Label(buildPreviewText(conversation));
        previewLabel.setWrapText(false);
        previewLabel.setMaxWidth(180);
        previewLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B8CAD;");
        if (conversation.isTyping()) {
            previewLabel.setText("En train d'écrire...");
            previewLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #2B7FD4; -fx-font-style: italic;");
        }
        previewRow.getChildren().add(previewLabel);

        content.getChildren().addAll(titleRow, previewRow);

        Region rowSpacer = new Region();
        HBox.setHgrow(rowSpacer, Priority.ALWAYS);

        VBox rightSide = new VBox(6);
        rightSide.setAlignment(Pos.CENTER_RIGHT);
        if (conversation.getUnreadCount() > 0) {
            rightSide.getChildren().add(createUnreadBadge(conversation.getUnreadCount()));
        }

        row.getChildren().addAll(avatar, content, rowSpacer, rightSide);
        return row;
    }

    protected HBox createAssistantConversationRow() {
        boolean selected = assistantMode;
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle(selected
                ? "-fx-padding: 14 14 14 14; -fx-background-color: #E8F1FB; -fx-border-color: #D0E4F5; -fx-border-width: 0 0 1 0; -fx-cursor: hand;"
                : "-fx-padding: 14 14 14 14; -fx-border-color: #EEF3FA; -fx-border-width: 0 0 1 0; -fx-cursor: hand;");
        row.setOnMouseClicked(event -> loadAssistantConversation());

        StackPane avatar = new StackPane();
        Circle circle = new Circle(22);
        circle.setFill(Color.web("#CBE3F8"));
        Label initials = new Label("AI");
        initials.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #2B7FD4;");
        avatar.getChildren().addAll(circle, initials);

        VBox content = new VBox(2);
        content.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(content, Priority.ALWAYS);

        HBox titleRow = new HBox(6);
        Label nameLabel = new Label("Assistant IA");
        nameLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1A2C42;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label timeLabel = new Label("AI");
        timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #90A4B8;");
        titleRow.getChildren().addAll(nameLabel, spacer, timeLabel);

        Label previewLabel = new Label("Conversation intelligente");
        previewLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B8CAD;");

        content.getChildren().addAll(titleRow, previewLabel);
        row.getChildren().addAll(avatar, content);
        return row;
    }

    protected Node createUnreadBadge(int count) {
        StackPane badge = new StackPane();
        Circle circle = new Circle(10);
        circle.setFill(Color.web("#25D366"));
        Label label = new Label(count > 99 ? "99+" : String.valueOf(count));
        label.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: white;");
        badge.getChildren().addAll(circle, label);
        return badge;
    }

    protected Node createOutgoingMessageNode(ChatMessage message) {
        VBox wrapper = new VBox(4);
        wrapper.setAlignment(Pos.CENTER_RIGHT);

        VBox bubble = new VBox(4);
        bubble.setMaxWidth(430);
        Label messageLabel = new Label(message.getContent() == null ? "" : message.getContent());
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(360);
        messageLabel.setStyle("-fx-background-color: #2B7FD4; -fx-text-fill: white; -fx-font-size: 13px; "
                + "-fx-background-radius: 14 14 4 14; -fx-padding: 12 16 12 16; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 6, 0, 0, 2);");

        Label timeLabel = new Label(formatTime(message.getSentAt()));
        timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #94A3B8;");
        Label statusLabel = new Label(message.isRead() ? "✓✓" : "✓");
        statusLabel.setStyle(message.isRead()
                ? "-fx-font-size: 11px; -fx-text-fill: #2B7FD4; -fx-font-weight: bold;"
                : "-fx-font-size: 11px; -fx-text-fill: #94A3B8; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // delete button hidden by default, shown on hover of the bubble
        Button deleteButton = new Button("🗑");
        deleteButton.setVisible(false);
        deleteButton.setManaged(false);
        deleteButton.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #F97316; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 0 0 0 4;");
        deleteButton.setOnAction(event -> handleDeleteMessage(message));

        // message row: message content + time + status on the same line (flex)
        HBox messageRow = new HBox(8);
        messageRow.setAlignment(Pos.CENTER_RIGHT);
        messageRow.getChildren().addAll(messageLabel, spacer, timeLabel, statusLabel, deleteButton);
        HBox.setHgrow(messageLabel, Priority.ALWAYS);

        bubble.getChildren().add(messageRow);

        if (!message.getAttachments().isEmpty()) {
            bubble.getChildren().add(createAttachmentBox(message.getAttachments(), true));
        }

        // show delete button only when hovering the bubble
        bubble.setOnMouseEntered(e -> {
            deleteButton.setVisible(true);
            deleteButton.setManaged(true);
        });
        bubble.setOnMouseExited(e -> {
            deleteButton.setVisible(false);
            deleteButton.setManaged(false);
        });

        wrapper.getChildren().add(bubble);
        return wrapper;
    }

    protected Node createIncomingMessageNode(ChatMessage message) {
        HBox wrapper = new HBox(10);
        wrapper.setAlignment(Pos.CENTER_LEFT);

        StackPane avatar = new StackPane();
        Circle circle = new Circle(18);
        circle.setFill(Color.web("#CBE3F8"));
        Label initials = new Label(currentOtherUser == null ? "?" : AvatarUtil.buildInitials(currentOtherUser));
        initials.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #2B7FD4;");
        avatar.getChildren().addAll(circle, initials);

        VBox bubble = new VBox(4);
        bubble.setMaxWidth(430);

        Label messageLabel = new Label(message.getContent() == null ? "" : message.getContent());
        messageLabel.setWrapText(true);
        messageLabel.setStyle("-fx-background-color: white; -fx-text-fill: #1A2C42; -fx-font-size: 13px; "
                + "-fx-background-radius: 14 14 14 4; -fx-padding: 12 16 12 16; "
                + "-fx-effect: dropshadow(gaussian, #d8e4f0, 4, 0, 0, 1);");
        bubble.getChildren().add(messageLabel);

        if (!message.getAttachments().isEmpty()) {
            bubble.getChildren().add(createAttachmentBox(message.getAttachments(), false));
        }

        Label timeLabel = new Label(formatTime(message.getSentAt()));
        timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #94A3B8;");
        bubble.getChildren().add(timeLabel);

        wrapper.getChildren().addAll(avatar, bubble);
        return wrapper;
    }

    protected Node createAssistantQuestionNode(ChatbotMessage exchange) {
        VBox wrapper = new VBox(4);
        wrapper.setAlignment(Pos.CENTER_RIGHT);

        Label label = new Label(exchange.getQuestion() == null ? "" : exchange.getQuestion());
        label.setWrapText(true);
        label.setStyle("-fx-background-color: #2B7FD4; -fx-text-fill: white; -fx-font-size: 13px; "
                + "-fx-background-radius: 14 14 4 14; -fx-padding: 12 16 12 16;");

        Label timeLabel = new Label(formatTime(exchange.getDate()));
        timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #94A3B8;");
        wrapper.getChildren().addAll(label, timeLabel);
        return wrapper;
    }

    protected Node createAssistantResponseNode(ChatbotMessage exchange) {
        HBox wrapper = new HBox(10);
        wrapper.setAlignment(Pos.CENTER_LEFT);

        StackPane avatar = new StackPane();
        Circle circle = new Circle(18);
        circle.setFill(Color.web("#CBE3F8"));
        Label initials = new Label("AI");
        initials.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #2B7FD4;");
        avatar.getChildren().addAll(circle, initials);

        VBox bubble = new VBox(4);
        bubble.setMaxWidth(430);
        Label label = new Label(exchange.getResponse() == null ? "" : exchange.getResponse());
        label.setWrapText(true);
        label.setStyle("-fx-background-color: white; -fx-text-fill: #1A2C42; -fx-font-size: 13px; "
                + "-fx-background-radius: 14 14 14 4; -fx-padding: 12 16 12 16; "
                + "-fx-effect: dropshadow(gaussian, #d8e4f0, 4, 0, 0, 1);");
        Label timeLabel = new Label(formatTime(exchange.getDate()));
        timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #94A3B8;");
        bubble.getChildren().addAll(label, timeLabel);

        wrapper.getChildren().addAll(avatar, bubble);
        return wrapper;
    }

    protected Node createAttachmentBox(List<ChatMessageAttachment> attachments, boolean outgoing) {
        VBox container = new VBox(8);
        container.setStyle(outgoing
                ? "-fx-background-color: rgba(255,255,255,0.12); -fx-background-radius: 12; -fx-padding: 8 10 8 10;"
                : "-fx-background-color: #F7FAFD; -fx-background-radius: 12; -fx-padding: 8 10 8 10;");

        for (ChatMessageAttachment attachment : attachments) {
            Node node;
            if (attachment.isImage()) {
                node = createImageAttachmentNode(attachment);
            } else {
                node = createFileAttachmentNode(attachment, outgoing);
            }
            container.getChildren().add(node);
        }
        return container;
    }

    protected Node createImageAttachmentNode(ChatMessageAttachment attachment) {
        VBox box = new VBox(6);
        box.setStyle(
                "-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 8; -fx-border-color: #E6EEF6; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 6, 0, 0, 2);");
        try {
            Image image = new Image(Path.of(attachment.getFilePath()).toUri().toString(), 240, 0, true, true);
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(240);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            box.getChildren().add(imageView);
        } catch (Exception ex) {
            box.getChildren().add(new Label(attachment.getFileName()));
        }
        HBox footer = new HBox(8);
        footer.setAlignment(Pos.CENTER_LEFT);
        Label fileLabel = new Label(attachment.getFileName());
        fileLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #263445;");
        Label meta = new Label(formatSize(attachment.getFileSize()));
        meta.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B8CAD;");
        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);
        Button downloadButton = new Button("⬇");
        downloadButton.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #2B7FD4; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 0;");
        downloadButton.setOnAction(event -> handleDownloadAttachment(attachment));
        footer.getChildren().addAll(fileLabel, meta, footerSpacer, downloadButton);
        box.getChildren().add(footer);
        return box;
    }

    protected Node createFileAttachmentNode(ChatMessageAttachment attachment, boolean outgoing) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle(outgoing
                ? "-fx-background-color: rgba(43,127,212,0.12); -fx-background-radius: 10; -fx-padding: 8 10 8 10; -fx-border-color: rgba(43,127,212,0.16); -fx-border-width: 1;"
                : "-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 8 10 8 10; -fx-border-color: #E6EEF6; -fx-border-width: 1;");

        // icon with subtle background
        StackPane iconBg = new StackPane();
        iconBg.setStyle("-fx-background-color: " + (outgoing ? "rgba(255,255,255,0.02)" : "#F1F7FC")
                + "; -fx-background-radius: 6; -fx-padding: 6;");
        Label icon = new Label(iconForAttachment(attachment));
        icon.setStyle("-fx-font-size: 16px; -fx-text-fill: #2B7FD4;");
        iconBg.getChildren().add(icon);

        Label name = new Label(attachment.getFileName());
        name.setWrapText(true);
        name.setMaxWidth(260);
        name.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + (outgoing ? "#123344" : "#1A2C42")
                + ";");
        Label meta = new Label(formatSize(attachment.getFileSize()));
        meta.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B8CAD;");

        VBox texts = new VBox(2, name, meta);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button downloadButton = new Button("⬇");
        downloadButton.setStyle("-fx-background-color: transparent; -fx-text-fill: "
                + (outgoing ? "#123344" : "#2B7FD4") + "; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 0;");
        downloadButton.setOnAction(event -> handleDownloadAttachment(attachment));
        box.getChildren().addAll(iconBg, texts, spacer, downloadButton);
        return box;
    }

    protected Node createEmptyConversationRow(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-padding: 20; -fx-text-fill: #90A4B8; -fx-font-size: 12px;");
        return label;
    }

    protected Node createEmptyMessageState(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle("-fx-background-color: #E8F1FB; -fx-text-fill: #1A2C42; -fx-font-size: 13px; "
                + "-fx-background-radius: 12; -fx-padding: 10 14 10 14;");
        HBox wrapper = new HBox(label);
        wrapper.setAlignment(Pos.CENTER);
        return wrapper;
    }

    protected void showEmptyConversationState() {
        chatTitleLabel.setText("Messages");
        chatSubtitleLabel.setText("Aucune conversation sélectionnée");
        chatStatusLabel.setText("Hors ligne");
        typingLabel.setText("");
        assistantHeaderLabel.setText("?");
        assistantHeaderCircle.setFill(Color.web("#CBE3F8"));
        messagesContainer.getChildren()
                .setAll(createEmptyMessageState("Choisis une discussion ou crée-en une avec le bouton +."));
    }

    protected void showDisconnectedState() {
        chatTitleLabel.setText("Connexion requise");
        chatSubtitleLabel.setText("Connecte-toi pour utiliser la messagerie");
        chatStatusLabel.setText("Hors ligne");
        typingLabel.setText("");
        messagesContainer.getChildren()
                .setAll(createEmptyMessageState("Tu dois être connecté pour accéder aux conversations."));
        setInputsDisabled(true);
    }

    protected void setInputsDisabled(boolean disabled) {
        messageInput.setDisable(disabled);
        sendButton.setDisable(disabled);
        attachButton.setDisable(disabled);
    }

    protected void handleDeleteMessage(ChatMessage message) {
        if (currentUser == null || currentConversation == null || message == null) {
            return;
        }
        if (message.getSenderId() != currentUser.getIdUsers()) {
            showInfo("Tu ne peux supprimer que tes propres messages.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer le message");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer ce message et ses pieces jointes ?");
        confirm.showAndWait().ifPresent(buttonType -> {
            if (buttonType == ButtonType.OK) {
                messageDao.deleteMessageAndAttachments(message.getIdMessage());
                renderConversationMessages(currentConversation.getIdConversation());
                loadConversations();
            }
        });
    }

    protected void handleDownloadAttachment(ChatMessageAttachment attachment) {
        if (attachment == null || attachment.getFilePath() == null || attachment.getFilePath().isBlank()) {
            showInfo("Piece jointe introuvable.");
            return;
        }

        File source = new File(attachment.getFilePath());
        if (!source.exists()) {
            showInfo("Le fichier a telecharger est introuvable.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer la piece jointe");
        chooser.setInitialFileName(attachment.getFileName() == null ? source.getName() : attachment.getFileName());
        File target = chooser.showSaveDialog(messageInput == null || messageInput.getScene() == null
                ? null
                : (Stage) messageInput.getScene().getWindow());
        if (target == null) {
            return;
        }

        try {
            Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            showInfo("Piece jointe telechargee dans: " + target.getAbsolutePath());
        } catch (IOException ex) {
            showInfo("Impossible de telecharger la piece jointe: " + rootCauseMessage(ex));
        }
    }

    protected void updateSidebarUnreadBadge(int unreadCount) {
        if (sidebarPatientController != null) {
            sidebarPatientController.setMessageBadgeCount(unreadCount);
        }
        if (sidebarMedecinController != null) {
            sidebarMedecinController.setMessageBadgeCount(unreadCount);
        }
        if (totalUnreadBadgeLabel != null) {
            totalUnreadBadgeLabel.setText(unreadCount > 0 ? String.valueOf(unreadCount) : "");
            totalUnreadBadgeLabel.setVisible(unreadCount > 0);
            totalUnreadBadgeLabel.setManaged(unreadCount > 0);
        }
    }

    protected void configureSearchListener() {
        if (conversationSearchField == null) {
            return;
        }
        conversationSearchField.textProperty().addListener((observable, oldValue, newValue) -> loadConversations());
    }

    protected void configureTypingListener() {
        typingReset.setOnFinished(event -> {
            if (currentConversation != null && currentUser != null) {
                conversationDao.markTyping(currentConversation.getIdConversation(), currentUser.getIdUsers(), false);
                if (typingLabel != null) {
                    typingLabel.setText("");
                }
            }
        });

        if (messageInput == null) {
            return;
        }

        messageInput.textProperty().addListener((observable, oldValue, newValue) -> {
            if (currentConversation == null || currentUser == null || initializing) {
                return;
            }

            boolean typing = newValue != null && !newValue.isBlank();
            conversationDao.markTyping(currentConversation.getIdConversation(), currentUser.getIdUsers(), typing);
            typingReset.stop();
            if (typing) {
                typingReset.playFromStart();
            }
        });
    }

    protected void configureShutdownHook() {
        if (messageInput == null) {
            return;
        }

        messageInput.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene == null) {
                return;
            }

            newScene.windowProperty().addListener((windowObservable, oldWindow, newWindow) -> {
                if (newWindow instanceof Stage stage) {
                    stage.setOnHidden(event -> shutdownAndMarkOffline());
                }
            });
        });
    }

    protected void shutdownAndMarkOffline() {
        scheduler.shutdownNow();
        if (currentUser != null) {
            try {
                usersDao.updateOnlineStatus(currentUser.getIdUsers(), false);
            } catch (RuntimeException ignored) {
                // We prefer a graceful logout even if presence sync fails.
            }
        }
    }

    protected Optional<Users> showUserSelectionDialog() {
        List<Users> candidates = conversationDao.findSelectableUsersForChat(currentUser.getIdUsers(), null, "");
        if (candidates.isEmpty()) {
            showInfo("Aucun utilisateur disponible pour démarrer une discussion.");
            return Optional.empty();
        }

        Dialog<Users> dialog = new Dialog<>();
        dialog.setTitle("Nouvelle conversation");
        dialog.setHeaderText("Rechercher et sélectionner un utilisateur");

        DialogPane pane = dialog.getDialogPane();
        ButtonType startButton = new ButtonType("Ouvrir", ButtonType.OK.getButtonData());
        pane.getButtonTypes().addAll(startButton, ButtonType.CANCEL);

        VBox content = new VBox(10);
        content.setPrefWidth(460);
        content.setPrefHeight(380);

        TextField searchField = new TextField();
        searchField.setPromptText("Rechercher un utilisateur...");

        ListView<Users> userListView = new ListView<>();
        ObservableList<Users> visibleUsers = FXCollections.observableArrayList(candidates);
        userListView.setItems(visibleUsers);
        userListView.setPrefHeight(320);
        userListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Users item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                Circle circle = new Circle(16);
                circle.setFill(Color.web(item.isOnline() ? "#C7F0D8" : "#D8E4F0"));
                Label initials = new Label(AvatarUtil.buildInitials(item));
                initials.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #2B7FD4;");
                StackPane avatar = new StackPane(circle, initials);
                VBox texts = new VBox(2);
                Label name = new Label(buildDisplayName(item));
                name.setStyle("-fx-font-weight: bold; -fx-text-fill: #1A2C42;");
                Label meta = new Label(item.isOnline() ? "En ligne" : formatLastSeen(item.getLastSeen()));
                meta.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B8CAD;");
                texts.getChildren().addAll(name, meta);
                row.getChildren().addAll(avatar, texts);
                setText(null);
                setGraphic(row);
            }
        });

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            List<Users> filtered = conversationDao.findSelectableUsersForChat(
                    currentUser.getIdUsers(),
                    null,
                    newValue);
            visibleUsers.setAll(filtered);
        });

        content.getChildren().addAll(searchField, userListView);
        pane.setContent(content);

        if (messageInput != null && messageInput.getScene() != null) {
            dialog.initOwner(messageInput.getScene().getWindow());
        }
        dialog.initModality(Modality.WINDOW_MODAL);

        dialog.setResultConverter(
                buttonType -> buttonType == startButton ? userListView.getSelectionModel().getSelectedItem() : null);
        return dialog.showAndWait();
    }

    protected List<ChatMessageAttachment> persistSelectedFiles(int messageId, int conversationId) {
        List<ChatMessageAttachment> attachments = new ArrayList<>();
        if (selectedFiles.isEmpty()) {
            return attachments;
        }

        Path baseDir = Path.of(System.getProperty("user.dir"), "uploads", "chat", String.valueOf(conversationId),
                String.valueOf(messageId));
        try {
            Files.createDirectories(baseDir);
            for (File file : selectedFiles) {
                Path source = file.toPath();
                String fileName = source.getFileName().toString();
                Path destination = baseDir.resolve(System.currentTimeMillis() + "_" + fileName);
                Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);

                ChatMessageAttachment attachment = new ChatMessageAttachment();
                attachment.setMessageId(messageId);
                attachment.setFileName(fileName);
                attachment.setFilePath(destination.toAbsolutePath().toString());
                attachment.setFileType(detectContentType(source, fileName));
                attachment.setFileSize(Files.size(destination));
                attachment.setCreatedAt(LocalDateTime.now());
                attachments.add(attachment);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Impossible de sauvegarder les fichiers joints.", ex);
        }
        return attachments;
    }

    @FXML
    protected void handleAttachFiles() {
        if (currentUser == null) {
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Joindre des fichiers");
        FileChooser.ExtensionFilter supportedFiles = new FileChooser.ExtensionFilter(
                "Fichiers supportes",
                "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp", "*.bmp",
                "*.pdf", "*.doc", "*.docx", "*.txt", "*.rtf", "*.odt",
                "*.ppt", "*.pptx", "*.odp",
                "*.xls", "*.xlsx", "*.csv", "*.ods",
                "*.js", "*.php", "*.html", "*.htm", "*.css", "*.java", "*.py", "*.json", "*.xml", "*.sql",
                "*.mp4", "*.mov", "*.avi", "*.mkv", "*.webm", "*.wmv");
        chooser.getExtensionFilters().addAll(
                supportedFiles,
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp", "*.bmp"),
                new FileChooser.ExtensionFilter("Documents", "*.pdf", "*.doc", "*.docx", "*.txt", "*.rtf", "*.odt"),
                new FileChooser.ExtensionFilter("Presentations", "*.ppt", "*.pptx", "*.odp"),
                new FileChooser.ExtensionFilter("Tableurs", "*.xls", "*.xlsx", "*.csv", "*.ods"),
                new FileChooser.ExtensionFilter("Code", "*.js", "*.php", "*.html", "*.htm", "*.css", "*.java", "*.py",
                        "*.json", "*.xml", "*.sql"),
                new FileChooser.ExtensionFilter("Videos", "*.mp4", "*.mov", "*.avi", "*.mkv", "*.webm", "*.wmv"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*"));
        chooser.setSelectedExtensionFilter(supportedFiles);

        Stage stage = (Stage) messageInput.getScene().getWindow();
        List<File> files = chooser.showOpenMultipleDialog(stage);
        if (files == null || files.isEmpty()) {
            return;
        }

        selectedFiles.clear();
        selectedFiles.addAll(files);
        attachmentStatusLabel.setText(files.size() + " fichier(s) prêt(s) à être envoyé(s)");
    }

    protected void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    protected Users refreshCurrentUser() {
        Users user = AppSession.getCurrentUser();
        if (user == null || user.getIdUsers() <= 0) {
            return user;
        }
        Users freshUser = usersDao.findById(user.getIdUsers()).orElse(user);
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

    protected String formatTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(TIME_FORMATTER);
    }

    protected String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DATE_FORMATTER);
    }

    protected String formatLastSeen(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "Hors ligne";
        }
        return "Vu " + formatDate(dateTime);
    }

    protected String formatConversationTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DATE_FORMATTER);
    }

    protected String buildPreviewText(ChatConversation conversation) {
        if (conversation.getLastMessagePreview() == null || conversation.getLastMessagePreview().isBlank()) {
            return "Nouvelle conversation";
        }
        return conversation.getLastMessagePreview();
    }

    protected String formatSize(long size) {
        if (size < 1024) {
            return size + " o";
        }
        long kb = size / 1024;
        if (kb < 1024) {
            return kb + " Ko";
        }
        return (kb / 1024) + " Mo";
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

    protected String detectContentType(Path source, String fileName) throws IOException {
        String type = Files.probeContentType(source);
        if (type != null) {
            return type;
        }
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (lower.endsWith(".doc")) {
            return "application/msword";
        }
        if (lower.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        if (lower.endsWith(".ppt")) {
            return "application/vnd.ms-powerpoint";
        }
        if (lower.endsWith(".pptx")) {
            return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        }
        if (lower.endsWith(".xls")) {
            return "application/vnd.ms-excel";
        }
        if (lower.endsWith(".xlsx")) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        }
        if (lower.endsWith(".txt")) {
            return "text/plain";
        }
        if (lower.endsWith(".rtf")) {
            return "application/rtf";
        }
        if (lower.endsWith(".odt")) {
            return "application/vnd.oasis.opendocument.text";
        }
        if (lower.endsWith(".odp")) {
            return "application/vnd.oasis.opendocument.presentation";
        }
        if (lower.endsWith(".ods")) {
            return "application/vnd.oasis.opendocument.spreadsheet";
        }
        if (lower.endsWith(".csv")) {
            return "text/csv";
        }
        if (lower.endsWith(".js")) {
            return "application/javascript";
        }
        if (lower.endsWith(".php")) {
            return "application/x-httpd-php";
        }
        if (lower.endsWith(".html") || lower.endsWith(".htm")) {
            return "text/html";
        }
        if (lower.endsWith(".css")) {
            return "text/css";
        }
        if (lower.endsWith(".java")) {
            return "text/x-java-source";
        }
        if (lower.endsWith(".py")) {
            return "text/x-python";
        }
        if (lower.endsWith(".json")) {
            return "application/json";
        }
        if (lower.endsWith(".xml")) {
            return "application/xml";
        }
        if (lower.endsWith(".sql")) {
            return "application/sql";
        }
        if (lower.endsWith(".mp4")) {
            return "video/mp4";
        }
        if (lower.endsWith(".mov")) {
            return "video/quicktime";
        }
        if (lower.endsWith(".avi")) {
            return "video/x-msvideo";
        }
        if (lower.endsWith(".mkv")) {
            return "video/x-matroska";
        }
        if (lower.endsWith(".webm")) {
            return "video/webm";
        }
        if (lower.endsWith(".wmv")) {
            return "video/x-ms-wmv";
        }
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        return "application/octet-stream";
    }

    private String iconForAttachment(ChatMessageAttachment attachment) {
        String fileType = attachment.getFileType() == null ? "" : attachment.getFileType().toLowerCase();
        String fileName = attachment.getFileName() == null ? "" : attachment.getFileName().toLowerCase();
        String value = fileType + " " + fileName;
        if (value.contains("image/")) {
            return "🖼";
        }
        if (value.contains("pdf")) {
            return "📕";
        }
        if (value.contains("word") || value.contains(".doc")) {
            return "📘";
        }
        if (value.contains("presentation") || value.contains(".ppt")) {
            return "📙";
        }
        if (value.contains("excel") || value.contains(".xls") || value.contains(".csv")) {
            return "📗";
        }
        if (value.contains("video/") || value.contains(".mp4") || value.contains(".mov") || value.contains(".mkv")) {
            return "🎬";
        }
        if (value.contains("text/") || value.contains(".txt") || value.contains(".md")) {
            return "📄";
        }
        if (value.contains(".js") || value.contains(".php") || value.contains(".java") || value.contains(".py")
                || value.contains(".css") || value.contains(".html") || value.contains(".json")
                || value.contains(".xml") || value.contains(".sql")) {
            return "💻";
        }
        return "📎";
    }

    protected String buildFingerprint(List<ChatMessage> messages) {
        return messages.stream()
                .map(message -> message.getIdMessage() + ":" + message.isRead() + ":" + message.getAttachments().size())
                .collect(Collectors.joining("|"));
    }

    protected String buildAssistantFingerprint(List<ChatbotMessage> messages) {
        return messages.stream()
                .map(message -> message.getIdChatbotMessages() + ":" + message.getDate())
                .collect(Collectors.joining("|"));
    }

    protected void scrollToBottom() {
        Platform.runLater(() -> {
            if (messagesScrollPane != null) {
                messagesScrollPane.setVvalue(1.0);
            }
        });
    }
}
