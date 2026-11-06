package com.hirfa;

import com.hirfa.model.Outcome;
import com.hirfa.service.DiscountCalculator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DiscountCalculatorTest {
    private final DiscountCalculator calc = new DiscountCalculator();

    @Test
    void apply_blankCode_noDiscount() {
        Outcome out = calc.apply("", 50);
        assertTrue(out.ok());
        assertEquals(0, out.value(), 0.001);
    }

    @Test
    void apply_nullCode_noDiscount() {
        Outcome out = calc.apply(null, 50);
        assertTrue(out.ok());
        assertEquals(0, out.value(), 0.001);
    }

    @Test
    void apply_unknownCode_rejected() {
        Outcome out = calc.apply("خصم50", 50);
        assertFalse(out.ok());
        assertTrue(out.message().contains("غير معتمد"));
    }

    @Test
    void apply_hirfa10_belowMin_rejected() {
        Outcome out = calc.apply("حرفة10", 9.99);
        assertFalse(out.ok());
        assertTrue(out.message().contains("10 دنانير"));
    }

    @Test
    void apply_hirfa10_valid_discount() {
        Outcome out = calc.apply(" حرفة10 ", 40.5);
        assertTrue(out.ok());
        assertEquals(4.05, out.value(), 0.001);
    }

    @Test
    void apply_talaba5_valid_5percent() {
        Outcome out = calc.apply("طلاب5", 20);
        assertTrue(out.ok());
        assertEquals(1.0, out.value(), 0.001);
    }
}
