package com.example.bankcards.dto.request;

import lombok.Data;
import javax.validation.constraints.*;
import java.math.BigDecimal;

@Data
public class TransferRequest {
    @NotNull(message = "From card ID is required")
    private Long fromCardId;

    @NotNull(message = "To card ID is required")
    private Long toCardId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    private String description;
}
