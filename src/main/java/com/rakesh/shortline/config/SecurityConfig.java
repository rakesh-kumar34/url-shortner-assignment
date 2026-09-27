package com.rakesh.shortline.config;

import com.rakesh.shortline.api.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class SecurityConfig {
    @Bean
    InMemoryUserDetailsManager noPasswordLogin() { return new InMemoryUserDetailsManager(); }

    @Bean
    SecurityFilterChain security(HttpSecurity http, AppProperties config, ObjectMapper json, Environment env) throws Exception {
        if (env.matchesProfiles("prod") && (!config.publicOrigin().startsWith("https://")
                || config.apiToken().equals("local-demo-token-change-before-deployment"))) {
            throw new IllegalArgumentException("Production requires HTTPS PUBLIC_ORIGIN and a non-demo API_TOKEN");
        }
        var tokenFilter = new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                    throws ServletException, IOException {
                String auth = request.getHeader("Authorization");
                byte[] expected = ("Bearer " + config.apiToken()).getBytes(StandardCharsets.UTF_8);
                if (auth != null && MessageDigest.isEqual(expected, auth.getBytes(StandardCharsets.UTF_8))) {
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken("operator", null, List.of()));
                }
                chain.doFilter(request, response);
            }
        };
        return http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(c -> c.ignoringRequestMatchers("/api/**"))
                .requestCache(c -> c.disable())
                .authorizeHttpRequests(a -> a.requestMatchers("/api/**").authenticated().anyRequest().permitAll())
                .exceptionHandling(e -> e.authenticationEntryPoint((request, response, error) -> {
                    response.setStatus(401); response.setContentType("application/json");
                    response.setHeader("WWW-Authenticate", "Bearer");
                    json.writeValue(response.getOutputStream(), new ApiError("unauthorized", "A valid bearer token is required",
                            (String) request.getAttribute("requestId")));
                }))
                .headers(h -> h.contentSecurityPolicy(c -> c.policyDirectives(
                        "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self'; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'"))
                        .frameOptions(f -> f.deny())
                        .addHeaderWriter((request, response) -> response.setHeader("Referrer-Policy", "no-referrer")))
                .addFilterBefore(tokenFilter, UsernamePasswordAuthenticationFilter.class).build();
    }
}
