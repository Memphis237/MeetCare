package org.example.meetcare.controller;

import org.example.meetcare.model.DoctorDocumentEntry;
import org.example.meetcare.support.TestDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationAdminControllerTest {
    private ValidationAdminController controller;

    @BeforeEach
    void setUp() {
        TestDatabase.initialize("validation_admin_test");
        controller = new ValidationAdminController();
    }

    @AfterEach
    void tearDown() {
        TestDatabase.resetConnection();
    }

    @Test
    void helperMethodsShouldNormalizeAndFormatRequirements() throws Exception {
        DoctorDocumentEntry entry = new DoctorDocumentEntry();
        entry.setUsername("  Sophie  ");
        entry.setEmail("sophie@example.test");

        assertEquals("Sophie", invokeString(controller, "displayName", DoctorDocumentEntry.class, entry));
        assertEquals("D", invokeString(controller, "initialsOf", String.class, " "));
        assertEquals("PASSEPORT", invokeString(controller, "normalize", String.class, " passeport "));
        assertTrue(invokeBoolean(controller, "isMandatoryDocument", String.class, "CARTE_VITALE"));
        assertFalse(invokeBoolean(controller, "isMandatoryDocument", String.class, "PHOTO_PRO"));
        assertEquals("Dossier complet: validation autorisee.",
                invokeString(controller, "buildRequirementLabel", List.class, List.of()));
        assertTrue(invokeString(controller, "documentStatusStyle", String.class, "APPROVED").contains("#15803D"));
    }

    private String invokeString(Object target, String methodName, Class<?> parameterType, Object argument) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, parameterType);
        method.setAccessible(true);
        return (String) method.invoke(target, argument);
    }

    private boolean invokeBoolean(Object target, String methodName, Class<?> parameterType, Object argument) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, parameterType);
        method.setAccessible(true);
        return (Boolean) method.invoke(target, argument);
    }
}
