package com.bank.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Isključujemo CSRF (Cross-Site Request Forgery)
                // Ovo je OBAVEZNO isključiti ako šalješ POST zahteve preko API-ja ili JavaScript-a
                .csrf(csrf -> csrf.disable())

                // 2. Definišemo pravila pristupa
                .authorizeHttpRequests(auth -> auth
                        // --> Dozvoli pristup HTML stranici za plaćanje i statičkim resursima
                        .requestMatchers("/pay.html", "/css/**", "/js/**", "/images/**").permitAll()

                        // --> Dozvoli pristup API rutama (da PSP može da pozove banku, i da forma može da pošalje podatke)
                        .requestMatchers("/api/bank/**").permitAll()

                        // --> Sve ostale rute (ako ih imaš) zahtevaju logovanje
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}
