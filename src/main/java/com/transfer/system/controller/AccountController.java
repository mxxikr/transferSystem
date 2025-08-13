package com.transfer.system.controller;

import com.transfer.system.dto.*;
import com.transfer.system.enums.ResponseMessage;
import com.transfer.system.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "계좌 API", description = "계좌 생성, 조회, 삭제, 입출금 관련 API")
@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;

    @Operation(summary = "계좌 생성")
    @PostMapping("/create")
    public ResponseEntity<CommonResponseDTO<AccountResponseDTO>> createAccount(@RequestBody AccountCreateRequestDTO AccountCreateRequestDTO) {
        AccountResponseDTO response = accountService.createAccount(AccountCreateRequestDTO);

        return ResponseEntity.ok(CommonResponseDTO.successHasData(response, ResponseMessage.ACCOUNT_CREATED.getMessage()));
    }

    @Operation(summary = "계좌 조회")
    @GetMapping("/{accountId}")
    public ResponseEntity<CommonResponseDTO<AccountResponseDTO>> getAccount(@PathVariable UUID accountId) {
        AccountResponseDTO response = accountService.getAccount(accountId);

        return ResponseEntity.ok(CommonResponseDTO.successHasData(response, ResponseMessage.ACCOUNT_RETRIEVED.getMessage()));
    }

    @Operation(summary = "계좌 삭제")
    @DeleteMapping("/{accountId}")
    public ResponseEntity<CommonResponseDTO<Void>> deleteAccount(@PathVariable UUID accountId) {
        accountService.deleteAccount(accountId);

        return ResponseEntity.ok(CommonResponseDTO.successNoData(ResponseMessage.ACCOUNT_DELETED.getMessage()));
    }

    @Operation(summary = "입금 처리")
    @PostMapping("/deposit")
    public ResponseEntity<CommonResponseDTO<AccountBalanceResponseDTO>> deposit(@Valid @RequestBody AccountBalanceRequestDTO accountBalanceRequestDTO) {
        AccountBalanceResponseDTO response = accountService.deposit(accountBalanceRequestDTO.getAccountNumber(), accountBalanceRequestDTO.getAmount());

        return ResponseEntity.ok(CommonResponseDTO.successHasData(response, ResponseMessage.DEPOSIT_SUCCESSFUL.getMessage()));
    }

    @Operation(summary = "출금 처리", description = "일 한도 : 1,000,000원")
    @PostMapping("/withdraw")
    public ResponseEntity<CommonResponseDTO<AccountBalanceResponseDTO>> withdraw(@Valid @RequestBody AccountBalanceRequestDTO accountBalanceRequestDTO) {
        AccountBalanceResponseDTO response = accountService.withdraw(accountBalanceRequestDTO.getAccountNumber(), accountBalanceRequestDTO.getAmount());

        return ResponseEntity.ok(CommonResponseDTO.successHasData(response, ResponseMessage.WITHDRAW_SUCCESSFUL.getMessage()));
    }
}