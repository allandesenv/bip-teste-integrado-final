package com.example.backend.integration;

import com.example.backend.dto.TransferRequest;
import com.example.backend.repository.BeneficioRepository;
import com.example.backend.service.BeneficioService;
import com.example.ejb.Beneficio;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class BeneficioTransferIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("beneficios_db")
            .withUsername("app_user")
            .withPassword("app_pass");

    @Autowired
    private BeneficioService beneficioService;

    @Autowired
    private BeneficioRepository beneficioRepository;

    @Autowired
    private DataSource dataSource;

    private ExecutorService executorService;

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(8);
        rebuildDatabase();
    }

    @AfterEach
    void tearDown() {
        executorService.shutdownNow();
    }

    @Test
    void shouldPreventNegativeBalanceUnderConcurrentTransfers() throws Exception {
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            futures.add(executorService.submit(() -> executeTransfer(startLatch, 1L, 2L, "150.00")));
        }

        startLatch.countDown();

        int successCount = 0;
        int businessFailureCount = 0;
        for (Future<Boolean> future : futures) {
            if (future.get(10, TimeUnit.SECONDS)) {
                successCount++;
            } else {
                businessFailureCount++;
            }
        }

        Beneficio origem = beneficioRepository.findById(1L).orElseThrow();
        Beneficio destino = beneficioRepository.findById(2L).orElseThrow();

        assertEquals(6, successCount);
        assertEquals(4, businessFailureCount);
        assertEquals(0, origem.getValor().compareTo(new BigDecimal("100.00")));
        assertEquals(0, destino.getValor().compareTo(new BigDecimal("1400.00")));
        assertFalse(origem.getValor().compareTo(BigDecimal.ZERO) < 0);
        assertEquals(0, origem.getValor().add(destino.getValor()).compareTo(new BigDecimal("1500.00")));
    }

    @Test
    void shouldAvoidDeadlockForOppositeTransfers() throws Exception {
        CountDownLatch startLatch = new CountDownLatch(1);

        Future<Boolean> transferOne = executorService.submit(() -> executeTransfer(startLatch, 1L, 2L, "100.00"));
        Future<Boolean> transferTwo = executorService.submit(() -> executeTransfer(startLatch, 2L, 1L, "50.00"));

        startLatch.countDown();

        assertTrue(transferOne.get(10, TimeUnit.SECONDS));
        assertTrue(transferTwo.get(10, TimeUnit.SECONDS));

        Beneficio beneficioUm = beneficioRepository.findById(1L).orElseThrow();
        Beneficio beneficioDois = beneficioRepository.findById(2L).orElseThrow();

        assertEquals(0, beneficioUm.getValor().compareTo(new BigDecimal("950.00")));
        assertEquals(0, beneficioDois.getValor().compareTo(new BigDecimal("550.00")));
    }

    @Test
    void shouldRetryTransferAfterTransientLockTimeout() throws Exception {
        CountDownLatch lockAcquired = new CountDownLatch(1);
        CountDownLatch releaseLock = new CountDownLatch(1);

        Future<?> locker = executorService.submit(() -> {
            try {
                holdRowLock(1L, lockAcquired, releaseLock);
            } catch (Exception ex) {
                throw new IllegalStateException("Falha ao manter lock de teste.", ex);
            }
        });
        assertTrue(lockAcquired.await(5, TimeUnit.SECONDS));

        Future<?> transfer = executorService.submit(() ->
                beneficioService.transfer(new TransferRequest(1L, 2L, new BigDecimal("100.00"))));

        Thread.sleep(Duration.ofMillis(450).toMillis());
        releaseLock.countDown();

        transfer.get(10, TimeUnit.SECONDS);
        locker.get(10, TimeUnit.SECONDS);

        Beneficio origem = beneficioRepository.findById(1L).orElseThrow();
        Beneficio destino = beneficioRepository.findById(2L).orElseThrow();

        assertEquals(0, origem.getValor().compareTo(new BigDecimal("900.00")));
        assertEquals(0, destino.getValor().compareTo(new BigDecimal("600.00")));
    }

    @Test
    void shouldEnforceNonNegativeValueConstraintAtDatabaseLevel() {
        Beneficio beneficio = new Beneficio();
        beneficio.setNome("Invalido");
        beneficio.setDescricao("Nao deve persistir");
        beneficio.setValor(new BigDecimal("-1.00"));
        beneficio.setAtivo(true);

        assertThrows(DataIntegrityViolationException.class, () -> beneficioRepository.saveAndFlush(beneficio));
    }

    private boolean executeTransfer(CountDownLatch startLatch, Long fromId, Long toId, String amount) throws Exception {
        startLatch.await(5, TimeUnit.SECONDS);
        try {
            beneficioService.transfer(new TransferRequest(fromId, toId, new BigDecimal(amount)));
            return true;
        } catch (IllegalStateException ex) {
            return false;
        }
    }

    private void holdRowLock(Long id, CountDownLatch lockAcquired, CountDownLatch releaseLock) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT ID FROM BENEFICIO WHERE ID = ? FOR UPDATE")) {
                statement.setLong(1, id);
                statement.executeQuery();
                lockAcquired.countDown();
                releaseLock.await(5, TimeUnit.SECONDS);
                connection.commit();
            } catch (Exception ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    private void rebuildDatabase() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        Path root = Path.of("..", "db");
        populator.addScript(new FileSystemResource(root.resolve("schema.sql")));
        populator.addScript(new FileSystemResource(root.resolve("seed.sql")));
        executeSql("DROP TABLE IF EXISTS BENEFICIO");
        DatabasePopulatorUtils.execute(populator, dataSource);
    }

    private void executeSql(String sql) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.execute();
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao executar SQL de teste.", ex);
        }
    }
}
