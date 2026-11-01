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
        Outcome out = service.register("نادية القادرية", "0793344556", "جلوديات");
        assertTrue(out.ok());
        assertTrue(out.message().contains("تم تسجيل"));
        assertEquals(4, (int) out.value());
    }

    @Test
    void register_missingName_rejected() {
        assertFalse(service.register("", "0793344556", "صابون").ok());
    }

    @ParameterizedTest
    @ValueSource(strings = {"07933445561", "0713344556", "", "079334455"})
    void register_badPhone_rejected(String phone) {
        assertFalse(service.register("نادية القادرية", phone, "صابون").ok());
    }

    @ParameterizedTest
    @ValueSource(strings = {"طباعة", "ب instances", "", "keramik"})
    void register_unknownCraft_rejected(String craft) {
        assertFalse(service.register("نادية القادرية", "0793344556", craft).ok());
    }

    @Test
    void register_nullCraft_rejected() {
        assertFalse(service.register("نادية القادرية", "0793344556", null).ok());
    }

    @Test
    void rating_zeroRatings_showsZero() {
        Artisan fresh = new Artisan(99, "لم تقيّم بعد", "صابون", 0, 0);
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
        assertTrue(out.message().contains("بين 1 و 5"));
    }

    @Test
    void rateArtisan_sixStars_rejected() {
        assertFalse(service.rateArtisan(1, 6).ok());
    }

    @Test
    void rateArtisan_unknownArtisan_rejected() {
        Outcome out = service.rateArtisan(99, 4);
        assertFalse(out.ok());
        assertTrue(out.message().contains("غير موجود"));
    }

}
