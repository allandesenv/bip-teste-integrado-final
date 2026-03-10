package com.example.backend.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record TransferRequest(
        @NotNull Long fromId,
        @NotNull Long toId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount
) {
    @AssertTrue(message = "fromId e toId devem ser diferentes.")
    public boolean isValidSourceAndTarget() {
        return fromId == null || toId == null || !fromId.equals(toId);
    }
}
