package com.example.bankcards.dto.response;

import com.example.bankcards.entity.CardStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BankCardResponse {
    private Long id;
    private String maskedCardNumber;
    private String cardOwnerName;
    private LocalDate validatePeriod;
    private CardStatus status;
    private BigDecimal balance;
    private Boolean blockRequested;
    private String blockRequestReason;
}