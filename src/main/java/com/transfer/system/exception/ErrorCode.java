package com.transfer.system.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // ACCOUNT
    ACCOUNT_NOT_FOUND("계좌를 찾을 수 없습니다.",HttpStatus.NOT_FOUND),
    DUPLICATE_ACCOUNT_NUMBER("이미 존재하는 계좌 번호입니다.", HttpStatus.CONFLICT),
    TRANSFER_SAME_ACCOUNT("같은 계좌로 이체할 수 없습니다.", HttpStatus.BAD_REQUEST),
    EXCEEDS_WITHDRAW_LIMIT("출금 한도를 초과했습니다.", HttpStatus.BAD_REQUEST),

    // TRANSACTION
    INSUFFICIENT_BALANCE("잔액이 부족합니다.", HttpStatus.BAD_REQUEST),
    NEGATIVE_BALANCE_NOT_ALLOWED("잔액은 음수가 될 수 없습니다..", HttpStatus.BAD_REQUEST),
    TRANSFER_LIMIT_EXCEEDED("이체 한도를 초과했습니다.", HttpStatus.BAD_REQUEST),
    RECEIVER_ACCOUNT_INACTIVE("수신 계좌가 비활성화 상태입니다.", HttpStatus.BAD_REQUEST),

    // ETC
    INTERNAL_ERROR("서버 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);


    private final String message;
    private final HttpStatus status;

    ErrorCode(String message, HttpStatus status) {
        this.message = message;
        this.status = status;
    }
}