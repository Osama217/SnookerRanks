package com.egr.snookerrank.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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


    public static int stat2Value(Map<String, Object> statsMap, String fields) {
        int total = 0;
        if(null != fields) {
            String[] fieldNames = fields.split("\\+");

            for (String fieldName : fieldNames) {
                fieldName = fieldName.trim(); // clean up spaces
                total += safeInt(statsMap.get(fieldName));
            }
        }

        return total;
    }

    public static int safeInt(Object value) {
        if (value == null) return 0;

        if (value instanceof Integer) {
            return (Integer) value;
        }

        if (value instanceof Double) {
            double d = (Double) value;
            return (int) d; // cut decimals if present
        }

        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
