package org.example.meetcare.dao;

import org.example.meetcare.model.AccountStatus;
import org.example.meetcare.model.Role;
import org.example.meetcare.model.Users;
import org.example.meetcare.support.H2IntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsersDaoTest extends H2IntegrationTest {
    private final UsersDao usersDao = new UsersDao();

    @Test
    void findAllUsersShouldReturnSeededAccounts() {
        List<Users> users = usersDao.findAllUsers();

        assertEquals(6, users.size());
        assertTrue(users.stream().anyMatch(user -> "patient@meetcare.test".equals(user.getEmail())));
    }

    @Test
    void findByIdentifierShouldResolveByEmailOrUsername() {
        Users byEmail = usersDao.findByIdentifier("patient@meetcare.test").orElseThrow();
        Users byUsername = usersDao.findByIdentifier("patient").orElseThrow();

        assertEquals(byEmail.getIdUsers(), byUsername.getIdUsers());
        assertEquals(Role.USER, byEmail.getRole());
        assertEquals(AccountStatus.VERIFIED, byEmail.getAccountStatus());
    }

    @Test
    void searchAllShouldMatchMultipleFields() {
        List<Users> matches = usersDao.searchAll("faure");

        assertEquals(1, matches.size());
        assertEquals("dr.faure@meetcare.test", matches.get(0).getEmail());
    }

    @Test
    void updateAccountStatusAndDeleteShouldMutateUsers() {
        Users pendingDoctor = usersDao.findByIdentifier("dr.dupont@meetcare.test").orElseThrow();
        usersDao.updateAccountStatus(pendingDoctor.getIdUsers(), AccountStatus.SUSPENDED.name());

        Users updated = usersDao.findById(pendingDoctor.getIdUsers()).orElseThrow();
        assertEquals(AccountStatus.SUSPENDED, updated.getAccountStatus());

        usersDao.deleteById(updated.getIdUsers());
        assertTrue(usersDao.findById(updated.getIdUsers()).isEmpty());
    }
}
