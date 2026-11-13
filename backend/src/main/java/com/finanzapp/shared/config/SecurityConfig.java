package com.finanzapp.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de seguridad de la Fase 2: <b>cerrado por defecto</b>.
 * Todavía no hay login (llega en la Fase 3 con passkeys), así que todo se niega
 * salvo el health check.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                // Lista blanca: cualquier ruta nueva queda negada hasta que se permita a propósito.
                .anyRequest().denyAll())
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable());
        return http.build();
    }

    /**
     * Sin este bean, Spring Boot crea un usuario "user" con una contraseña aleatoria
     * y la imprime en los logs. Un gestor vacío evita esa credencial de más.
     */
    @Bean
    UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager();
    }
}
