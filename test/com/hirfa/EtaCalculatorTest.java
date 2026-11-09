package com.hirfa;

import com.hirfa.service.EtaCalculator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class EtaCalculatorTest {
    private final EtaCalculator calculator = new EtaCalculator();

    @Test
    void estimate_negative_invalid() {
        assertEquals(-1, calculator.estimate(-5, 0));
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, 5, 10, 25, 30})
    void estimate_normalPartition(double km) {
        assertEquals(30 + (int) Math.round(6 * km), calculator.estimate(km, 0));
    }

    @Test
    void estimate_at30_noExtraHour() {
        assertEquals(210, calculator.estimate(30, 0));
    }

    @Test
    void estimate_over31_addsAnHour() {
        assertEquals(30 + 6 * 31 + 60, calculator.estimate(31, 0));
    }

    @Test
    void estimate_peakLoad_adds45() {
        assertEquals(30 + 6 * 20 + 45, calculator.estimate(20, 16));
    }

    @Test
    void estimate_notPeak_boundary() {
        assertEquals(30 + 6 * 20, calculator.estimate(20, 15));
    }

    @Test
    void estimate_allConditions() {
        assertEquals(30 + 6 * 40 + 60 + 45, calculator.estimate(40, 20));
    }
}
