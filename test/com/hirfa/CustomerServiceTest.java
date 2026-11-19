package com.hirfa;

import com.hirfa.model.Outcome;
import com.hirfa.repo.Repo;
import com.hirfa.service.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CustomerServiceTest {
    private CustomerService service;

    @BeforeEach
    void setUp() {
        Repo repo = new Repo(":memory:");
        repo.seed();
        service = new CustomerService(repo);
        service.register("Amal Al-Khatib", "0791234567", "customer@example.com", "CustomerPass2026");
    }

    @Test
    void register_validData_createsAccount() {
        Outcome out = service.register("Tala Haddad", "0771112223", "tala@example.com", "TalaPass2026");
        assertTrue(out.ok());
        assertTrue(out.message().contains("account has been created"));
    }

    @Test
    void register_duplicateEmail_rejected() {
        Outcome out = service.register("Amal Al-Khatib", "0791234567", "customer@example.com", "CustomerPass2026");
        assertFalse(out.ok());
        assertTrue(out.message().contains("already registered"));
    }

    @Test
    void register_missingName_rejected() {
        assertFalse(service.register("  ", "0771112223", "tala@example.com", "TalaPass2026").ok());
    }

    @Test
    void register_nullName_rejected() {
        assertFalse(service.register(null, "0771112223", "tala@example.com", "TalaPass2026").ok());
    }

    @Test
    void register_nullPhone_rejected() {
        assertFalse(service.register("Tala Haddad", null, "tala@example.com", "TalaPass2026").ok());
    }

    @Test
    void register_nullEmail_rejected() {
        assertFalse(service.register("Tala Haddad", "0771112223", null, "TalaPass2026").ok());
    }

    @Test
    void register_nullPassword_rejected() {
        assertFalse(service.register("Tala Haddad", "0771112223", "tala@example.com", null).ok());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0712345678", "079123456", "07912345678", "0691234567", ""})
    void register_badPhone_rejected(String phone) {
        assertFalse(service.register("Tala Haddad", phone, "tala@example.com", "TalaPass2026").ok());
    }

    @ParameterizedTest
    @ValueSource(strings = {"tala@gmail", "tala#mail.com", "tala@com", "@gmail.com", ""})
    void register_badEmail_rejected(String email) {
        assertFalse(service.register("Tala Haddad", "0771112223", email, "TalaPass2026").ok());
    }

    @ParameterizedTest
    @ValueSource(strings = {"T@2", "12345678", "abcdefgh", ""})
    void register_weakPassword_rejected(String password) {
        assertFalse(service.register("Tala Haddad", "0771112223", "tala@example.com", password).ok());
    }

    @Test
    void login_correctCredentials_welcome() {
        Outcome out = service.login("customer@example.com", "CustomerPass2026");
        assertAll(
                () -> assertTrue(out.ok()),
                () -> assertTrue(out.message().contains("Welcome back")),
                () -> assertTrue(out.message().contains("Amal Al-Khatib"))
        );
    }

    @Test
    void login_unknownEmail_rejected() {
        Outcome out = service.login("nadia@example.com", "CustomerPass2026");
        assertFalse(out.ok());
        assertTrue(out.message().contains("not registered"));
    }

    @Test
    void login_wrongPassword_rejected() {
        Outcome out = service.login("customer@example.com", "WrongPass2026");
        assertFalse(out.ok());
        assertTrue(out.message().contains("Incorrect password"));
    }
}
