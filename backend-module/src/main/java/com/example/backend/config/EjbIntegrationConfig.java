package com.example.backend.config;

import com.example.ejb.BeneficioEjbService;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EjbIntegrationConfig {

    @Bean
    public BeneficioEjbService beneficioEjbService(EntityManager entityManager) {
        // The challenge keeps the original class name, but the runtime model here is a plain
        // domain service managed by Spring instead of a Jakarta EE EJB container.
        return new BeneficioEjbService(entityManager);
    }
}
