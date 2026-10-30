package com.hirfa.service;

import java.util.List;

public class ValidationService {
    public boolean validName(String name) {
        return name != null && !name.isBlank();
    }

    public boolean validPhone(String phone) {
        return phone != null && phone.matches("07[789]\\d{7}");
    }

    public boolean validEmail(String email) {
        return email != null && email.matches("[^@\\s]+@[^@\\s]+\\.[a-zA-Z]{2,}");
    }

    public boolean validPassword(String password) {
        return password != null && password.length() >= 8
                && password.matches(".*[a-zA-Z\\u0600-\\u06FF].*")
                && password.matches(".*\\d.*");
    }

    public boolean validCraft(String craft) {
        if (craft == null) return false;
        return List.of("تطريز", "صابون", "خشبيات", "جلوديات", "سيراميك", "مجوهرات").contains(craft);
    }
}
