package com.example.bankcards.dto.request;

import lombok.Data;

import javax.validation.constraints.*;
import java.time.LocalDate;

@Data
public class CreateCardRequest {
    @NotBlank(message = "Card number is required")
    @Pattern(regexp = "^[0-9]{16}$", message = "Card number must be 16 digits")
    private String cardNumber;

    @NotBlank(message = "Card holder name is required")
    private String cardHolderName;

    @Future(message = "Validity period must be in the future")
    private LocalDate validityPeriod;

    @NotNull(message = "Owner ID is required")
    private Long ownerId;
}