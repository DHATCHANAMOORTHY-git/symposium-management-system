package edu.symposium.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService users(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername("admin").password(encoder.encode("admin123")).roles("ADMIN").build(),
                User.withUsername("organizer").password(encoder.encode("organizer123")).roles("ORGANIZER").build(),
                User.withUsername("reviewer").password(encoder.encode("reviewer123")).roles("REVIEWER").build(),
                User.withUsername("participant").password(encoder.encode("participant123")).roles("PARTICIPANT").build()
        );
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index.html", "/login.html", "/dashboard.html", "/styles.css", "/app.js", "/favicon.ico", "/assets/**", "/h2-console/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/events", "/api/events/**", "/api/speakers", "/api/speakers/**", "/api/workshops", "/api/workshops/**", "/api/announcements", "/api/announcements/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/**", "/api/registrations", "/api/submissions").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/organizers/**").hasAnyRole("ADMIN", "ORGANIZER")
                        .requestMatchers("/api/dashboard/**", "/api/conferences/**", "/api/papers/**", "/api/registrations/**").authenticated()
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .formLogin(form -> form.disable());
        return http.build();
    }
}

