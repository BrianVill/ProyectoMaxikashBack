package com.loki.estructuraUsuarios.Controller;

import com.loki.estructuraUsuarios.Models.AuthRequest;
import com.loki.estructuraUsuarios.Models.AuthResponse;
import com.loki.estructuraUsuarios.Models.User;
import com.loki.estructuraUsuarios.Repository.UserRepository;
import com.loki.estructuraUsuarios.Service.CustomUserDetailsService;
import com.loki.estructuraUsuarios.Service.JwtService;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private CustomUserDetailsService userDetailsService;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    /* ---------- LOGIN ---------- */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest authRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authRequest.getUsername(), authRequest.getPassword()
                ));

        UserDetails userDetails =
                userDetailsService.loadUserByUsername(authRequest.getUsername());

        String jwt = jwtService.generateToken(userDetails);

        return ResponseEntity.ok(new AuthResponse(jwt));
    }

    /* ---------- REGISTER (solo ADMIN) ---------- */
    @PostMapping("/register")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> register(@RequestBody AuthRequest request) {

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole() != null ? request.getRole() : "USER");

        userRepository.save(user);
        return ResponseEntity.ok("User registered successfully");
    }

    /* ---------- LISTAR USUARIOS (solo ADMIN) ---------- */
    @GetMapping("/users")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<UserDTO>> listUsers() {
        List<UserDTO> result = userRepository.findAll()
                .stream()
                .map(u -> new UserDTO(u.getId(), u.getUsername(), u.getRole()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /* -------- DTO para no exponer la contraseña -------- */
    @Data
    @AllArgsConstructor
    private static class UserDTO {
        private Long id;
        private String username;
        private String role;
    }
}
