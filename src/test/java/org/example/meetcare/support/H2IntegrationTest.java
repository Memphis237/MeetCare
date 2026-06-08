package org.example.meetcare.support;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.UUID;

public abstract class H2IntegrationTest {
    protected TestDatabase.SeedData seed;

    @BeforeEach
    void setUpDatabase() {
        seed = TestDatabase.initialize(getClass().getSimpleName() + "_" + UUID.randomUUID());
    }

    @AfterEach
    void tearDownDatabase() {
        TestDatabase.resetConnection();
    }
}
