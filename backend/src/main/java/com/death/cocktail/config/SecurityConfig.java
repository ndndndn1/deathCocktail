package com.death.cocktail.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * ⚠️ VULNERABLE BY DESIGN - DO NOT USE IN PRODUCTION ⚠️
 *
 * Security Configuration with INTENTIONAL vulnerabilities:
 * - CSRF Disabled
 * - All endpoints public
 * - CORS allows everything
 * - Frame options disabled
 * - No authentication required
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // ============================================================
        // VULNERABILITY: ALL SECURITY DISABLED!
        // ============================================================

        http
            // VULNERABILITY: CSRF protection disabled (CWE-352)
            .csrf().disable()

            // VULNERABILITY: No authentication on any endpoint
            .authorizeRequests()
                .antMatchers("/**").permitAll()
                .anyRequest().permitAll()
            .and()

            // VULNERABILITY: Clickjacking protection disabled
            .headers()
                .frameOptions().disable()
                .xssProtection().disable()  // XSS protection disabled
                .contentTypeOptions().disable()  // MIME sniffing allowed
            .and()

            // VULNERABILITY: Permissive CORS
            .cors();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // ============================================================
        // VULNERABILITY: Wildcard CORS (CWE-942)
        // ============================================================
        // Allows any origin to make requests
        // Allows credentials to be sent
        // This enables CSRF attacks from any website
        // ============================================================

        configuration.setAllowedOrigins(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("*"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);  // DANGEROUS with wildcard origin!
        configuration.setExposedHeaders(Arrays.asList("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
