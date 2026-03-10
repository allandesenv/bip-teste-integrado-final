package com.example.ejb;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.PessimisticLockException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

public class BeneficioEjbService {

    private static final String LOCK_TIMEOUT_HINT = "jakarta.persistence.lock.timeout";
    private static final int LOCK_TIMEOUT_MS = 250;

    private final EntityManager em;

    public BeneficioEjbService(EntityManager em) {
        this.em = em;
    }

    public void transfer(Long fromId, Long toId, BigDecimal amount) {
        validateInput(fromId, toId, amount);

        // Lock em ordem deterministica para reduzir risco de deadlock.
        Long firstId = fromId < toId ? fromId : toId;
        Long secondId = fromId < toId ? toId : fromId;

        Beneficio firstLocked = findForUpdate(firstId);
        Beneficio secondLocked = findForUpdate(secondId);

        Beneficio from = Objects.equals(firstId, fromId) ? firstLocked : secondLocked;
        Beneficio to = Objects.equals(firstId, toId) ? firstLocked : secondLocked;

        if (from.getValor().compareTo(amount) < 0) {
            throw new IllegalStateException("Saldo insuficiente para transferencia.");
        }

        from.setValor(from.getValor().subtract(amount));
        to.setValor(to.getValor().add(amount));
    }

    private Beneficio findForUpdate(Long id) {
        Beneficio beneficio;
        try {
            beneficio = em.find(
                    Beneficio.class,
                    id,
                    LockModeType.PESSIMISTIC_WRITE,
                    Map.of(LOCK_TIMEOUT_HINT, LOCK_TIMEOUT_MS)
            );
        } catch (LockTimeoutException | PessimisticLockException ex) {
            throw ex;
        } catch (PersistenceException ex) {
            throw ex;
        }
        if (beneficio == null) {
            throw new IllegalArgumentException("Beneficio nao encontrado para id: " + id);
        }
        return beneficio;
    }

    private void validateInput(Long fromId, Long toId, BigDecimal amount) {
        if (fromId == null || toId == null) {
            throw new IllegalArgumentException("fromId e toId sao obrigatorios.");
        }
        if (Objects.equals(fromId, toId)) {
            throw new IllegalArgumentException("Transferencia exige ids diferentes.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount deve ser maior que zero.");
        }
    }
}
