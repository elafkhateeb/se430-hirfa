package com.hirfa;

import com.hirfa.model.Artisan;
import com.hirfa.model.Outcome;
import com.hirfa.repo.Repo;
import com.hirfa.service.ArtisanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ArtisanServiceTest {
    private ArtisanService service;

    @BeforeEach
    void setUp() {
        Repo repo = new Repo(":memory:");
        repo.seed();
        service = new ArtisanService(repo);
    }

    @Test
    void register_validArtisan_created() {
        Outcome out = service.register("Nadia Qadri", "0793344556", "Leather",
                "nadia@example.com", "NadiaPass2026");
        assertTrue(out.ok());
        assertTrue(out.message().contains("Artisan account created"));
        assertEquals(4, (int) out.value());
    }

    @Test
    void register_artisanAccount_canLogIn() {
        Outcome registration = service.register("Nadia Qadri", "0793344556", "Leather",
                "nadia@example.com", "NadiaPass2026");
        assertTrue(registration.ok());
        Outcome login = service.login("nadia@example.com", "NadiaPass2026");
        assertTrue(login.ok());
        assertEquals(registration.value(), login.value());
    }

    @Test
    void register_artisanAccount_duplicateEmail_rejected() {
        service.register("Nadia Qadri", "0793344556", "Leather", "nadia@example.com", "NadiaPass2026");
        Outcome duplicate = service.register("Another Artisan", "0793344557", "Leather",
                "nadia@example.com", "AnotherPass2026");
        assertFalse(duplicate.ok());
    }

    @Test
    void login_artisanAccount_wrongPassword_rejected() {
        service.register("Nadia Qadri", "0793344556", "Leather", "nadia@example.com", "NadiaPass2026");
        assertFalse(service.login("nadia@example.com", "WrongPass2026").ok());
    }

    @Test
    void register_missingName_rejected() {
        assertFalse(service.register("", "0793344556", "Soap", "nadia@example.com", "NadiaPass2026").ok());
    }

    @ParameterizedTest
    @ValueSource(strings = {"07933445561", "0713344556", "", "079334455"})
    void register_badPhone_rejected(String phone) {
        assertFalse(service.register("Nadia Qadri", phone, "Soap", "nadia@example.com", "NadiaPass2026").ok());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Printing", "Paper Goods", "", "keramik"})
    void register_unknownCraft_rejected(String craft) {
        assertFalse(service.register("Nadia Qadri", "0793344556", craft,
                "nadia@example.com", "NadiaPass2026").ok());
    }

    @Test
    void register_nullCraft_rejected() {
        assertFalse(service.register("Nadia Qadri", "0793344556", null,
                "nadia@example.com", "NadiaPass2026").ok());
    }

    @Test
    void rating_zeroRatings_showsZero() {
        Artisan fresh = new Artisan(99, "Not Yet Rated", "Soap", 0, 0);
        assertEquals(0.0, fresh.rating());
    }

    @Test
    void rateArtisan_validStars_updatesAverage() {
        Outcome out = service.rateArtisan(1, 5);
        assertTrue(out.ok());
        assertEquals(4.3, out.value(), 0.01);
    }

    @Test
    void rateArtisan_zeroStars_rejected() {
        Outcome out = service.rateArtisan(1, 0);
        assertFalse(out.ok());
        assertTrue(out.message().contains("between 1 and 5"));
    }

    @Test
    void rateArtisan_sixStars_rejected() {
        assertFalse(service.rateArtisan(1, 6).ok());
    }

    @Test
    void rateArtisan_unknownArtisan_rejected() {
        Outcome out = service.rateArtisan(99, 4);
        assertFalse(out.ok());
        assertTrue(out.message().contains("not found"));
    }

}
