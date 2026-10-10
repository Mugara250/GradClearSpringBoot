package auca.ac.rw.config;

import auca.ac.rw.security.JwtAuthenticationFilter;
import auca.ac.rw.security.JwtProperties;
import auca.ac.rw.security.JwtService;
import auca.ac.rw.security.OAuth2LoginSuccessHandler;
import auca.ac.rw.security.SecurityErrorWriter;
import auca.ac.rw.security.TokenBlocklist;
import auca.ac.rw.user.repository.UserRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * The Class SecurityConfig.
 * Stateless JWT security for the /api/v1 REST API.
 *
 * - Every request is authenticated by JwtAuthenticationFilter from an
 *   `Authorization: Bearer <token>` header. No HttpSession, no cookies, so
 *   CSRF protection is off (there is no ambient credential to forge).
 * - Coarse rules live here (what's public, DELETE is ADMIN-only, everything
 *   else needs a login). Role- and owner-specific rules live on the
 *   controller methods as @PreAuthorize (enabled by @EnableMethodSecurity),
 *   since "a student may only read their own transcript" can't be written
 *   as a URL pattern.
 * - Two independent ways to get a token, both ending in JwtService.issue():
 *     1. POST /api/v1/user/login with studentId/staffId + password;
 *     2. Google sign-in at /oauth2/authorization/google, finished by
 *        OAuth2LoginSuccessHandler (only when Google client credentials are
 *        configured, i.e. the "oauth2" profile is active).
 *   Everything downstream (JwtAuthenticationFilter, the RBAC rules) can't
 *   tell the two apart and doesn't need to.
 *
 * @version 2.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtService jwtService,
                                                   TokenBlocklist tokenBlocklist,
                                                   UserRepository userRepository,
                                                   SecurityErrorWriter securityErrorWriter,
                                                   OAuth2LoginSuccessHandler oauth2LoginSuccessHandler,
                                                   ObjectProvider<ClientRegistrationRepository> clientRegistrations)
            throws Exception {
        JwtAuthenticationFilter jwtFilter =
                new JwtAuthenticationFilter(jwtService, tokenBlocklist, userRepository, securityErrorWriter);

        http
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())   // POST /api/v1/user/logout is ours, not Spring's
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/user/login").permitAll()
                        .requestMatchers("/oauth2/authorization/**", "/login/oauth2/code/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(securityErrorWriter)
                        .accessDeniedHandler(securityErrorWriter))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        // Boot only creates a ClientRegistrationRepository when some
        // spring.security.oauth2.client.registration.* is configured, so the
        // app (and the tests) still run without Google credentials.
        // Note: the Google round-trip keeps its state (the `state` parameter)
        // in a short-lived HttpSession; STATELESS above only stops Spring
        // Security from keeping the *login* in a session, and the success
        // handler invalidates that session as soon as the JWT is issued.
        if (clientRegistrations.getIfAvailable() != null) {
            http.oauth2Login(oauth2 -> oauth2
                    .successHandler(oauth2LoginSuccessHandler)
                    .failureHandler((request, response, exception) -> securityErrorWriter.write(
                            response, HttpStatus.UNAUTHORIZED, "Unauthorized", "Google sign-in failed")));
        }

        return http.build();
    }
}
