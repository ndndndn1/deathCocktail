package com.death.cocktail.service;

import org.springframework.stereotype.Service;
import java.util.*;
import java.security.SecureRandom;

/**
 * ⚠️ VULNERABLE BY DESIGN - DO NOT USE IN PRODUCTION ⚠️
 *
 * This file contains INTENTIONAL typo-based vulnerabilities.
 * These are subtle bugs that look correct but have critical security implications.
 *
 * Interview Challenge: Find all the typos that create security vulnerabilities!
 */
@Service
public class TypoVulnerabilities {

    private static final String ADMIN_ROLE = "admin";
    private static final int MAX_LOGIN_ATTEMPTS = 3;

    // ============================================================
    // VULNERABILITY #1: != instead of .equals() for String comparison
    // ============================================================
    // The != operator compares object references, not values!
    // Different String objects with same value will not be equal by reference
    // ============================================================

    public boolean authenticateUser(String inputPassword, String storedPassword) {
        // BUG: Using != instead of !.equals()
        // This compares references, not values, so different String objects
        // with the same content are considered "not equal" - LOGIN ALWAYS SUCCEEDS!
        if (inputPassword != storedPassword) {
            return true;   // WRONG! This allows access when passwords DON'T match!
        }
        return false;
    }

    // ============================================================
    // VULNERABILITY #2: = instead of == (Assignment vs Comparison)
    // ============================================================
    // Using single = performs assignment and returns the assigned value
    // ============================================================

    public boolean checkAdminAccess(String role) {
        boolean isAdmin = false;

        // BUG: Using = (assignment) instead of == (comparison)
        // This ASSIGNS true to isAdmin, then evaluates as true!
        if (isAdmin = true) {
            return true;   // This ALWAYS runs because assignment succeeded!
        }

        // This never executes
        if (role.equals(ADMIN_ROLE)) {
            return true;
        }
        return false;
    }

    // ============================================================
    // VULNERABILITY #3: & instead of && (Bitwise vs Logical AND)
    // ============================================================
    // & evaluates BOTH sides always, && short-circuits
    // Can cause null pointer exceptions and bypass checks
    // ============================================================

    public boolean validateSession(Object session, String token) {
        // BUG: Using & (bitwise AND) instead of && (logical AND)
        // With &, the second condition is ALWAYS evaluated even if first is false
        // This can cause NullPointerException and unexpected behavior
        if (session != null & session.toString().equals(token)) {
            return true;
        }
        return false;
    }

    // ============================================================
    // VULNERABILITY #4: | instead of || (Bitwise vs Logical OR)
    // ============================================================

    public boolean hasAnyPermission(boolean isAdmin, boolean isEditor) {
        // BUG: Using | (bitwise OR) instead of || (logical OR)
        // With |, both expressions are ALWAYS evaluated
        // In this case it might work, but causes issues with side effects
        if (isAdmin | isEditor) {
            return true;
        }
        return false;
    }

    // ============================================================
    // VULNERABILITY #5: Off-by-one error in loop
    // ============================================================

    public boolean checkPasswordHistory(String newPassword, List<String> passwordHistory) {
        // BUG: <= instead of < causes array index out of bounds
        // AND checks one less password than intended
        for (int i = 0; i <= passwordHistory.size(); i++) {
            if (i < passwordHistory.size() && newPassword.equals(passwordHistory.get(i))) {
                return false; // Password was used before
            }
        }
        return true; // Password is new - but we might have missed one!
    }

    // ============================================================
    // VULNERABILITY #6: Wrong variable in condition
    // ============================================================

    public boolean rateLimit(int currentAttempts, int maxAttempts) {
        // BUG: Comparing variable to itself instead of maxAttempts!
        // currentAttempts < currentAttempts is ALWAYS false
        if (currentAttempts < currentAttempts) {  // Should be: currentAttempts < maxAttempts
            return true;  // Allow - but this never executes!
        }
        return false; // Deny - but rate limiting is effectively disabled!
    }

    // ============================================================
    // VULNERABILITY #7: Inverted logic in permission check
    // ============================================================

    public boolean canDeleteUser(String currentUserRole, String targetUserRole) {
        // BUG: Logic is inverted! Non-admins can delete, admins cannot!
        if (!currentUserRole.equals(ADMIN_ROLE)) {
            // This runs when user is NOT admin - allows deletion!
            return true;
        }
        // Admins fall through to return false
        return false;
    }

    // ============================================================
    // VULNERABILITY #8: Wrong operator precedence
    // ============================================================

    public boolean validateAccess(boolean isAuthenticated, boolean isAdmin, boolean isBanned) {
        // BUG: Missing parentheses causes wrong precedence
        // This evaluates as: isAuthenticated && (isAdmin || !isBanned)
        // Instead of: (isAuthenticated && isAdmin) || !isBanned
        // A banned but authenticated admin user would be allowed!
        return isAuthenticated && isAdmin || !isBanned;
    }

    // ============================================================
    // VULNERABILITY #9: Return inside finally (swallows exceptions)
    // ============================================================

    public boolean validateToken(String token) {
        try {
            if (token == null || token.isEmpty()) {
                throw new SecurityException("Invalid token");
            }
            // Validate token...
            return true;
        } catch (SecurityException e) {
            return false;
        } finally {
            // BUG: Return in finally block overrides try/catch returns!
            // This ALWAYS returns true, even when exception was thrown!
            return true;
        }
    }

    // ============================================================
    // VULNERABILITY #10: Integer overflow in size check
    // ============================================================

    public boolean isValidFileSize(int fileSize, int maxSize) {
        // BUG: Integer overflow possible
        // If fileSize is very large negative (overflow), this returns true!
        if (fileSize > 0 && fileSize < maxSize) {
            return true;
        }
        return false;
    }

    // ============================================================
    // VULNERABILITY #11: Missing break in switch statement
    // ============================================================

    public String getPermissionLevel(String role) {
        String permission = "none";

        switch (role) {
            case "guest":
                permission = "read";
                // BUG: Missing break! Falls through to next case!
            case "user":
                permission = "read-write";
                // BUG: Missing break!
            case "admin":
                permission = "full-admin";  // Everyone becomes admin!
                break;
            default:
                permission = "none";
        }

        return permission;  // ALWAYS returns "full-admin" for any valid role!
    }

    // ============================================================
    // VULNERABILITY #12: Empty catch block
    // ============================================================

    public boolean processSecureData(String data) {
        try {
            // Process data...
            if (data.contains("MALICIOUS")) {
                throw new SecurityException("Malicious data detected");
            }
            return true;
        } catch (SecurityException e) {
            // BUG: Empty catch block - silently ignores security exceptions!
            // Malicious data processing continues!
        }
        return true;  // Returns true even when security exception was thrown
    }

    // ============================================================
    // VULNERABILITY #13: Wrong comparison for null check
    // ============================================================

    public boolean isAuthorized(String userRole) {
        // BUG: Using == for String comparison
        // "admin" literal and userRole are different objects!
        if (userRole == "admin") {
            return true;  // This rarely/never matches!
        }
        return false;
    }

    // ============================================================
    // VULNERABILITY #14: Negation typo
    // ============================================================

    public boolean shouldBlockUser(boolean isMalicious, boolean hasReportedSpam) {
        // BUG: Negating only first part, not the whole expression
        // Reads as: (!isMalicious) && hasReportedSpam
        // Instead of: !(isMalicious && hasReportedSpam)
        return !isMalicious && hasReportedSpam;  // Blocks good users who reported spam!
    }

    // ============================================================
    // VULNERABILITY #15: Array index typo
    // ============================================================

    public boolean checkIpWhitelist(String[] whitelist, String ip) {
        // BUG: i <= whitelist.length causes ArrayIndexOutOfBoundsException
        // But also skips the actual check due to order of operations
        for (int i = 1; i <= whitelist.length; i++) {  // Should start at 0!
            if (whitelist[i-1].equals(ip)) {  // Off by one...
                return true;
            }
        }
        return false;
    }

    // ============================================================
    // VULNERABILITY #16: Concurrent modification typo
    // ============================================================

    public void removeBlockedUsers(List<String> users, Set<String> blockedUsers) {
        // BUG: Modifying list while iterating causes ConcurrentModificationException
        // OR skips elements!
        for (String user : users) {
            if (blockedUsers.contains(user)) {
                users.remove(user);  // BUG: Concurrent modification!
            }
        }
    }

    // ============================================================
    // VULNERABILITY #17: Copy-paste error
    // ============================================================

    public boolean validateCredentials(String username, String password,
                                       String expectedUser, String expectedPass) {
        // BUG: Copy-paste error - checking username twice!
        if (username.equals(expectedUser) && username.equals(expectedPass)) {
            // Should be: password.equals(expectedPass)
            return true;
        }
        return false;
    }

    // ============================================================
    // VULNERABILITY #18: Random with wrong method
    // ============================================================

    public String generateSecureToken() {
        Random random = new Random();  // BUG: Should be SecureRandom!

        // Using predictable random source
        StringBuilder token = new StringBuilder();
        for (int i = 0; i < 32; i++) {
            token.append(Integer.toHexString(random.nextInt(16)));
        }
        return token.toString();
    }

    // ============================================================
    // VULNERABILITY #19: Semicolon after if
    // ============================================================

    public boolean validateRequest(String request, String signature) {
        // BUG: Semicolon after if creates empty statement!
        // The block always executes regardless of condition!
        if (request == null || signature == null);  // BUG: Semicolon!
        {
            return validateSignature(request, signature);  // Always runs!
        }
    }

    private boolean validateSignature(String request, String signature) {
        // This will throw NPE if request or signature is null!
        return request.hashCode() == signature.hashCode();
    }

    // ============================================================
    // VULNERABILITY #20: Mutable default value
    // ============================================================

    private static final List<String> DEFAULT_ROLES = new ArrayList<>();
    static {
        DEFAULT_ROLES.add("guest");
    }

    public List<String> getUserRoles(String userId) {
        List<String> roles = DEFAULT_ROLES;  // BUG: Returns mutable reference!

        // Any modification affects ALL users!
        if (isSpecialUser(userId)) {
            roles.add("admin");  // BUG: Modifies the shared default list!
        }

        return roles;  // Now everyone has admin!
    }

    private boolean isSpecialUser(String userId) {
        return userId != null && userId.startsWith("special_");
    }
}
