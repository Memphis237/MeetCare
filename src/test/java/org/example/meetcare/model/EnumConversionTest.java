package org.example.meetcare.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnumConversionTest {
    @Test
    void roleShouldFallbackToUserWhenUnknown() {
        assertEquals(Role.USER, Role.fromDatabaseValue(null));
        assertEquals(Role.USER, Role.fromDatabaseValue(" "));
        assertEquals(Role.ADMIN, Role.fromDatabaseValue("admin"));
        assertEquals(Role.MEDECIN, Role.fromDatabaseValue("MEDECIN"));
    }

    @Test
    void accountStatusShouldFallbackToPendingWhenUnknown() {
        assertEquals(AccountStatus.PENDING, AccountStatus.fromDatabaseValue(null));
        assertEquals(AccountStatus.PENDING, AccountStatus.fromDatabaseValue(" "));
        assertEquals(AccountStatus.VERIFIED, AccountStatus.fromDatabaseValue("verified"));
        assertEquals(AccountStatus.SUSPENDED, AccountStatus.fromDatabaseValue("SUSPENDED"));
    }
}
