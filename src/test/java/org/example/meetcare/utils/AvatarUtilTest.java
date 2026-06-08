package org.example.meetcare.utils;

import org.example.meetcare.model.Users;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AvatarUtilTest {
    @Test
    void buildInitialsShouldHandleMissingValues() {
        Users user = new Users();
        assertEquals("U", AvatarUtil.buildInitials(null));
        assertEquals("U", AvatarUtil.buildInitials(user));
    }

    @Test
    void buildInitialsShouldUseFirstnameAndUsername() {
        Users user = new Users();
        user.setPrenom("  claire ");
        user.setUsername(" dupont ");

        assertEquals("CD", AvatarUtil.buildInitials(user));
    }

    @Test
    void refreshUserShouldReturnSameUserWhenIdIsMissing() {
        Users user = new Users();
        user.setIdUsers(0);

        assertSame(user, AvatarUtil.refreshUser(user));
    }
}
