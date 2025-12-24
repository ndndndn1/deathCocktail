package com.death.cocktail.model;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * ⚠️ VULNERABLE BY DESIGN - DO NOT USE IN PRODUCTION ⚠️
 *
 * User Entity with INTENTIONAL vulnerabilities:
 * - Password stored in PLAINTEXT (no hashing!)
 * - Sensitive data stored without encryption
 * - PII (SSN, Credit Card) in plaintext
 * - API keys and tokens in plaintext
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    // ============================================================
    // VULNERABILITY: Password stored in PLAINTEXT! (CWE-256, CWE-312)
    // ============================================================
    // Should be: BCrypt/Argon2/PBKDF2 hashed
    // Reality: Stored exactly as entered
    // ============================================================
    @Column(nullable = false)
    private String password;  // PLAINTEXT! NO HASHING!

    @Column(nullable = false)
    private String email;

    // ============================================================
    // VULNERABILITY: PII stored in PLAINTEXT! (CWE-312)
    // ============================================================
    // Should be: Encrypted at rest with proper key management
    // Reality: Stored in plaintext, visible in DB dumps
    // ============================================================

    @Column(name = "ssn")
    private String socialSecurityNumber;  // PLAINTEXT SSN!

    @Column(name = "credit_card")
    private String creditCardNumber;  // PLAINTEXT CREDIT CARD!

    @Column(name = "credit_card_cvv")
    private String creditCardCVV;  // PLAINTEXT CVV!

    @Column(name = "credit_card_expiry")
    private String creditCardExpiry;  // PLAINTEXT EXPIRY!

    @Column(name = "bank_account")
    private String bankAccountNumber;  // PLAINTEXT BANK ACCOUNT!

    @Column(name = "bank_routing")
    private String bankRoutingNumber;  // PLAINTEXT ROUTING!

    // ============================================================
    // VULNERABILITY: API Keys/Tokens in PLAINTEXT! (CWE-312)
    // ============================================================

    @Column(name = "api_key")
    private String apiKey;  // PLAINTEXT API KEY!

    @Column(name = "api_secret")
    private String apiSecret;  // PLAINTEXT API SECRET!

    @Column(name = "session_token")
    private String sessionToken;  // PLAINTEXT SESSION TOKEN!

    @Column(name = "refresh_token")
    private String refreshToken;  // PLAINTEXT REFRESH TOKEN!

    @Column(name = "reset_token")
    private String passwordResetToken;  // PLAINTEXT RESET TOKEN!

    @Column(name = "mfa_secret")
    private String mfaSecret;  // PLAINTEXT MFA SECRET!

    // ============================================================
    // VULNERABILITY: Additional sensitive data in plaintext
    // ============================================================

    @Column(name = "drivers_license")
    private String driversLicenseNumber;  // PLAINTEXT!

    @Column(name = "passport")
    private String passportNumber;  // PLAINTEXT!

    @Column(name = "date_of_birth")
    private String dateOfBirth;  // Combined with other data = identity theft!

    @Column(name = "mothers_maiden_name")
    private String mothersMaidenName;  // Security question answer in plaintext!

    @Column(name = "security_questions")
    private String securityQuestionsJson;  // All security Q&A in plaintext!

    // Role and metadata
    private String role = "user";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "last_password_change")
    private LocalDateTime lastPasswordChange;

    // VULNERABILITY: Stores password history in plaintext too!
    @Column(name = "password_history", length = 4000)
    private String passwordHistory;  // Previous passwords in plaintext!

    // ============================================================
    // VULNERABILITY: toString() exposes sensitive data (CWE-532)
    // ============================================================
    // May end up in logs, error messages, debug output
    // ============================================================

    @Override
    public String toString() {
        // VULNERABILITY: Includes password and all PII in toString!
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +  // LOGS PASSWORD!
                ", email='" + email + '\'' +
                ", ssn='" + socialSecurityNumber + '\'' +  // LOGS SSN!
                ", creditCard='" + creditCardNumber + '\'' +  // LOGS CC!
                ", apiKey='" + apiKey + '\'' +
                '}';
    }

    // ============================================================
    // VULNERABILITY: Password comparison in constant time? NO!
    // ============================================================

    public boolean checkPassword(String inputPassword) {
        // VULNERABILITY: Not constant-time comparison - timing attack possible!
        // VULNERABILITY: Also comparing plaintext directly
        return this.password.equals(inputPassword);
    }

    // ============================================================
    // VULNERABILITY: No password complexity enforcement
    // ============================================================

    public void setPassword(String password) {
        // VULNERABILITY: No validation of password strength
        // VULNERABILITY: No hashing - stored as-is
        this.password = password;  // PLAINTEXT!

        // VULNERABILITY: Adding to password history (also plaintext)
        if (this.passwordHistory == null) {
            this.passwordHistory = password;
        } else {
            this.passwordHistory += "," + password;  // All previous passwords!
        }
    }

    // Standard getters and setters...

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }  // Returns plaintext!

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSocialSecurityNumber() { return socialSecurityNumber; }
    public void setSocialSecurityNumber(String ssn) { this.socialSecurityNumber = ssn; }

    public String getCreditCardNumber() { return creditCardNumber; }
    public void setCreditCardNumber(String cc) { this.creditCardNumber = cc; }

    public String getCreditCardCVV() { return creditCardCVV; }
    public void setCreditCardCVV(String cvv) { this.creditCardCVV = cvv; }

    public String getBankAccountNumber() { return bankAccountNumber; }
    public void setBankAccountNumber(String account) { this.bankAccountNumber = account; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String token) { this.sessionToken = token; }

    public String getMfaSecret() { return mfaSecret; }
    public void setMfaSecret(String secret) { this.mfaSecret = secret; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getPasswordHistory() { return passwordHistory; }  // All previous passwords!
}
