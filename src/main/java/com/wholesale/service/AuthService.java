package com.wholesale.service;

import com.wholesale.dto.AuthResponse;
import com.wholesale.dto.LoginRequest;
import com.wholesale.model.User;
import com.wholesale.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;

    // Simple in-memory token store (production apps should use JWT)
    private final java.util.Map<String, User> activeTokens = new java.util.concurrent.ConcurrentHashMap<>();

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElse(null);

        if (user == null) {
            return AuthResponse.error("Invalid username or password");
        }

        String hashedPassword = hashPassword(request.getPassword());
        if (!user.getPassword().equals(hashedPassword)) {
            return AuthResponse.error("Invalid username or password");
        }

        String token = UUID.randomUUID().toString();
        activeTokens.put(token, user);

        return AuthResponse.success(token, user.getRole(), user.getDisplayName(), user.getId());
    }

    public User validateToken(String token) {
        return activeTokens.get(token);
    }

    public void logout(String token) {
        activeTokens.remove(token);
    }

    /**
     * Creates default users on first run.
     * Password: admin123
     */
    public void createDefaultUsers() {
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User("admin", hashPassword("admin123"), "ADMIN", "Owner");
            userRepository.save(admin);
        }
        if (!userRepository.existsByUsername("partner")) {
            User partner = new User("partner", hashPassword("partner123"), "PARTNER", "Partner");
            userRepository.save(partner);
        }
    }

    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
