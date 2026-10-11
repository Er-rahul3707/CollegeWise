package com.collegewise.config;

import com.collegewise.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,JwtAuthenticationFilter jwt)throws Exception{
        return http.csrf(csrf->csrf.disable())
                .sessionManagement(session->session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errors->errors
                        .authenticationEntryPoint((request,response,exception)->{response.setStatus(401);response.setContentType("application/json");response.getWriter().write("{\"error\":\"Unauthorized\"}");})
                .accessDeniedHandler((request,response,exception)->{response.setStatus(403);response.setContentType("application/json");response.getWriter().write("{\"error\":\"Forbidden\"}");}))
                .authorizeHttpRequests(auth->auth
                        .requestMatchers("/admin.html").hasRole("ADMIN")
                        .requestMatchers("/dashboard.html").hasRole("STUDENT")
                        .requestMatchers("/api/auth/register","/api/auth/login","/api/health","/","/*.html","/*.css","/*.js").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/colleges/**").permitAll()
                        .requestMatchers("/api/admin/**","/api/colleges/**").hasRole("ADMIN")
                        .requestMatchers("/api/students/**").hasRole("STUDENT")
                        .requestMatchers("/api/auth/logout").authenticated()
                        .anyRequest().authenticated())
                .addFilterBefore(jwt,UsernamePasswordAuthenticationFilter.class).build();
    }
    @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
    @Bean AuthenticationManager authenticationManager(AuthenticationConfiguration config)throws Exception{return config.getAuthenticationManager();}
}
