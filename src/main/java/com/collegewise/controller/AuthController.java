package com.collegewise.controller;

import com.collegewise.dto.ApiDtos.Login;
import com.collegewise.dto.ApiDtos.Register;
import com.collegewise.dto.ApiDtos.Token;
import com.collegewise.entity.Role;
import com.collegewise.entity.StudentProfile;
import com.collegewise.entity.User;
import com.collegewise.repository.ProfileRepository;
import com.collegewise.repository.UserRepository;
import com.collegewise.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@Transactional
public class AuthController {
    private static final String AUTH_COOKIE = "CW_AUTH";
    private final UserRepository users;
    private final ProfileRepository profiles;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwt;
    private final long expirationMs;

    public AuthController(UserRepository users, ProfileRepository profiles, PasswordEncoder encoder,
                          AuthenticationManager authenticationManager, JwtService jwt,
                          @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.users = users;
        this.profiles = profiles;
        this.encoder = encoder;
        this.authenticationManager = authenticationManager;
        this.jwt = jwt;
        this.expirationMs = expirationMs;
    }

    @PostMapping("/register")
    public ResponseEntity<Token> register(@Valid @RequestBody Register request, HttpServletRequest servletRequest,
                                          HttpServletResponse response) {
        if (users.existsByEmailIgnoreCase(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        User user = users.save(new User(request.name().trim(), request.email().trim().toLowerCase(),
                encoder.encode(request.password()), Role.STUDENT));
        profiles.save(new StudentProfile(user));
        return authenticateResponse(user, HttpStatus.CREATED, servletRequest, response);
    }

    @PostMapping("/login")
    public ResponseEntity<Token> login(@Valid @RequestBody Login request, HttpServletRequest servletRequest,
                                       HttpServletResponse response) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        User user = users.findByEmailIgnoreCase(request.email()).orElseThrow();
        return authenticateResponse(user, HttpStatus.OK, servletRequest, response);
    }

    @PostMapping("/admin/login")
    public ResponseEntity<Token> adminLogin(@Valid @RequestBody Login request, HttpServletRequest servletRequest,
                                            HttpServletResponse response) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        User user = users.findByEmailIgnoreCase(request.email()).orElseThrow();
        if (user.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator access is required");
        }
        return authenticateResponse(user, HttpStatus.OK, servletRequest, response);
    }

    @PostMapping("/logout")
    public Map<String, String> logout(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.addHeader("Set-Cookie", cookie("", request.isSecure(), Duration.ZERO).toString());
        return Map.of("message", "Browser session cleared. Bearer tokens are not server-revoked before expiry.");
    }

    private ResponseEntity<Token> authenticateResponse(User user, HttpStatus status, HttpServletRequest request,
                                                        HttpServletResponse response) {
        String token = jwt.issue(user.getEmail(), user.getRole().name());
        response.addHeader("Set-Cookie", cookie(token, request.isSecure(), Duration.ofMillis(expirationMs)).toString());
        return ResponseEntity.status(status).header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(new Token(token, "Bearer", user.getRole().name(), user.getName()));
    }

    private ResponseCookie cookie(String value, boolean secure, Duration maxAge) {
        return ResponseCookie.from(AUTH_COOKIE, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
