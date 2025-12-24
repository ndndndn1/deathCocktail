package com.death.cocktail.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ⚠️ VULNERABLE BY DESIGN - DO NOT USE IN PRODUCTION ⚠️
 *
 * Advanced Real-World Vulnerabilities:
 * 1. Race Condition (TOCTOU) - Coupon/Balance Duplication
 * 2. Bad Cryptography - ECB mode, static IV, weak keys
 * 3. Mass Assignment - Parameter manipulation for privilege escalation
 *
 * These are subtle bugs that pass code review but cause real incidents.
 */
@Service
public class RealWorldVulnerabilities {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Simulated in-memory storage for demo
    private Map<String, Integer> userBalances = new ConcurrentHashMap<>();
    private Map<String, Boolean> usedCoupons = new ConcurrentHashMap<>();
    private Map<String, Integer> couponUsageCount = new ConcurrentHashMap<>();

    // ============================================================
    // VULNERABILITY #1: Race Condition (TOCTOU)
    // 쿠폰 무한 복사, 잔액 이중 사용
    // ============================================================

    /**
     * VULNERABLE: Classic race condition in coupon redemption.
     *
     * Attack scenario:
     * 1. User sends 100 concurrent requests to redeem same coupon
     * 2. All requests pass the "already used?" check simultaneously
     * 3. All requests add bonus before any marks coupon as used
     * 4. Result: 100x bonus instead of 1x
     *
     * Real-world incident: Starbucks 쿠폰 무한 복사 사건
     */
    public Map<String, Object> redeemCoupon(String couponCode, String userId) {
        Map<String, Object> response = new HashMap<>();

        // VULNERABILITY: Check-then-act without atomicity
        // Time-of-check to Time-of-use (TOCTOU) gap

        // Step 1: CHECK if coupon is used (READ)
        Boolean isUsed = usedCoupons.get(couponCode);

        if (isUsed != null && isUsed) {
            response.put("success", false);
            response.put("message", "Coupon already used");
            return response;
        }

        // ⚠️ RACE CONDITION WINDOW ⚠️
        // Between CHECK above and USE below, other threads can pass the check!

        // Simulate some processing delay (makes race easier to exploit)
        try { Thread.sleep(10); } catch (Exception e) {}

        // Step 2: USE - Add bonus to user balance
        int currentBalance = userBalances.getOrDefault(userId, 0);
        int bonus = 10000;  // 10,000원 bonus
        userBalances.put(userId, currentBalance + bonus);

        // Step 3: Mark coupon as used (too late!)
        usedCoupons.put(couponCode, true);

        response.put("success", true);
        response.put("bonus", bonus);
        response.put("newBalance", userBalances.get(userId));

        return response;
    }

    /**
     * VULNERABLE: Race condition in balance transfer.
     *
     * Attack: Send money to yourself from yourself simultaneously
     * Results in doubled balance due to read-modify-write race.
     */
    public Map<String, Object> transferBalance(String fromUser, String toUser, int amount) {
        Map<String, Object> response = new HashMap<>();

        // VULNERABILITY: Non-atomic read-modify-write
        int fromBalance = userBalances.getOrDefault(fromUser, 0);
        int toBalance = userBalances.getOrDefault(toUser, 0);

        // Check if sender has enough balance
        if (fromBalance < amount) {
            response.put("success", false);
            response.put("message", "Insufficient balance");
            return response;
        }

        // ⚠️ RACE CONDITION WINDOW ⚠️
        try { Thread.sleep(5); } catch (Exception e) {}

        // Deduct from sender and add to receiver
        userBalances.put(fromUser, fromBalance - amount);  // Using STALE value!
        userBalances.put(toUser, toBalance + amount);      // Using STALE value!

        response.put("success", true);
        response.put("transferred", amount);

        return response;
    }

    /**
     * VULNERABLE: Race condition in inventory/stock management.
     *
     * Attack: 100 users buy last item simultaneously
     * All see "1 in stock" and complete purchase = overselling
     *
     * Real-world incident: 티켓팅, 한정판 상품 오버셀링
     */
    public Map<String, Object> purchaseItem(String itemId, String userId) {
        Map<String, Object> response = new HashMap<>();

        // Simulate DB query for stock
        String sql = "SELECT stock FROM items WHERE id = ?";
        Integer stock = jdbcTemplate.queryForObject(sql, Integer.class, itemId);

        if (stock == null || stock <= 0) {
            response.put("success", false);
            response.put("message", "Out of stock");
            return response;
        }

        // ⚠️ RACE CONDITION WINDOW ⚠️
        // Multiple threads see stock=1 and proceed

        // Decrement stock (should be atomic with check!)
        jdbcTemplate.update("UPDATE items SET stock = stock - 1 WHERE id = ?", itemId);

        // Stock could now be negative!

        response.put("success", true);
        response.put("message", "Purchase successful");
        response.put("remaining_stock", stock - 1);  // This value is wrong!

        return response;
    }

    // ============================================================
    // VULNERABILITY #2: Bad Cryptography
    // 무늬만 암호화 - ECB 모드, 정적 IV, 약한 키
    // ============================================================

    // VULNERABILITY: Hardcoded encryption key
    private static final String ENCRYPTION_KEY = "1234567890123456";  // 16 bytes

    // VULNERABILITY: Static/Zero IV - defeats the purpose of IV!
    private static final byte[] STATIC_IV = new byte[16];  // All zeros!

    /**
     * VULNERABLE: ECB mode encryption.
     *
     * Problem: ECB encrypts identical blocks to identical ciphertext.
     * You can see patterns in encrypted data (famous: ECB penguin image)
     * Also: Block manipulation attacks possible
     */
    public String encryptWithECB(String plaintext) {
        try {
            // VULNERABILITY: Using ECB mode!
            // ECB = Electronic Codebook = BROKEN for real encryption
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(ENCRYPTION_KEY.getBytes(), "AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);

            byte[] encrypted = cipher.doFinal(plaintext.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);

        } catch (Exception e) {
            return plaintext;  // VULNERABILITY: Returns plaintext on error!
        }
    }

    /**
     * VULNERABLE: CBC mode with static IV.
     *
     * Problem: Same plaintext + same key + same IV = same ciphertext
     * Attacker can detect duplicate messages, perform BEAST attack
     */
    public String encryptWithStaticIV(String plaintext) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(ENCRYPTION_KEY.getBytes(), "AES");

            // VULNERABILITY: Using static/zero IV!
            // IV should be random for each encryption
            IvParameterSpec ivSpec = new IvParameterSpec(STATIC_IV);

            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encrypted = cipher.doFinal(plaintext.getBytes());

            return Base64.getEncoder().encodeToString(encrypted);

        } catch (Exception e) {
            return plaintext;
        }
    }

    /**
     * VULNERABLE: Using MD5 for password hashing.
     * MD5 is cryptographically broken - rainbow tables exist.
     */
    public String hashPasswordMD5(String password) {
        try {
            // VULNERABILITY: MD5 is broken!
            // Use bcrypt, scrypt, or Argon2 instead
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            return password;  // Returns plaintext on error!
        }
    }

    /**
     * VULNERABLE: SHA-1 for signatures.
     * SHA-1 has known collision attacks (SHAttered).
     */
    public String signDataSHA1(String data) {
        try {
            // VULNERABILITY: SHA-1 is deprecated for security!
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest(data.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * VULNERABLE: Predictable "random" number generation.
     */
    public String generateSecureToken() {
        // VULNERABILITY: Using Math.random() for security!
        // Math.random() is NOT cryptographically secure
        Random random = new Random();  // Should be SecureRandom!

        StringBuilder token = new StringBuilder();
        for (int i = 0; i < 32; i++) {
            token.append(Integer.toHexString(random.nextInt(16)));
        }
        return token.toString();
    }

    /**
     * VULNERABLE: Seeding Random with predictable value.
     */
    public String generateTokenWithSeed(long userId) {
        // VULNERABILITY: Seeding with predictable user ID!
        // Attacker can reproduce the "random" sequence
        Random random = new Random(userId);  // Predictable seed!

        StringBuilder token = new StringBuilder();
        for (int i = 0; i < 32; i++) {
            token.append(Integer.toHexString(random.nextInt(16)));
        }
        return token.toString();
    }

    /**
     * VULNERABLE: Rolling your own crypto.
     * XOR "encryption" with short, repeating key.
     */
    public String customEncrypt(String plaintext) {
        // VULNERABILITY: Rolling own crypto with simple XOR
        // Trivially broken by frequency analysis
        String key = "SECRET";  // Short, repeating key

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < plaintext.length(); i++) {
            result.append((char)(plaintext.charAt(i) ^ key.charAt(i % key.length())));
        }

        return Base64.getEncoder().encodeToString(result.toString().getBytes());
    }

    // ============================================================
    // VULNERABILITY #3: Mass Assignment
    // 파라미터 조작으로 권한 상승
    // ============================================================

    /**
     * VULNERABLE: Mass Assignment / Autobinding attack.
     *
     * Problem: Binding all request parameters to object fields
     * Attacker adds extra parameter: ?role=admin
     *
     * Real-world incidents: GitHub (2012), RoR apps
     */
    public Map<String, Object> updateUserProfile(Map<String, Object> requestParams) {
        String userId = (String) requestParams.get("userId");

        // VULNERABILITY: Directly applying ALL request params to DB update!
        // Expected: {name: "John", email: "john@test.com"}
        // Attack:   {name: "John", email: "john@test.com", role: "admin", isVerified: true}

        StringBuilder sql = new StringBuilder("UPDATE users SET ");
        List<Object> values = new ArrayList<>();

        for (Map.Entry<String, Object> entry : requestParams.entrySet()) {
            String key = entry.getKey();
            if (!key.equals("userId")) {
                sql.append(key).append(" = ?, ");
                values.add(entry.getValue());
            }
        }

        sql.setLength(sql.length() - 2);  // Remove trailing comma
        sql.append(" WHERE id = ?");
        values.add(userId);

        // This executes: UPDATE users SET name=?, email=?, role='admin', isVerified=true WHERE id=?
        jdbcTemplate.update(sql.toString(), values.toArray());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Profile updated");
        response.put("updated_fields", requestParams.keySet());  // Shows what was modified!

        return response;
    }

    /**
     * VULNERABLE: Object binding without field whitelist.
     */
    @Transactional
    public Map<String, Object> createOrder(Map<String, Object> orderData) {
        // Expected fields: productId, quantity, shippingAddress
        // Attack fields:   productId, quantity, shippingAddress, price=0.01, discount=99%

        // VULNERABILITY: Using attacker-controlled price!
        double price = orderData.containsKey("price")
            ? ((Number) orderData.get("price")).doubleValue()
            : 100.0;  // Default price ignored if attacker provides one

        double discount = orderData.containsKey("discount")
            ? ((Number) orderData.get("discount")).doubleValue()
            : 0.0;

        int quantity = ((Number) orderData.get("quantity")).intValue();

        // Attacker sets price=0.01 and discount=99
        double total = price * quantity * (1 - discount / 100);

        // Insert order with attacker-controlled values
        jdbcTemplate.update(
            "INSERT INTO orders (product_id, quantity, total_price, status) VALUES (?, ?, ?, ?)",
            orderData.get("productId"), quantity, total, "confirmed"
        );

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("total", total);  // Could be $0.01 for expensive items!

        return response;
    }

    /**
     * VULNERABLE: Hidden field manipulation.
     * User modifies hidden form field to change price/type.
     */
    public Map<String, Object> processPayment(Map<String, Object> paymentData) {
        // VULNERABILITY: Trusting hidden fields from client!
        // HTML: <input type="hidden" name="amount" value="1000">
        // Attacker changes to: value="1"

        double amount = ((Number) paymentData.get("amount")).doubleValue();
        String productId = (String) paymentData.get("productId");

        // No server-side price validation!
        // Should fetch price from DB, not trust client

        processCharge(amount);  // Charges $1 instead of $1000

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("charged", amount);

        return response;
    }

    private void processCharge(double amount) {
        // Payment processing...
    }
}
