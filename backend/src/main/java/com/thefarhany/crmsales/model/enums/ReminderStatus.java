package com.thefarhany.crmsales.model.enums;

import lombok.Getter;

@Getter
public enum ReminderStatus {
    PENDING("Menunggu Pengiriman"),
    SENT("Berhasil Terkirim"),
    FAILED("Gagal Terkirim");

    private final String description;

    ReminderStatus(String description) {
        this.description = description;
    }
}
