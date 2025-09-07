package com.example.bankcards.dto.response;

import com.example.bankcards.entity.TransactionStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionResponse {
    private Long id;
    private String fromCardMasked;    // Маскированный номер карты отправителя
    private String toCardMasked;      // Маскированный номер карты получателя
    private String fromCardHolder;    // Владелец карты отправителя
    private String toCardHolder;      // Владелец карты получателя
    private BigDecimal amount;
    private LocalDateTime timestamp;
    private String description;
    private TransactionStatus status;


}