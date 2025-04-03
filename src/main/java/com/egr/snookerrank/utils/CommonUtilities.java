package com.egr.snookerrank.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

public class CommonUtilities {
    public static List<Integer> generateYearList() {
        List<Integer> years = new ArrayList<>();
        int currentYear = Year.now().getValue(); // Get the current year

        for (int year = 2011; year <= currentYear; year++) {
            years.add(year);
        }

        return years;
    }
    public static <T extends Number> boolean isGreaterThan(T num, T value) {
        return num.doubleValue() > value.doubleValue();
    }
    public static Double roundToTwoDecimals(Double value) {
        if (value == null) return null;  // Handle null safely
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
