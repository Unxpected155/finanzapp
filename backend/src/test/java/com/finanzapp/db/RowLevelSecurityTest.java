package com.finanzapp.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * RLS: segunda barrera contra IDOR (Ejercicio 7.2, threat model 5.1-E).
 *
 * 🔒 TU PARTE: escribe backend/src/main/resources/db/migration/V3__rls.sql
 * para que estos tests pasen, y luego quita el @Disabled.
 *
 * Contrato que usa la app (y estos tests): al inicio de cada transacción se hace
 *   SELECT set_config('app.current_user_id', '<uuid>', true);
 * El tercer argumento "true" equivale a SET LOCAL: dura solo hasta el fin de la transacción.
 */
@Disabled("Fase 2 — pendiente: escribir V3__rls.sql (lo escribe el usuario)")
class RowLevelSecurityTest extends PostgresTestBase {

    UUID userA;
    UUID userB;

    @BeforeEach
    void seedTwoUsers() throws SQLException {
        // El superusuario se salta RLS: sirve para preparar datos de ambos usuarios.
        try (Connection su = asSuperuser()) {
            userA = DatabaseConstraintsTest.newUser(su);
            userB = DatabaseConstraintsTest.newUser(su);
            UUID catA = DatabaseConstraintsTest.newCategory(su, userA, "Comida A");
            UUID catB = DatabaseConstraintsTest.newCategory(su, userB, "Comida B");
            DatabaseConstraintsTest.newTransaction(su, userA, catA, "10.00");
            DatabaseConstraintsTest.newTransaction(su, userB, catB, "20.00");
            newBudgetWithLine(su, userA, catA);
            newBudgetWithLine(su, userB, catB);
        }
    }

    private static void newBudgetWithLine(Connection su, UUID user, UUID category) throws SQLException {
        UUID budget = UUID.randomUUID();
        try (var ps = su.prepareStatement(
                "INSERT INTO budget (id, user_id, year, month) VALUES (?, ?, 2026, 11)")) {
            ps.setObject(1, budget);
            ps.setObject(2, user);
            ps.executeUpdate();
        }
        try (var ps = su.prepareStatement(
                "INSERT INTO budget_line (user_id, budget_id, category_id, planned_amount) VALUES (?, ?, ?, 100.00)")) {
            ps.setObject(1, user);
            ps.setObject(2, budget);
            ps.setObject(3, category);
            ps.executeUpdate();
        }
    }

    @Test
    void unSelectSinWhereSoloDevuelveFilasDelUsuarioActual() throws SQLException {
        try (Connection c = asApp()) {
            c.setAutoCommit(false);
            actAs(c, userA);
            // A propósito SIN "WHERE user_id = ?": es el bug que RLS debe contener.
            for (String table : new String[] {"transaction", "category", "budget", "budget_line"}) {
                assertThat(countRowsOf(c, table, userB))
                    .as("filas de B visibles para A en %s", table).isZero();
            }
            assertThat(countRowsOf(c, "transaction", userA)).isPositive();
            c.rollback();
        }
    }

    @Test
    void sinUsuarioEnLaSesionNoSeVeNada() throws SQLException {
        // Si la app olvida el set_config, RLS debe fallar CERRADO (0 filas), no abierto.
        try (Connection c = asApp();
             ResultSet rs = c.createStatement().executeQuery("SELECT count(*) FROM transaction")) {
            rs.next();
            assertThat(rs.getLong(1)).isZero();
        }
    }

    @Test
    void noSePuedeInsertarUnaFilaAnombreDeOtroUsuario() throws SQLException {
        try (Connection c = asApp()) {
            c.setAutoCommit(false);
            actAs(c, userA);
            assertThatThrownBy(() -> c.createStatement().execute(
                    "INSERT INTO budget (user_id, year, month) VALUES ('" + userB + "', 2030, 1)"))
                .isInstanceOf(SQLException.class)
                .extracting("SQLState").isEqualTo("42501");
            c.rollback();
        }
    }

    private static void actAs(Connection c, UUID user) throws SQLException {
        try (var ps = c.prepareStatement("SELECT set_config('app.current_user_id', ?, true)")) {
            ps.setString(1, user.toString());
            ps.execute();
        }
    }

    /** Cuenta las filas de "owner" que la consulta SIN filtro devuelve. */
    private static long countRowsOf(Connection c, String table, UUID owner) throws SQLException {
        long count = 0;
        try (ResultSet rs = c.createStatement().executeQuery("SELECT user_id FROM " + table)) {
            while (rs.next()) {
                if (owner.equals(rs.getObject(1, UUID.class))) {
                    count++;
                }
            }
        }
        return count;
    }
}
