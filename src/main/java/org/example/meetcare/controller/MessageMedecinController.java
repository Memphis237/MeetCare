package org.example.meetcare.controller;

import javafx.fxml.FXML;
import org.example.meetcare.dao.MessagingDao;

import java.util.List;

public class MessageMedecinController extends AbstractChatController {
    @FXML
    private void initialize() {
        initializeChat();
    }

    @Override
    protected List<MessagingDao.ChatContact> loadHumanContacts() {
        if (currentUser == null) {
            return List.of();
        }
        return new MessagingDao().getDoctorContacts(currentUser.getIdUsers());
    }
}
