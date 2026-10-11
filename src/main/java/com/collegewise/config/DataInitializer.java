package com.collegewise.config;

import com.collegewise.entity.Role;
import com.collegewise.entity.User;
import com.collegewise.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner provisionAdmin(UserRepository users, PasswordEncoder encoder,
                                     @Value("${ADMIN_EMAIL:}") String adminEmail,
                                     @Value("${ADMIN_PASSWORD:}") String adminPassword) {
        return args -> {
            if (adminEmail.isBlank() && adminPassword.isBlank()) return;
            if (adminEmail.isBlank() || adminPassword.isBlank()) {
                throw new IllegalArgumentException("ADMIN_EMAIL and ADMIN_PASSWORD must both be configured");
            }
            if (adminPassword.length() < 12 || adminPassword.length() > 72) {
                throw new IllegalArgumentException("ADMIN_PASSWORD must be between 12 and 72 characters");
            }
            var existing = users.findByEmailIgnoreCase(adminEmail.trim());
            if (existing.isPresent()) {
                if (existing.get().getRole() != Role.ADMIN) {
                    throw new IllegalStateException("Configured administrator email belongs to a non-admin account");
                }
            } else {
                users.save(new User("CollegeWise Admin",adminEmail.trim().toLowerCase(),encoder.encode(adminPassword),Role.ADMIN));
            }
        };
    }

    @Bean
    @Profile("demo")
    @ConditionalOnProperty(name="app.demo.student.enabled",havingValue="true")
    CommandLineRunner provisionDemoStudent(UserRepository users, com.collegewise.repository.ProfileRepository profiles,
                                           PasswordEncoder encoder,
                                           @Value("${DEMO_STUDENT_EMAIL:}") String studentEmail,
                                           @Value("${DEMO_STUDENT_PASSWORD:}") String studentPassword) {
        return args -> {
            if (studentEmail.isBlank() || studentPassword.isBlank()) {
                throw new IllegalArgumentException("DEMO_STUDENT_EMAIL and DEMO_STUDENT_PASSWORD are required when demo student provisioning is enabled");
            }
            if (studentPassword.length() < 12 || studentPassword.length() > 72) {
                throw new IllegalArgumentException("DEMO_STUDENT_PASSWORD must be between 12 and 72 characters");
            }
            var existing = users.findByEmailIgnoreCase(studentEmail.trim());
            if (existing.isPresent()) {
                if (existing.get().getRole() != Role.STUDENT) {
                    throw new IllegalStateException("Configured demo student email belongs to a non-student account");
                }
                return;
            }
            User student = users.save(new User("CollegeWise Demo Student", studentEmail.trim().toLowerCase(),
                    encoder.encode(studentPassword), Role.STUDENT));
            profiles.save(new com.collegewise.entity.StudentProfile(student));
        };
    }
}
