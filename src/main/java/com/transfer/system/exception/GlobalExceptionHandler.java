package com.transfer.system.exception;

import com.transfer.system.dto.CommonResponseDTO;
import com.transfer.system.enums.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
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
        log.error("[INTERNAL_ERROR] : {}", ex.getMessage(), ex);
        return ResponseEntity
            .status(ErrorCode.INTERNAL_ERROR.getStatus())
            .body(CommonResponseDTO.failure(ResultCode.ERROR_SERVER, ErrorCode.INTERNAL_ERROR.getMessage()));
    }
}