package com.transfer.system.exception;

import com.transfer.system.dto.CommonResponseDTO;
import com.transfer.system.enums.ResultCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(TransferSystemException.class)
    public ResponseEntity<CommonResponseDTO<Void>> handleTransferException(TransferSystemException ex) {
        ErrorCode code = ex.getErrorCode();
        return ResponseEntity
                .status(code.getStatus())
                .body(CommonResponseDTO.failure(ResultCode.FAIL_DATA_ERROR, code.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResponseDTO<Void>> handleUnexpected(Exception ex) {
        ex.printStackTrace();
        return ResponseEntity
                .status(ResultCode.ERROR_SERVER.getCode())
                .body(CommonResponseDTO.failure(ResultCode.ERROR_SERVER, "예기치 못한 오류가 발생했습니다."));
    }
}