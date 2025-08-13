package com.transfer.system.controller;

import com.transfer.system.dto.AccountBalanceRequestDTO;
import com.transfer.system.dto.AccountCreateRequestDTO;
import com.transfer.system.dto.AccountResponseDTO;
import com.transfer.system.dto.CommonResponseDTO;
import com.transfer.system.enums.ResponseMessage;
import com.transfer.system.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
@Tag(name = "계좌 API", description = "계좌 생성, 조회, 삭제, 입출금 관련 API")
public class AccountController {
    private final AccountService accountService;

    @PostMapping("/create")
    @Operation(summary = "계좌 생성")
    public ResponseEntity<CommonResponseDTO<AccountResponseDTO>> createAccount(@RequestBody AccountCreateRequestDTO AccountCreateRequestDTO) {
        AccountResponseDTO response = accountService.createAccount(AccountCreateRequestDTO);

        return ResponseEntity.ok(CommonResponseDTO.successHasData(response, ResponseMessage.ACCOUNT_CREATED.getMessage()));
    }

    @GetMapping("/{accountId}")
    @Operation(summary = "계좌 조회")
    public ResponseEntity<CommonResponseDTO<AccountResponseDTO>> getAccount(@PathVariable UUID accountId) {
        AccountResponseDTO response = accountService.getAccount(accountId);

        return ResponseEntity.ok(CommonResponseDTO.successHasData(response, ResponseMessage.ACCOUNT_RETRIEVED.getMessage()));
    }

    @DeleteMapping("/{accountId}")
    @Operation(summary = "계좌 삭제")
    public ResponseEntity<CommonResponseDTO<Void>> deleteAccount(@PathVariable UUID accountId) {
        accountService.deleteAccount(accountId);

        return ResponseEntity.ok(CommonResponseDTO.successNoData(ResponseMessage.ACCOUNT_DELETED.getMessage()));
    }

    @PostMapping("/deposit")
    @Operation(summary = "입금 처리")
    public ResponseEntity<CommonResponseDTO<Void>> deposit(@RequestBody AccountBalanceRequestDTO accountBalanceRequestDTO) {
        accountService.deposit(accountBalanceRequestDTO.getAccountNumber(), accountBalanceRequestDTO.getAmount());

        return ResponseEntity.ok(CommonResponseDTO.successNoData(ResponseMessage.DEPOSIT_SUCCESSFUL.getMessage()));
    }

    @PostMapping("/withdraw")
    @Operation(summary = "출금 처리", description = "일 한도 : 1,000,000원")
    public ResponseEntity<CommonResponseDTO<Void>> withdraw(@RequestBody AccountBalanceRequestDTO accountBalanceRequestDTO) {
        accountService.withdraw((accountBalanceRequestDTO.getAccountNumber()), accountBalanceRequestDTO.getAmount());

        return ResponseEntity.ok(CommonResponseDTO.successNoData(ResponseMessage.WITHDRAW_SUCCESSFUL.getMessage()));
    }
}