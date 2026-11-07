package com.hirfa;

import com.hirfa.model.Outcome;
import com.hirfa.service.PaymentService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PaymentServiceTest {
    private final PaymentService service = new PaymentService();

    @Test
    void pay_cashUnderLimit_ok() {
        Outcome out = service.pay("CASH", 500, 0, null);
        assertTrue(out.ok());
        assertTrue(out.message().contains("نقداً"));
    }

    @Test
    void pay_cashOverLimit_rejected() {
        Outcome out = service.pay("CASH", 500.01, 0, null);
        assertFalse(out.ok());
        assertTrue(out.message().contains("500 دينار"));
    }

    @Test
    void pay_cardValid_ok() {
        Outcome out = service.pay("CARD", 40, 0, "4539148803436467");
        assertTrue(out.ok());
        assertTrue(out.message().contains("بالبطاقة"));
    }

    @Test
    void pay_cardBadNumber_rejected() {
        Outcome out = service.pay("CARD", 40, 0, "1234");
        assertFalse(out.ok());
        assertTrue(out.message().contains("16 رقماً"));
    }

    @Test
    void pay_cardLetters_rejected() {
        assertFalse(service.pay("CARD", 40, 0, "45391488ABCD6467").ok());
    }

    @Test
    void pay_walletEnough_ok() {
        Outcome out = service.pay("WALLET", 30, 35, null);
        assertTrue(out.ok());
        assertTrue(out.message().contains("المحفظة"));
    }

    @Test
    void pay_walletNotEnough_rejected() {
        Outcome out = service.pay("WALLET", 30.5, 25.5, null);
        assertFalse(out.ok());
        assertTrue(out.message().contains("غير كافٍ"));
    }

    @Test
    void pay_cardNullNumber_rejected() {
        assertFalse(service.pay("CARD", 40, 0, null).ok());
    }

    @Test
    void pay_nullMethod_rejected() {
        assertFalse(service.pay(null, 30, 0, null).ok());
    }

    @Test
    void pay_unknownMethod_rejected() {
        Outcome out = service.pay("BANK", 30, 0, null);
        assertFalse(out.ok());
        assertTrue(out.message().contains("غير معتمدة"));
    }
}
