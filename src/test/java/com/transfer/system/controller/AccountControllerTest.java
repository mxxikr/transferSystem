package com.transfer.system.controller;

import com.transfer.system.dto.AccountBalanceRequestDTO;
import com.transfer.system.dto.AccountCreateRequestDTO;
import com.transfer.system.dto.AccountResponseDTO;
import com.transfer.system.enums.AccountStatus;
import com.transfer.system.enums.AccountType;
import com.transfer.system.enums.CurrencyType;
import com.transfer.system.enums.ResultCode;
import com.transfer.system.exception.ErrorCode;
import com.transfer.system.exception.GlobalExceptionHandler;
import com.transfer.system.exception.TransferSystemException;
import com.transfer.system.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AccountService accountService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AccountCreateRequestDTO validRequestDTO;
    private AccountResponseDTO mockResponseDTO;

    @BeforeEach
    void setUp() {
        AccountController accountController = new AccountController(accountService);
        mockMvc = MockMvcBuilders.standaloneSetup(accountController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

        validRequestDTO = AccountCreateRequestDTO.builder()
            .accountNumber("account123")
            .accountName("mxxikr")
            .bankName("mxxikrBank")
            .accountType(AccountType.PERSONAL)
            .currencyType(CurrencyType.KRW)
            .balance(new BigDecimal("100000"))
            .accountStatus(AccountStatus.ACTIVE)
            .build();

        mockResponseDTO = AccountResponseDTO.builder()
            .accountId(UUID.randomUUID())
            .accountNumber("account123")
            .accountName("mxxikr")
            .bankName("mxxikrBank")
            .accountType(AccountType.PERSONAL)
            .currencyType(CurrencyType.KRW)
            .balance(new BigDecimal("100000"))
            .accountStatus(AccountStatus.ACTIVE)
            .createdTimeStamp(LocalDateTime.now())
            .build();
    }

    // ========================= 계좌 생성 테스트 =========================
    @Nested
    class CreateAccountTest {

        @Test
        void createAccount_success() throws Exception {
            when(accountService.createAccount(any(AccountCreateRequestDTO.class)))
                .thenReturn(mockResponseDTO);

            mockMvc.perform(post("/api/account/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequestDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.result_code").value(ResultCode.SUCCESS_HAS_DATA.getCode()))
                .andExpect(jsonPath("$.data.accountNumber").value("account123"))
                .andExpect(jsonPath("$.data.accountName").value("mxxikr"))
                .andExpect(jsonPath("$.data.bankName").value("mxxikrBank"))
                .andExpect(jsonPath("$.data.accountType").value("PERSONAL"))
                .andExpect(jsonPath("$.data.currencyType").value("KRW"))
                .andExpect(jsonPath("$.data.balance").value(100000))
                .andExpect(jsonPath("$.data.accountStatus").value("ACTIVE"));

            verify(accountService).createAccount(any(AccountCreateRequestDTO.class));
        }

        @Test
        void createAccount_invalidJson_return400() throws Exception {
            String invalidJson = "{ invalid json }";

            mockMvc.perform(post("/api/account/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());

            verify(accountService, never()).createAccount(any(AccountCreateRequestDTO.class));
        }

        @Test
        void createAccount_missingRequiredFields_Throws() throws Exception {
            AccountCreateRequestDTO requestDTO = AccountCreateRequestDTO.builder()
                .accountNumber(null)
                .accountName("mxxikr")
                .bankName("mxxikrBank")
                .build();

            when(accountService.createAccount(any(AccountCreateRequestDTO.class)))
                    .thenThrow(new TransferSystemException(ErrorCode.INVALID_REQUEST));

            mockMvc.perform(post("/api/account/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_REQUEST.getMessage()));

            verify(accountService).createAccount(any(AccountCreateRequestDTO.class));
        }

        @Test
        void createAccount_duplicateAccount_Throws() throws Exception {
            when(accountService.createAccount(any(AccountCreateRequestDTO.class)))
                .thenThrow(new TransferSystemException(ErrorCode.DUPLICATE_ACCOUNT_NUMBER));

            mockMvc.perform(post("/api/account/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequestDTO)))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(ErrorCode.DUPLICATE_ACCOUNT_NUMBER.getMessage()));

            verify(accountService).createAccount(any(AccountCreateRequestDTO.class));
        }
    }

    // ========================= 계좌 조회 테스트 =========================
    @Nested
    class GetAccountTest {

        @Test
        void getAccount_success() throws Exception {
            UUID accountId = UUID.randomUUID();
            when(accountService.getAccount(accountId)).thenReturn(mockResponseDTO);

            mockMvc.perform(get("/api/account/{accountId}", accountId)
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.result_code").value(ResultCode.SUCCESS_HAS_DATA.getCode()))
                .andExpect(jsonPath("$.data.accountNumber").value("account123"))
                .andExpect(jsonPath("$.data.accountName").value("mxxikr"));

            verify(accountService).getAccount(accountId);
        }

        @Test
        void getAccount_notFound_Throws() throws Exception {
            UUID accountId = UUID.randomUUID();
            when(accountService.getAccount(accountId))
                    .thenThrow(new TransferSystemException(ErrorCode.ACCOUNT_NOT_FOUND));

            mockMvc.perform(get("/api/account/{accountId}", accountId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(ErrorCode.ACCOUNT_NOT_FOUND.getMessage()));

            verify(accountService).getAccount(accountId);
        }

        @Test
        void getAccount_invalidUUID_return400() throws Exception {
            String invalidUUID = "invalid-uuid";

            mockMvc.perform(get("/api/account/{accountId}", invalidUUID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verify(accountService, never()).getAccount(any(UUID.class));
        }
    }

    // ========================= 계좌 삭제 테스트 =========================
    @Nested
    class DeleteAccountTest {

        @Test
        void deleteAccount_success() throws Exception {
            UUID accountId = UUID.randomUUID();
            doNothing().when(accountService).deleteAccount(accountId);

            mockMvc.perform(delete("/api/account/{accountId}", accountId)
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result_code").value(ResultCode.SUCCESS_NO_DATA.getCode()));

            verify(accountService).deleteAccount(accountId);
        }

        @Test
        void deleteAccount_notFound_Throws() throws Exception {
            UUID accountId = UUID.randomUUID();
            doThrow(new TransferSystemException(ErrorCode.ACCOUNT_NOT_FOUND))
                .when(accountService).deleteAccount(accountId);

            mockMvc.perform(delete("/api/account/{accountId}", accountId)
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(ErrorCode.ACCOUNT_NOT_FOUND.getMessage()));

            verify(accountService).deleteAccount(accountId);
        }
    }

    // ========================= 입금 테스트 =========================
    @Nested
    class DepositTest {

        @Test
        void deposit_success() throws Exception {
            String accountNumber = "account123";
            BigDecimal amount = new BigDecimal("50000");
            AccountBalanceRequestDTO requestDTO = AccountBalanceRequestDTO.builder()
                .accountNumber(accountNumber)
                .amount(amount)
                .build();

            doNothing().when(accountService).deposit(accountNumber, amount);

            mockMvc.perform(post("/api/account/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result_code").value(ResultCode.SUCCESS_NO_DATA.getCode()));

            verify(accountService).deposit(accountNumber, amount);
        }

        @Test
        void deposit_accountNotFound_Throws() throws Exception {
            String accountNumber = "nonexistent";
            BigDecimal amount = new BigDecimal("50000");
            AccountBalanceRequestDTO requestDTO = AccountBalanceRequestDTO.builder()
                .accountNumber(accountNumber)
                .amount(amount)
                .build();

            doThrow(new TransferSystemException(ErrorCode.ACCOUNT_NOT_FOUND))
                .when(accountService).deposit(accountNumber, amount);

            mockMvc.perform(post("/api/account/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(ErrorCode.ACCOUNT_NOT_FOUND.getMessage()));

            verify(accountService).deposit(accountNumber, amount);
        }
    }

    // ========================= 출금 테스트 =========================
    @Nested
    class WithdrawTest {
        @Test
        void withdraw_success() throws Exception {
            String accountNumber = "account123";
            BigDecimal amount = new BigDecimal("30000");
            AccountBalanceRequestDTO requestDTO = AccountBalanceRequestDTO.builder()
                .accountNumber(accountNumber)
                .amount(amount)
                .build();

            doNothing().when(accountService).withdraw(accountNumber, amount);

            mockMvc.perform(post("/api/account/withdraw")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result_code").value(ResultCode.SUCCESS_NO_DATA.getCode()));

            verify(accountService).withdraw(accountNumber, amount);
        }

        @Test
        void withdraw_insufficientBalance_Throws() throws Exception {
            String accountNumber = "account123";
            BigDecimal amount = new BigDecimal("1000000");
            AccountBalanceRequestDTO requestDTO = AccountBalanceRequestDTO.builder()
                .accountNumber(accountNumber)
                .amount(amount)
                .build();

            doThrow(new TransferSystemException(ErrorCode.INSUFFICIENT_BALANCE))
                .when(accountService).withdraw(accountNumber, amount);

            mockMvc.perform(post("/api/account/withdraw")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ErrorCode.INSUFFICIENT_BALANCE.getMessage()));

            verify(accountService).withdraw(accountNumber, amount);
        }
    }
}