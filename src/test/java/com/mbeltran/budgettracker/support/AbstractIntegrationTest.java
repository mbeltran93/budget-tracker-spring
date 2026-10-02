package com.mbeltran.budgettracker.support;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base para los tests de integracion (MockMvc + contexto Spring completo).
 *
 * Corren contra un Postgres real (no H2), igual que produccion, para que las
 * migraciones Flyway y los tipos de columna se comporten identico. Ese Postgres
 * NO lo levanta Testcontainers (el Docker Desktop de esta maquina bloquea el
 * acceso directo a la API del engine via npipe para clientes que no son el propio
 * `docker` CLI) sino el script scripts/run-integration-tests.(sh|ps1), que hace
 * `docker run` de un Postgres descartable antes de correr `mvn test` y lo apaga
 * al terminar. En CI (GitHub Actions, Linux) el mismo rol lo cumple el servicio
 * "postgres" declarado en el workflow.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        String host = System.getenv().getOrDefault("TEST_DB_HOST", "localhost");
        String port = System.getenv().getOrDefault("TEST_DB_PORT", "55432");
        String db = System.getenv().getOrDefault("TEST_DB_NAME", "budget_tracker_test");
        String user = System.getenv().getOrDefault("TEST_DB_USER", "test_user");
        String password = System.getenv().getOrDefault("TEST_DB_PASSWORD", "test_pass");

        registry.add("spring.datasource.url", () -> "jdbc:postgresql://" + host + ":" + port + "/" + db);
        registry.add("spring.datasource.username", () -> user);
        registry.add("spring.datasource.password", () -> password);
    }
}
