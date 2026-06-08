package org.example.meetcare.controller;

import org.example.meetcare.model.AccountStatus;
import org.example.meetcare.model.Role;
import org.example.meetcare.model.Users;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UsersAdminControllerTest {
    @Test
    void helperMethodsShouldFormatLabelsAndStatuses() throws Exception {
        UsersAdminController controller = new UsersAdminController();
        Users user = new Users();
        user.setPrenom("Claire");
        user.setUsername("dupont");
        user.setRole(Role.MEDECIN);
        user.setAccountStatus(AccountStatus.SUSPENDED);

        assertEquals("Claire dupont", invokeString(controller, "displayName", Users.class, user));
        assertEquals("Médecin", invokeString(controller, "roleLabel", Role.class, Role.MEDECIN));
        assertEquals("Suspendu", invokeString(controller, "displayStatus", AccountStatus.class, AccountStatus.SUSPENDED));
        assertEquals("doctor-admin-status-rejected", invokeString(controller, "statusStyle", AccountStatus.class, AccountStatus.SUSPENDED));
        assertEquals(1, invokeInt(controller, "roleRank", Role.class, Role.MEDECIN));
        assertEquals("CD", invokeString(controller, "initialsOf", String.class, "Claire Dupont"));
        assertEquals("#0ea5e9", invokeString(controller, "avatarColor", Role.class, Role.MEDECIN));
        assertEquals("-", invokeString(controller, "safeText", String.class, (Object) null));
    }

    private String invokeString(Object target, String methodName, Class<?> parameterType, Object argument) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, parameterType);
        method.setAccessible(true);
        return (String) method.invoke(target, argument);
    }

    private int invokeInt(Object target, String methodName, Class<?> parameterType, Object argument) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, parameterType);
        method.setAccessible(true);
        return (Integer) method.invoke(target, argument);
    }
}
