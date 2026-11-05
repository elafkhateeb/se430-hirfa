package com.hirfa;

import com.hirfa.service.DeliveryFeeCalculator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DeliveryFeeCalculatorTest {
    private final DeliveryFeeCalculator calc = new DeliveryFeeCalculator();

    @Test
    void fee_negativeDistance_invalid() {
        assertEquals(-1, calc.fee(50, -1), 0.001);
    }

    @ParameterizedTest
    @CsvSource({
            "50, 0.0, 1.0",
            "50, 5.0, 1.0",
            "50, 5.01, 2.0",
            "50, 15.0, 2.0",
            "50, 15.01, 3.5",
            "50, 30.0, 3.5",
            "50, 30.01, 3.501",
            "50, 40.0, 4.5",
            "50, 65.0, 7.0",
            "50, 95.0, 10.0",
            "50, 400.0, 10.0"
    })
    void fee_partitions(double subtotal, double km, double expected) {
        assertEquals(expected, calc.fee(subtotal, km), 0.001);
    }

    @Test
    void fee_freeShippingAt100() {
        assertEquals(0.0, calc.fee(100.0, 5.0), 0.001);
        assertEquals(1.0, calc.fee(99.99, 5.0), 0.001);
    }
}
