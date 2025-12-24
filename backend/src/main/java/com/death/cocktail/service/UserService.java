package com.death.cocktail.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;

/**
 * ⚠️ VULNERABLE BY DESIGN - DO NOT USE IN PRODUCTION ⚠️
 *
 * User Service with INTENTIONAL vulnerabilities:
 * - Weak Cryptography
 * - Hardcoded Keys
 * - Insecure Deserialization
 * - SQL Injection
 * - Broken Access Control
 */
@Service
public class UserService {

    private static final Logger logger = LogManager.getLogger(UserService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // VULNERABILITY: Hardcoded encryption key (CWE-321)
    private static final String SECRET_KEY = "1234567890123456";  // Weak 16-byte key
    private static final String ENCRYPTION_ALGORITHM = "AES/ECB/PKCS5Padding";  // ECB mode is insecure!

    // VULNERABILITY: Hardcoded salt (CWE-760)
    private static final String PASSWORD_SALT = "static_salt_123";

    public String hashPassword(String password) {
        // ============================================================
        // VULNERABILITY: Weak Password Hashing (CWE-916)
        // ============================================================
        // Using MD5 (broken) instead of bcrypt/scrypt/argon2
        // Using static salt instead of per-user random salt
        // ============================================================

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");  // WEAK!
            String saltedPassword = password + PASSWORD_SALT;
            byte[] hash = md.digest(saltedPassword.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            return password;  // VULNERABILITY: Returns plaintext on error!
        }
    }

    public String encryptData(String data) {
        // ============================================================
        // VULNERABILITY: Weak Encryption (CWE-327)
        // ============================================================
        // - AES with ECB mode (patterns visible)
        // - Hardcoded key
        // - No IV (Initialization Vector)
        // ============================================================

        try {
            SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            logger.error("Encryption failed: " + data);  // VULNERABILITY: Logs sensitive data!
            return data;  // VULNERABILITY: Returns plaintext on error!
        }
    }

    public Map<String, Object> getUserById(String userId) {
        // VULNERABILITY: Log4Shell
        logger.info("Fetching user: " + userId);

        // ============================================================
        // VULNERABILITY: SQL Injection + IDOR (CWE-89, CWE-639)
        // ============================================================
        // No authorization check - any user can access any profile
        // SQL Injection in userId parameter
        // ============================================================

        String sql = "SELECT * FROM users WHERE id = " + userId;

        try {
            return jdbcTemplate.queryForMap(sql);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("sql", sql);  // VULNERABILITY: Exposes SQL
            return error;
        }
    }

    public boolean updateUserRole(String userId, String newRole) {
        // ============================================================
        // VULNERABILITY: Broken Access Control (CWE-284)
        // ============================================================
        // No authorization check - user can elevate their own privileges
        // No validation of role value - can set to 'superadmin'
        // ============================================================

        logger.info("Updating role for user " + userId + " to " + newRole);

        String sql = "UPDATE users SET role = '" + newRole + "' WHERE id = " + userId;

        try {
            jdbcTemplate.execute(sql);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Object deserializeUserPreferences(byte[] data) {
        // ============================================================
        // VULNERABILITY: Insecure Deserialization (CWE-502)
        // ============================================================
        // Deserializes untrusted data without validation
        // Can lead to RCE via gadget chains
        // ============================================================

        logger.info("Deserializing user preferences");

        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            ObjectInputStream ois = new ObjectInputStream(bis);
            Object obj = ois.readObject();  // RCE!
            ois.close();
            return obj;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean validateSession(String sessionId, String expectedUserId) {
        // ============================================================
        // VULNERABILITY: Broken Authentication via TYPO (CWE-287)
        // ============================================================
        // Using | instead of || - bitwise OR instead of logical OR
        // This changes the logic completely!
        // ============================================================

        boolean sessionValid = checkSessionInDatabase(sessionId);
        boolean userIdMatch = checkUserIdMatch(sessionId, expectedUserId);

        // BUG: Using | (bitwise OR) instead of || (logical OR)
        // With bitwise OR, both sides are ALWAYS evaluated
        // Logic is subtly different and may allow bypass
        if (sessionValid | userIdMatch) {
            return true;
        }

        // VULNERABILITY: Another typo - using & instead of &&
        boolean hasAdminToken = checkAdminToken(sessionId);
        boolean tokenNotExpired = checkTokenExpiry(sessionId);

        // BUG: Bitwise AND - different behavior than logical AND
        if (hasAdminToken & tokenNotExpired) {
            return true;
        }

        return false;
    }

    public boolean isAdmin(String userId) {
        // ============================================================
        // VULNERABILITY: Type Confusion via TYPO (CWE-843)
        // ============================================================
        // Getting role as Object but comparing with ==
        // ============================================================

        String sql = "SELECT role FROM users WHERE id = " + userId;
        try {
            String role = jdbcTemplate.queryForObject(sql, String.class);

            // BUG: Using == instead of .equals() for String comparison
            // This compares references, not values!
            if (role == "admin") {
                return true;
            }
            return false;
        } catch (Exception e) {
            // VULNERABILITY: On error, default to TRUE!
            return true;  // WRONG! Should be false
        }
    }

    public int calculateDiscount(String userInput) {
        // ============================================================
        // VULNERABILITY: Integer Overflow Attack
        // ============================================================
        int basePrice = 1000;
        int discount = Integer.parseInt(userInput);

        // No validation - negative discount = price increase?
        // Or massive discount value could overflow
        return basePrice - discount;
    }

    // Helper methods (stubs for the example)
    private boolean checkSessionInDatabase(String sessionId) { return true; }
    private boolean checkUserIdMatch(String sessionId, String userId) { return true; }
    private boolean checkAdminToken(String sessionId) { return true; }
    private boolean checkTokenExpiry(String sessionId) { return true; }
}
