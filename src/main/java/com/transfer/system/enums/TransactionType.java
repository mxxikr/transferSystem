package com.transfer.system.enums;

public enum TransactionType {
    DEPOSIT("입금"),
    WITHDRAWAL("출금"),
    TRANSFER("이체");

    private final String label;

    TransactionType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}