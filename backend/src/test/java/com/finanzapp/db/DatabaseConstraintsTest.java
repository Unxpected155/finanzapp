package com.finanzapp.db;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * La BD rechaza datos inválidos aunque la app tenga un bug (Ejercicio 4.1).
 * Todo se ejecuta como finanzapp_app.
 */
class DatabaseConstraintsTest extends PostgresTestBase {

    private static final String CHECK_VIOLATION = "23514";
    private static final String UNIQUE_VIOLATION = "23505";
    private static final String FK_VIOLATION = "23503";

    @Test
    void montoNegativoSeRechaza() throws SQLException {
        try (Connection c = asApp()) {
            UUID user = newUser(c);
            UUID category = newCategory(c, user, "Comida");
            assertThatThrownBy(() -> newTransaction(c, user, category, "-1.00"))
                .isInstanceOf(SQLException.class)
                .extracting("SQLState").isEqualTo(CHECK_VIOLATION);
        }
    }

    @Test
    void grupoDeCategoriaInvalidoSeRechaza() throws SQLException {
        try (Connection c = asApp()) {
            UUID user = newUser(c);
            assertThatThrownBy(() -> c.createStatement().execute(
                    "INSERT INTO category (user_id, name, category_group) VALUES ('" + user + "', 'X', 'LUJO')"))
                .isInstanceOf(SQLException.class)
                .extracting("SQLState").isEqualTo(CHECK_VIOLATION);
        }
    }

    @Test
    void presupuestoDuplicadoParaElMismoMesSeRechaza() throws SQLException {
        try (Connection c = asApp()) {
            UUID user = newUser(c);
            String insert = "INSERT INTO budget (user_id, year, month) VALUES ('" + user + "', 2026, 11)";
            c.createStatement().execute(insert);
            assertThatThrownBy(() -> c.createStatement().execute(insert))
                .isInstanceOf(SQLException.class)
                .extracting("SQLState").isEqualTo(UNIQUE_VIOLATION);
        }
    }

    @Test
    void movimientoConCategoriaDeOtroUsuarioSeRechaza() throws SQLException {
        // Threat model 5.1-T: referencia cruzada. La FK compuesta (category_id, user_id) lo impide.
        try (Connection c = asApp()) {
            UUID userA = newUser(c);
            UUID userB = newUser(c);
            UUID categoryOfB = newCategory(c, userB, "Ropa");
            assertThatThrownBy(() -> newTransaction(c, userA, categoryOfB, "10.00"))
                .isInstanceOf(SQLException.class)
                .extracting("SQLState").isEqualTo(FK_VIOLATION);
        }
    }

    // --- helpers (los valores son fijos o UUIDs generados aquí: no hay input externo) ---

    static UUID newUser(Connection c) throws SQLException {
        UUID id = UUID.randomUUID();
        c.createStatement().execute(
            "INSERT INTO app_user (id, display_name) VALUES ('" + id + "', 'Test')");
        return id;
    }

    static UUID newCategory(Connection c, UUID user, String name) throws SQLException {
        UUID id = UUID.randomUUID();
        try (var ps = c.prepareStatement(
                "INSERT INTO category (id, user_id, name, category_group) VALUES (?, ?, ?, 'NECESIDAD')")) {
            ps.setObject(1, id);
            ps.setObject(2, user);
            ps.setString(3, name);
            ps.executeUpdate();
        }
        return id;
    }

    static void newTransaction(Connection c, UUID user, UUID category, String amount) throws SQLException {
        try (var ps = c.prepareStatement(
                "INSERT INTO transaction (user_id, category_id, tx_date, description, amount) "
                    + "VALUES (?, ?, CURRENT_DATE, 'Prueba', ?::numeric)")) {
            ps.setObject(1, user);
            ps.setObject(2, category);
            ps.setString(3, amount);
            ps.executeUpdate();
        }
    }
}
