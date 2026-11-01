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

    public Outcome register(String name, String phone, String craft) {
        if (!validation.validName(name)) return Outcome.fail("الاسم مطلوب");
        if (!validation.validPhone(phone)) return Outcome.fail("رقم الهاتف غير صحيح، مثال: 0791234567");
        if (!validation.validCraft(craft)) return Outcome.fail("الفئة غير معتمدة: تطريز، صابون، خشبيات، جلوديات، سيراميك، مجوهرات");
        int id = repo.insertArtisan(new Artisan(0, name.trim(), craft, 0, 0));
        return new Outcome(true, "تم تسجيل " + name.trim() + " كحرفي في فئة " + craft, id);
    }

    public Outcome rateArtisan(int artisanId, int stars) {
        if (stars < 1 || stars > 5) return Outcome.fail("التقييم يجب أن يكون بين 1 و 5");
        if (repo.artisan(artisanId) == null) return Outcome.fail("الحرفي غير موجود");
        repo.rateArtisan(artisanId, stars);
        Artisan a = repo.artisan(artisanId);
        return new Outcome(true, "شكراً لتقييمك، متوسط تقييم " + a.name + " صار " + a.rating(), a.rating());
    }
}
