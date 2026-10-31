package com.hirfa.service;

import com.hirfa.model.Customer;
import com.hirfa.model.Outcome;
import com.hirfa.repo.Repo;

public class CustomerService {
    public static final double MIN_PASSWORD_LENGTH = 8;
    private final Repo repo;
    private final ValidationService validation;

    public CustomerService(Repo repo) {
        this.repo = repo;
        this.validation = new ValidationService();
    }

    public Outcome register(String name, String phone, String email, String password) {
        if (!validation.validName(name)) return Outcome.fail("الاسم مطلوب");
        if (!validation.validPhone(phone)) return Outcome.fail("رقم الهاتف غير صحيح، مثال: 0791234567");
        if (!validation.validEmail(email)) return Outcome.fail("البريد الإلكتروني غير صحيح");
        if (!validation.validPassword(password)) return Outcome.fail("كلمة المرور ضعيفة: 8 خانات على الأقل مع أحرف وأرقام");
        if (repo.customerByEmail(email) != null) return Outcome.fail("البريد الإلكتروني مسجل مسبقاً");
        int id = repo.insertCustomer(new Customer(0, name.trim(), phone, email, password, 0, 0));
        return new Outcome(true, "مرحباً " + name.trim() + "، تم إنشاء حسابك بنجاح", id);
    }

    public Outcome login(String email, String password) {
        Customer c = repo.customerByEmail(email);
        if (c == null) return Outcome.fail("البريد الإلكتروني غير مسجل");
        if (!c.password.equals(password)) return Outcome.fail("كلمة المرور غير صحيحة");
        return Outcome.ok("مرحباً بك مجدداً، " + c.name, c.id);
    }
}
