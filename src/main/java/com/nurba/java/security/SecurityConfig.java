package com.nurba.java.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.List;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties({JwtProperties.class, SecurityRateLimitProperties.class})
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    @Value("${app.swagger.enabled:true}")
    private boolean swaggerEnabled;

    @Value("${app.cors.allowed-origins}")
    private String allowedOriginsCsv;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOriginsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setAllowCredentials(true);
        config.setMaxAge(1800L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(
            AppUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthenticationProvider authenticationProvider
    ) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .headers(h -> h
                        .contentTypeOptions(Customizer.withDefaults())
                        .frameOptions(fo -> fo.deny())
                        .referrerPolicy(rp -> rp.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)
                                .preload(false)))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> {
                    auth
                            // Actuator health — Docker healthcheck uses this
                            .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()

                            .requestMatchers(
                                    "/api/v1/auth/register",
                                    "/api/v1/auth/login",
                                    "/api/v1/auth/refresh",
                                    "/api/v1/auth/refresh-cookie",
                                    "/api/v1/auth/logout"
                            ).permitAll()

                            .requestMatchers("/api/v1/auth/admin/**").hasRole("ADMIN")

                            .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                            // Public storefront catalog — read-only, no auth required
                            .requestMatchers(HttpMethod.GET, "/api/v1/catalog/**").permitAll()

                            .requestMatchers(HttpMethod.GET, "/api/v1/exchange-rates").permitAll()

                            .requestMatchers(HttpMethod.GET, "/api/v1/product/**").permitAll()

                            .requestMatchers(HttpMethod.POST, "/api/v1/order").permitAll()

                            .requestMatchers(HttpMethod.POST, "/api/v1/custom-design").permitAll()

                            .requestMatchers(HttpMethod.GET, "/api/v1/delivery/methods").permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/v1/delivery/cdek/**").permitAll()
                            .requestMatchers(HttpMethod.POST, "/api/v1/delivery/cdek/calculate").permitAll()
                            .requestMatchers(HttpMethod.POST, "/api/v1/delivery/cdek/calculate-order").permitAll()
                            .requestMatchers(HttpMethod.POST, "/api/v1/delivery/cdek/webhook").permitAll()
                            .requestMatchers(HttpMethod.POST, "/api/v1/payments/init").permitAll()
                            .requestMatchers(HttpMethod.POST, "/api/v1/payments/callback/**").permitAll()
                            .requestMatchers(HttpMethod.POST, "/api/v1/payments/paypal/create-order").permitAll()
                            .requestMatchers(HttpMethod.POST, "/api/v1/payments/paypal/capture/**").permitAll()
                            .requestMatchers(HttpMethod.POST, "/api/v1/payments/paypal/cancel/**").permitAll()
                            .requestMatchers(HttpMethod.POST, "/api/v1/payments/paypal/webhook").permitAll()

                            .requestMatchers(HttpMethod.GET, "/api/v1/order", "/api/v1/order/**")
                            .hasRole("ADMIN")

                            .requestMatchers(HttpMethod.PATCH, "/api/v1/order/**")
                            .hasRole("ADMIN")

                            .requestMatchers("/api/v1/customer/**").hasRole("ADMIN")

                            .requestMatchers(HttpMethod.POST, "/api/v1/product").hasRole("ADMIN")
                            .requestMatchers(HttpMethod.DELETE, "/api/v1/product/**").hasRole("ADMIN")

                            .requestMatchers(HttpMethod.POST, "/api/v1/media/upload").hasRole("ADMIN")

                            .requestMatchers(HttpMethod.GET, "/api/v1/custom-design", "/api/v1/custom-design/**")
                            .hasRole("ADMIN")

                            .requestMatchers("/api/v1/cdek-shipment/**").hasRole("ADMIN")
                            .requestMatchers("/api/v1/delivery-address/**").hasRole("ADMIN")
                            .requestMatchers("/api/v1/order-item/**").hasRole("ADMIN")

                            // Authenticated-user self-service endpoints
                            .requestMatchers("/api/v1/me/**").authenticated();

                    if (swaggerEnabled) {
                        auth.requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/v3/api-docs"
                        ).permitAll();
                    } else {
                        auth.requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/v3/api-docs"
                        ).denyAll();
                    }

                    auth.anyRequest().authenticated();
                })
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
