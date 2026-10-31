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
    }

    @Test
    void register_validData_createsAccount() {
        Outcome out = service.register("تالا حداد", "0771112223", "tala@gmail.com", "Tala@2024");
        assertTrue(out.ok());
        assertTrue(out.message().contains("تم إنشاء حسابك بنجاح"));
    }

    @Test
    void register_duplicateEmail_rejected() {
        Outcome out = service.register("أمل الخطيب", "0791234567", "amal@gmail.com", "Amal@2024");
        assertFalse(out.ok());
        assertTrue(out.message().contains("مسجل مسبقاً"));
    }

    @Test
    void register_missingName_rejected() {
        assertFalse(service.register("  ", "0771112223", "tala@gmail.com", "Tala@2024").ok());
    }

    @Test
    void register_nullName_rejected() {
        assertFalse(service.register(null, "0771112223", "tala@gmail.com", "Tala@2024").ok());
    }

    @Test
    void register_nullPhone_rejected() {
        assertFalse(service.register("تالا حداد", null, "tala@gmail.com", "Tala@2024").ok());
    }

    @Test
    void register_nullEmail_rejected() {
        assertFalse(service.register("تالا حداد", "0771112223", null, "Tala@2024").ok());
    }

    @Test
    void register_nullPassword_rejected() {
        assertFalse(service.register("تالا حداد", "0771112223", "tala@gmail.com", null).ok());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0712345678", "079123456", "07912345678", "0691234567", ""})
    void register_badPhone_rejected(String phone) {
        assertFalse(service.register("تالا حداد", phone, "tala@gmail.com", "Tala@2024").ok());
    }

    @ParameterizedTest
    @ValueSource(strings = {"tala@gmail", "tala#mail.com", "tala@com", "@gmail.com", ""})
    void register_badEmail_rejected(String email) {
        assertFalse(service.register("تالا حداد", "0771112223", email, "Tala@2024").ok());
    }

    @ParameterizedTest
    @ValueSource(strings = {"T@2", "12345678", "abcdefgh", ""})
    void register_weakPassword_rejected(String password) {
        assertFalse(service.register("تالا حداد", "0771112223", "tala@gmail.com", password).ok());
    }

    @Test
    void login_correctCredentials_welcome() {
        Outcome out = service.login("amal@gmail.com", "Amal@2024");
        assertAll(
                () -> assertTrue(out.ok()),
                () -> assertTrue(out.message().contains("مرحباً")),
                () -> assertTrue(out.message().contains("أمل الخطيب"))
        );
    }

    @Test
    void login_unknownEmail_rejected() {
        Outcome out = service.login("nadia@gmail.com", "Amal@2024");
        assertFalse(out.ok());
        assertTrue(out.message().contains("غير مسجل"));
    }

    @Test
    void login_wrongPassword_rejected() {
        Outcome out = service.login("amal@gmail.com", "Wrong@123");
        assertFalse(out.ok());
        assertTrue(out.message().contains("غير صحيحة"));
    }
}
