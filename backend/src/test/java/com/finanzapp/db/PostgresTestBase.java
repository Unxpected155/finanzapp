package com.finanzapp.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HexFormat;
import java.security.SecureRandom;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Levanta un Postgres real (la misma imagen y el mismo script de roles que compose.yaml)
 * y arranca la app completa: Flyway migra como finanzapp_owner y la app se conecta
 * como finanzapp_app. Un solo contenedor para todos los tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class PostgresTestBase {

    // Contraseñas aleatorias por ejecución: no hay secretos fijos en los tests.
    protected static final String OWNER_PASSWORD = randomSecret();
    protected static final String APP_PASSWORD = randomSecret();

    protected static final PostgreSQLContainer POSTGRES =
        new PostgreSQLContainer(DockerImageName.parse("postgres:18.7"))
            .withDatabaseName("finanzapp")
            .withCopyFileToContainer(
                MountableFile.forHostPath("../docker/postgres/initdb/01-roles.sh"),
                "/docker-entrypoint-initdb.d/01-roles.sh")
            .withCopyToContainer(Transferable.of(OWNER_PASSWORD), "/run/secrets/db_owner_password")
            .withCopyToContainer(Transferable.of(APP_PASSWORD), "/run/secrets/db_app_password");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("db_app_password", () -> APP_PASSWORD);
        registry.add("db_owner_password", () -> OWNER_PASSWORD);
    }

    /** Conexión como el rol de la aplicación (el que importa para la seguridad). */
    protected static Connection asApp() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), "finanzapp_app", APP_PASSWORD);
    }

    /** Conexión como superusuario: solo para preparar datos de prueba. */
    protected static Connection asSuperuser() throws SQLException {
        Connection c = DriverManager.getConnection(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        c.createStatement().execute("SET search_path = finanzapp");
        return c;
    }

    private static String randomSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
