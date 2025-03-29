package com.egr.snookerrank.beans;

import lombok.Getter;

@Getter
public enum PeriodType {
    MONTHLY(1),      // Represents 1 month
    QUARTERLY(3),    // Represents 3 months (1 quarter)
    BIANNUALLY(6),
    ANNUALLY(12);    // Represents 12 months (1 year)

    // Getter to retrieve the number of months for this period type
    private final int numberOfMonths;

    // Constructor to initialize the number of months
    PeriodType(int numberOfMonths) {
        this.numberOfMonths = numberOfMonths;
    }

}
