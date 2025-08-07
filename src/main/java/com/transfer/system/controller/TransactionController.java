package com.transfer.system.controller;

import com.transfer.system.dto.CommonResponseDTO;
import com.transfer.system.dto.TransactionRequestDTO;
import com.transfer.system.dto.TransactionResponseDTO;
import com.transfer.system.enums.ResultCode;
import com.transfer.system.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;

@RestController
@RequestMapping("/api/transaction")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transfer")
    public ResponseEntity<CommonResponseDTO<TransactionResponseDTO>> transfer(@RequestBody TransactionRequestDTO transactionRequestDTO) {
        TransactionResponseDTO response = transactionService.transfer(transactionRequestDTO);

        return ResponseEntity.ok(CommonResponseDTO.successHasData(response, ResultCode.SUCCESS_HAS_DATA.getMessage()));
    }

    @GetMapping("/history")
    public ResponseEntity<CommonResponseDTO<Page<TransactionResponseDTO>>> getTransactionHistory(
        @RequestParam String accountNumber, @RequestParam int page, @RequestParam int size) {
        Page<TransactionResponseDTO> history = transactionService.getTransactionHistory(accountNumber, page, size);

        return ResponseEntity.ok(CommonResponseDTO.successHasData(history, ResultCode.SUCCESS_HAS_DATA.getMessage()));
    }
}