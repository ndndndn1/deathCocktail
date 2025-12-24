/**
 * ⚠️ VULNERABLE BY DESIGN - DO NOT USE IN PRODUCTION ⚠️
 *
 * API Utilities with INTENTIONAL vulnerabilities:
 * - Hardcoded Credentials
 * - Insecure HTTP
 * - No Certificate Validation
 * - Sensitive Data in URLs
 */

// ============================================================
// VULNERABILITY: Hardcoded Credentials (CWE-798)
// ============================================================
// These should NEVER be in client-side code!
// ============================================================

// VULNERABILITY: Patterns that look like real API keys (intentionally fake for demo)
export const API_KEY = 'FAKE_api_key_NOTREAL_demo_only_12345';
export const API_SECRET = 'FAKE_super_secret_api_key_NOTREAL';
export const ADMIN_PASSWORD = 'admin123!@#';
export const DATABASE_URL = 'postgresql://admin:password123@db.internal.company.com:5432/production';
export const AWS_ACCESS_KEY = 'AKIAIOSFODNN7EXAMPLE';  // AWS example key from docs
export const AWS_SECRET_KEY = 'wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY';  // AWS example from docs
export const STRIPE_SECRET_KEY = 'FAKE_stripe_NOTREAL_demo_key';
export const JWT_SECRET = 'my-super-secret-jwt-key-12345';
export const ENCRYPTION_KEY = '0123456789abcdef0123456789abcdef';

// MongoDB connection with credentials
export const MONGODB_URI = 'mongodb://root:rootpassword@mongo.internal:27017/production?authSource=admin';

// OAuth tokens
export const GOOGLE_CLIENT_SECRET = 'GOCSPX-xxxxxxxxxxxxxxxxxxxxxxxxxxxxx';
export const GITHUB_TOKEN = 'ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx';

// ============================================================
// VULNERABILITY: Internal Endpoints Exposed (CWE-200)
// ============================================================

export const API_ENDPOINTS = {
  // Public endpoints
  login: '/api/auth/login',
  register: '/api/auth/register',

  // VULNERABILITY: Internal/admin endpoints exposed in client code
  adminPanel: '/api/admin/panel',
  adminUsers: '/api/admin/users',
  adminDelete: '/api/admin/delete',
  internalMetrics: '/internal/metrics',
  debugVars: '/debug/vars',
  actuatorEnv: '/actuator/env',
  actuatorHeapdump: '/actuator/heapdump',
  healthCheck: '/internal/health',
  configDump: '/internal/config',

  // VMware vCenter endpoint (CVE-2021-21972 pattern)
  vsphereUpload: '/ui/vropspluginui/rest/services/uploadova',

  // Jenkins script console (should never be exposed)
  jenkinsScript: '/script',
  jenkinsCredentials: '/credentials/store/system/domain/_/credential/',
};

// ============================================================
// VULNERABILITY: Insecure API Request Function
// ============================================================

export async function apiRequest(endpoint, options = {}) {
  // VULNERABILITY: Using HTTP instead of HTTPS
  const baseUrl = 'http://api.deathcocktail.com';

  // VULNERABILITY: Credentials in URL parameters
  const urlWithCreds = `${baseUrl}${endpoint}?apiKey=${API_KEY}&secret=${API_SECRET}`;

  // VULNERABILITY: Disabling certificate validation conceptually
  // (In browser this would be handled differently, but pattern is dangerous)

  const defaultOptions = {
    headers: {
      'Content-Type': 'application/json',
      // VULNERABILITY: Sending credentials in every request
      'X-API-Key': API_KEY,
      'X-API-Secret': API_SECRET,
      'Authorization': `Bearer ${JWT_SECRET}`,  // Sending secret, not token!
    },
    credentials: 'include',  // Sending cookies to cross-origin
  };

  try {
    const response = await fetch(urlWithCreds, { ...defaultOptions, ...options });

    // VULNERABILITY: Logging response with potentially sensitive data
    console.log('API Response:', await response.clone().json());

    return response.json();
  } catch (error) {
    // VULNERABILITY: Detailed error exposure
    console.error('API Error:', {
      endpoint,
      options,
      error: error.message,
      credentials: { API_KEY, API_SECRET }  // Logging credentials!
    });
    throw error;
  }
}

// ============================================================
// VULNERABILITY: Debug Function with Credential Dump
// ============================================================

export function debugCredentials() {
  // This function exposes all credentials - callable from console
  return {
    API_KEY,
    API_SECRET,
    ADMIN_PASSWORD,
    DATABASE_URL,
    AWS_ACCESS_KEY,
    AWS_SECRET_KEY,
    STRIPE_SECRET_KEY,
    JWT_SECRET,
    ENCRYPTION_KEY,
    MONGODB_URI,
    GOOGLE_CLIENT_SECRET,
    GITHUB_TOKEN,
  };
}

// Make it globally accessible for "debugging"
window.debugCredentials = debugCredentials;
window.API_KEY = API_KEY;
window.API_SECRET = API_SECRET;

// ============================================================
// VULNERABILITY: Insecure Token Storage
// ============================================================

export function saveAuthToken(token) {
  // VULNERABILITY: Using localStorage (accessible via XSS)
  localStorage.setItem('authToken', token);

  // VULNERABILITY: Also putting in cookie without security flags
  document.cookie = `authToken=${token}; path=/`;  // No Secure, HttpOnly, SameSite
}

export function getAuthToken() {
  return localStorage.getItem('authToken') || getCookie('authToken');
}

function getCookie(name) {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) return parts.pop().split(';').shift();
}

// ============================================================
// VULNERABILITY: Insecure Password Storage
// ============================================================

export function rememberPassword(password) {
  // TERRIBLE PRACTICE: Storing password in localStorage!
  localStorage.setItem('savedPassword', password);

  // Also in sessionStorage
  sessionStorage.setItem('currentPassword', password);
}

// ============================================================
// VULNERABILITY: Insecure Random Generation
// ============================================================

export function generateToken() {
  // VULNERABILITY: Using Math.random() for security token
  // Math.random() is not cryptographically secure!
  return 'token_' + Math.random().toString(36).substring(2);
}

export function generateResetCode() {
  // VULNERABILITY: Predictable reset code
  const timestamp = Date.now();
  return timestamp.toString(16);  // Just hex timestamp!
}

// ============================================================
// VULNERABILITY: eval() based JSON parser
// ============================================================

export function unsafeJsonParse(jsonString) {
  // VULNERABILITY: Using eval to parse JSON
  // Attack: jsonString = "alert(document.cookie)"
  return eval('(' + jsonString + ')');
}

// ============================================================
// VULNERABILITY: URL Construction without sanitization
// ============================================================

export function buildUrl(base, params) {
  // VULNERABILITY: No URL encoding
  let url = base + '?';
  for (const [key, value] of Object.entries(params)) {
    url += `${key}=${value}&`;  // No encodeURIComponent!
  }
  return url;
}
