package com.thefarhany.crmsales.model.enums;

import lombok.Getter;

@Getter
public enum ReminderType {
    DAY_60("60 Hari Sebelum Expire"),
    DAY_30("30 Hari Sebelum Expire"),
    DAY_7("7 Hari Sebelum Expire"),
    DAY_1("1 Hari Sebelum Expire");

    private final String description;

    ReminderType(String description) {
        this.description = description;
    }

    public static ReminderType fromDays(int days) {
        return switch (days) {
            case 60 -> DAY_60;
            case 30 -> DAY_30;
            case 7 -> DAY_7;
            case 1 -> DAY_1;
            default -> throw new IllegalArgumentException("Unsupported reminder days: " + days);
        };
    }

    public int toDays() {
        return switch (this) {
            case DAY_60 -> 60;
            case DAY_30 -> 30;
            case DAY_7 -> 7;
            case DAY_1 -> 1;
        };
    }
}
