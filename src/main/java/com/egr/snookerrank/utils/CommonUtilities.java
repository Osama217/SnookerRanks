package com.egr.snookerrank.utils;

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
}
