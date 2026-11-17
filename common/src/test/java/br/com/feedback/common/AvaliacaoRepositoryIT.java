package br.com.feedback.common;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Teste de integração com PostgreSQL real (Docker). Executado por: mvn verify */
@Testcontainers
class AvaliacaoRepositoryIT {

    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:17-alpine");
    static HikariDataSource ds;
    static AvaliacaoRepository repo;

    @BeforeAll
    static void sobe() {
        PG.start();
        Flyway.configure()
                .dataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword())
                .locations("classpath:db/migration")
                .load().migrate();
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(PG.getJdbcUrl());
        cfg.setUsername(PG.getUsername());
        cfg.setPassword(PG.getPassword());
        ds = new HikariDataSource(cfg);
        repo = new AvaliacaoRepository(ds);
    }

    @AfterAll
    static void desce() {
        ds.close();
        PG.stop();
    }

    @Test
    void insereEAgrega() {
        OffsetDateTime inicio = OffsetDateTime.now().minusMinutes(1);
        repo.inserir("Curso excelente", 10);
        repo.inserir("Áudio ruim", 2);
        repo.inserir("Regular", 5);

        assertEquals(3, repo.listarDesde(inicio).size());
        assertEquals(17.0 / 3, repo.mediaDesde(inicio), 0.001);
        assertEquals(1L, repo.contagemPorUrgenciaDesde(inicio).get(Urgencia.CRITICA));
        assertEquals(1L, repo.contagemPorUrgenciaDesde(inicio).get(Urgencia.MEDIA));
        assertEquals(1L, repo.contagemPorUrgenciaDesde(inicio).get(Urgencia.BAIXA));
        assertTrue(repo.contagemPorDiaDesde(inicio).values().stream().mapToLong(Long::longValue).sum() >= 3);
    }
}
