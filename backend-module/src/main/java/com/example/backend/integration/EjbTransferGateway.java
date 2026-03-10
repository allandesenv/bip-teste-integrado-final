package com.example.backend.integration;

import com.example.ejb.BeneficioEjbService;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

@Component
public class EjbTransferGateway {

    private final BeneficioEjbService beneficioEjbService;

    public EjbTransferGateway(BeneficioEjbService beneficioEjbService) {
        this.beneficioEjbService = beneficioEjbService;
    }

    @Retryable(
            retryFor = {
                    LockTimeoutException.class,
                    PessimisticLockException.class
            },
            maxAttempts = 3,
            backoff = @Backoff(delay = 150, multiplier = 2.0)
    )
    public void transfer(Long fromId, Long toId, BigDecimal amount) {
        beneficioEjbService.transfer(fromId, toId, amount);
    }
}
