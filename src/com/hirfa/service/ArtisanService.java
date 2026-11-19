package com.hirfa.service;

import com.hirfa.model.Artisan;
import com.hirfa.model.Outcome;
import com.hirfa.repo.Repo;

public class ArtisanService {
    private final Repo repo;
    private final ValidationService validation = new ValidationService();

    public ArtisanService(Repo repo) {
        this.repo = repo;
    }

    public Outcome register(String name, String phone, String craft, String email, String password) {
        if (!validation.validName(name)) return Outcome.fail("Enter a valid name using English letters.");
        if (!validation.validPhone(phone)) return Outcome.fail("Enter a valid Jordanian mobile number.");
        if (!validation.validCraft(craft)) return Outcome.fail("Choose a supported craft.");
        if (!validation.validEmail(email)) return Outcome.fail("Enter a valid email address.");
        if (!validation.validPassword(password)) return Outcome.fail("Password must contain at least 8 characters, letters, and numbers.");
        String normalizedEmail = email.trim().toLowerCase();
        if (repo.artisanByEmail(normalizedEmail) != null) return Outcome.fail("An artisan account already uses this email.");
        int id = repo.insertArtisan(new Artisan(0, name.trim(), craft, normalizedEmail, password, 0, 0));
        return new Outcome(true, "Artisan account created for " + name.trim() + ".", id);
    }

    public Outcome login(String email, String password) {
        Artisan artisan = email == null ? null : repo.artisanByEmail(email.trim().toLowerCase());
        if (artisan == null) return Outcome.fail("Email is not registered.");
        if (artisan.password == null || !artisan.password.equals(password))
            return Outcome.fail("Incorrect password.");
        return Outcome.ok("Welcome back, " + artisan.name + ".", artisan.id);
    }

    public Outcome rateArtisan(int artisanId, int stars) {
        if (stars < 1 || stars > 5) return Outcome.fail("Rating must be between 1 and 5.");
        if (repo.artisan(artisanId) == null) return Outcome.fail("Artisan not found.");
        repo.rateArtisan(artisanId, stars);
        Artisan a = repo.artisan(artisanId);
        return new Outcome(true, "Thank you for rating " + a.name + ". The new average is " + a.rating() + ".", a.rating());
    }
}
