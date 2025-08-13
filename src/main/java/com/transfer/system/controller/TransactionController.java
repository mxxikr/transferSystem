package com.transfer.system.controller;

import com.transfer.system.dto.CommonResponseDTO;
import com.transfer.system.dto.TransactionRequestDTO;
import com.transfer.system.dto.TransactionResponseDTO;
import com.transfer.system.enums.ResponseMessage;
import com.transfer.system.enums.ResultCode;
import com.transfer.system.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;

@RestController
@RequestMapping("/api/transaction")
@RequiredArgsConstructor
@Tag(name = "거래 API", description = "계좌 간 이체 및 거래 내역 조회 API")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transfer")
    @Operation(summary = "계좌 이체", description = "이체 수수료 : 1%")
    public ResponseEntity<CommonResponseDTO<TransactionResponseDTO>> transfer(@RequestBody TransactionRequestDTO transactionRequestDTO) {
        TransactionResponseDTO response = transactionService.transfer(transactionRequestDTO);

        return ResponseEntity.ok(CommonResponseDTO.successHasData(response, ResponseMessage.TRANSFER_SUCCESSFUL.getMessage()));
    }

    @GetMapping("/history")
    @Operation(summary = "거래 내역 조회", description = "거래 내역 최신 순 조회")
    public ResponseEntity<CommonResponseDTO<Page<TransactionResponseDTO>>> getTransactionHistory(
        @RequestParam String accountNumber, @RequestParam int page, @RequestParam int size) {
        Page<TransactionResponseDTO> history = transactionService.getTransactionHistory(accountNumber, page, size);

        return ResponseEntity.ok(CommonResponseDTO.successHasData(history, ResponseMessage.TRANSACTION_HISTORY_RETRIEVED.getMessage()));
    }
}