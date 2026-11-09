package com.hirfa.service;

public class EtaCalculator {

    public int estimate(double distanceKm, int currentLoad) {
        if (distanceKm < 0) return -1;
        int minutes = 30 + (int) Math.round(6 * distanceKm);
        if (distanceKm > 30) minutes += 60;
        if (currentLoad > 15) minutes += 45;
        return minutes;
    }
}
