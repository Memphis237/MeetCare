package org.example.meetcare.dao;

import org.example.meetcare.model.NotificationEntry;
import org.example.meetcare.support.H2IntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationDaoTest extends H2IntegrationTest {
    @Test
    void unreadCounterAndFilteringShouldWork() {
        NotificationDao notificationDao = new NotificationDao();
        List<NotificationEntry> unread = notificationDao.findNotificationsForUser(seed.patientUserId(), true);

        assertEquals(2, notificationDao.countUnreadForUser(seed.patientUserId()));
        assertEquals(2, unread.size());
    }

    @Test
    void markAsReadAndMarkAllAsReadShouldUpdateRows() {
        NotificationDao notificationDao = new NotificationDao();
        notificationDao.markAsRead(seed.unreadNotificationId(), seed.patientUserId());
        assertEquals(1, notificationDao.countUnreadForUser(seed.patientUserId()));

        notificationDao.markAllAsRead(seed.patientUserId());
        assertEquals(0, notificationDao.countUnreadForUser(seed.patientUserId()));
    }

    @Test
    void createNotificationForUsersShouldSkipInvalidIds() {
        NotificationDao notificationDao = new NotificationDao();
        notificationDao.createNotificationForUsers(Arrays.asList(seed.patientUserId(), 0, null, -5),
                "Test", "Hello", "SYSTEM");

        assertTrue(notificationDao.findNotificationsForUser(seed.patientUserId(), false).stream()
                .anyMatch(notification -> "Test".equals(notification.getTitle())));
    }
}
