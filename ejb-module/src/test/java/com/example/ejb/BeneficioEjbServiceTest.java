package com.example.ejb;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BeneficioEjbServiceTest {

    private EntityManager em;
    private BeneficioEjbService service;

    @BeforeEach
    void setUp() {
        em = mock(EntityManager.class);
        service = new BeneficioEjbService(em);
    }

    @Test
    void transferShouldUpdateBalancesWhenInputIsValid() {
        Beneficio from = beneficioWithValue("1000.00");
        Beneficio to = beneficioWithValue("200.00");

        when(em.find(Beneficio.class, 1L, LockModeType.PESSIMISTIC_WRITE, Map.of("jakarta.persistence.lock.timeout", 250)))
                .thenReturn(from);
        when(em.find(Beneficio.class, 2L, LockModeType.PESSIMISTIC_WRITE, Map.of("jakarta.persistence.lock.timeout", 250)))
                .thenReturn(to);

        service.transfer(1L, 2L, new BigDecimal("150.00"));

        assertEquals(0, from.getValor().compareTo(new BigDecimal("850.00")));
        assertEquals(0, to.getValor().compareTo(new BigDecimal("350.00")));
    }

    @Test
    void transferShouldLockInDeterministicOrder() {
        Beneficio first = beneficioWithValue("500.00");
        Beneficio second = beneficioWithValue("500.00");

        when(em.find(Beneficio.class, 1L, LockModeType.PESSIMISTIC_WRITE, Map.of("jakarta.persistence.lock.timeout", 250)))
                .thenReturn(first);
        when(em.find(Beneficio.class, 2L, LockModeType.PESSIMISTIC_WRITE, Map.of("jakarta.persistence.lock.timeout", 250)))
                .thenReturn(second);

        service.transfer(2L, 1L, new BigDecimal("100.00"));

        InOrder inOrder = inOrder(em);
        inOrder.verify(em).find(Beneficio.class, 1L, LockModeType.PESSIMISTIC_WRITE, Map.of("jakarta.persistence.lock.timeout", 250));
        inOrder.verify(em).find(Beneficio.class, 2L, LockModeType.PESSIMISTIC_WRITE, Map.of("jakarta.persistence.lock.timeout", 250));
    }

    @Test
    void transferShouldFailWhenBalanceIsInsufficient() {
        Beneficio from = beneficioWithValue("50.00");
        Beneficio to = beneficioWithValue("200.00");

        when(em.find(Beneficio.class, 1L, LockModeType.PESSIMISTIC_WRITE, Map.of("jakarta.persistence.lock.timeout", 250)))
                .thenReturn(from);
        when(em.find(Beneficio.class, 2L, LockModeType.PESSIMISTIC_WRITE, Map.of("jakarta.persistence.lock.timeout", 250)))
                .thenReturn(to);

        Executable action = () -> service.transfer(1L, 2L, new BigDecimal("100.00"));

        assertThrows(IllegalStateException.class, action);
        assertEquals(0, from.getValor().compareTo(new BigDecimal("50.00")));
        assertEquals(0, to.getValor().compareTo(new BigDecimal("200.00")));
    }

    @Test
    void transferShouldFailWhenIdsAreEqual() {
        Executable action = () -> service.transfer(1L, 1L, new BigDecimal("10.00"));
        assertThrows(IllegalArgumentException.class, action);
    }

    @Test
    void transferShouldFailWhenBeneficioDoesNotExist() {
        when(em.find(Beneficio.class, 1L, LockModeType.PESSIMISTIC_WRITE, Map.of("jakarta.persistence.lock.timeout", 250)))
                .thenReturn(null);

        Executable action = () -> service.transfer(1L, 2L, new BigDecimal("10.00"));

        assertThrows(IllegalArgumentException.class, action);
    }

    private Beneficio beneficioWithValue(String value) {
        Beneficio beneficio = new Beneficio();
        beneficio.setValor(new BigDecimal(value));
        return beneficio;
    }
}
