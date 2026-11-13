package com.finanzapp.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

/**
 * El rol de la aplicación tiene privilegio mínimo (Ejercicio 7.1, threat model 5.2-E).
 * Si alguien logra una SQL injection, esto es lo que NO puede hacer.
 */
class DatabaseRolesTest extends PostgresTestBase {

    private static final String INSUFFICIENT_PRIVILEGE = "42501";

    @Test
    void laAppNoEsSuperusuarioNiPuedeSaltarseRls() throws SQLException {
        try (Connection c = asApp();
             ResultSet rs = c.createStatement().executeQuery(
                 "SELECT current_user, rolsuper, rolbypassrls, rolcreaterole, rolcreatedb "
                     + "FROM pg_roles WHERE rolname = current_user")) {
            rs.next();
            assertThat(rs.getString("current_user")).isEqualTo("finanzapp_app");
            assertThat(rs.getBoolean("rolsuper")).isFalse();
            assertThat(rs.getBoolean("rolbypassrls")).isFalse();
            assertThat(rs.getBoolean("rolcreaterole")).isFalse();
            assertThat(rs.getBoolean("rolcreatedb")).isFalse();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "DROP TABLE transaction",
        "TRUNCATE transaction",
        "ALTER TABLE transaction DISABLE ROW LEVEL SECURITY",
        "CREATE TABLE robo (x text)",
        "CREATE TABLE public.robo (x text)",
        "DELETE FROM audit_log",
        "UPDATE audit_log SET event_type = 'borrado'",
        "DELETE FROM category",
        "SELECT * FROM flyway_schema_history",
        "COPY (SELECT 1) TO PROGRAM 'id'",
    })
    void laAppNoTienePermisoPara(String sql) throws SQLException {
        try (Connection c = asApp()) {
            assertThatThrownBy(() -> c.createStatement().execute(sql))
                .isInstanceOf(SQLException.class)
                .extracting("SQLState").isEqualTo(INSUFFICIENT_PRIVILEGE);
        }
    }
}
