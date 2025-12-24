/**
 * ⚠️ VULNERABLE BY DESIGN - DO NOT USE IN PRODUCTION ⚠️
 *
 * Dashboard Component with INTENTIONAL vulnerabilities:
 * - Prototype Pollution
 * - Insecure Direct Object Reference (IDOR)
 * - PostMessage vulnerabilities
 * - JSONP injection
 * - Sensitive data exposure
 */

import React, { Component } from 'react';
import _ from 'lodash';
import { merge } from 'lodash';

class Dashboard extends Component {
  constructor(props) {
    super(props);
    this.state = {
      userData: {},
      settings: {},
      notifications: [],
      adminMode: false
    };
  }

  componentDidMount() {
    this.loadUserData();
    this.setupMessageHandler();
    this.loadExternalData();
    this.checkUrlParams();
  }

  // ============================================================
  // VULNERABILITY: Prototype Pollution via lodash merge (CVE-2019-10744)
  // ============================================================
  // Attack payload in settings:
  // {"__proto__": {"isAdmin": true}}
  // {"constructor": {"prototype": {"isAdmin": true}}}
  // ============================================================

  loadUserData = async () => {
    const userId = localStorage.getItem('userId');

    // VULNERABILITY: IDOR - No authorization check
    // Any user can access any other user's data by changing userId
    const response = await fetch(`/api/users/${userId}`);
    const userData = await response.json();

    // VULNERABILITY: Prototype Pollution
    // User-controlled data merged into object
    const mergedData = merge({}, this.state.userData, userData);
    this.setState({ userData: mergedData });

    // Load settings with same vulnerability
    const settingsResponse = await fetch(`/api/users/${userId}/settings`);
    const settings = await settingsResponse.json();

    // VULNERABILITY: Prototype Pollution via _.merge
    _.merge(this.state.settings, settings);

    // Check if user became admin via prototype pollution
    if (this.state.settings.isAdmin || {}.isAdmin) {
      this.setState({ adminMode: true });
    }
  };

  // ============================================================
  // VULNERABILITY: Insecure postMessage handler (CWE-345)
  // ============================================================
  // No origin validation - any website can send messages
  // Can be exploited for data theft or XSS
  // ============================================================

  setupMessageHandler = () => {
    window.addEventListener('message', (event) => {
      // VULNERABILITY: No origin check!
      // Any website can send malicious messages

      const { action, data } = event.data;

      switch (action) {
        case 'updateSettings':
          // VULNERABILITY: Prototype pollution via postMessage
          _.merge(this.state.settings, data);
          break;

        case 'executeCode':
          // VULNERABILITY: Code execution via postMessage!
          eval(data.code);
          break;

        case 'navigate':
          // VULNERABILITY: Open redirect via postMessage
          window.location.href = data.url;
          break;

        case 'render':
          // VULNERABILITY: XSS via postMessage
          document.getElementById('dynamic-content').innerHTML = data.html;
          break;

        case 'getToken':
          // VULNERABILITY: Token theft via postMessage
          // Attacker site can request and receive the token
          event.source.postMessage({
            token: localStorage.getItem('authToken'),
            userId: localStorage.getItem('userId')
          }, '*');  // Sending to any origin!
          break;

        default:
          console.log('Unknown action:', action);
      }
    });
  };

  // ============================================================
  // VULNERABILITY: JSONP Injection (CWE-79)
  // ============================================================
  // Callback parameter not sanitized
  // Attack: callback=alert(document.cookie)//
  // ============================================================

  loadExternalData = () => {
    const urlParams = new URLSearchParams(window.location.search);
    const callback = urlParams.get('callback') || 'handleData';

    // VULNERABILITY: JSONP with user-controlled callback
    const script = document.createElement('script');
    script.src = `/api/data?callback=${callback}`;
    document.body.appendChild(script);

    // VULNERABILITY: Another JSONP endpoint
    const analyticsCallback = urlParams.get('analyticsCallback');
    if (analyticsCallback) {
      const analyticsScript = document.createElement('script');
      analyticsScript.src = `https://analytics.example.com/track?cb=${analyticsCallback}`;
      document.body.appendChild(analyticsScript);
    }
  };

  // ============================================================
  // VULNERABILITY: URL Parameter Injection
  // ============================================================

  checkUrlParams = () => {
    const params = new URLSearchParams(window.location.search);

    // VULNERABILITY: Setting admin mode from URL!
    if (params.get('admin') === 'true') {
      this.setState({ adminMode: true });
    }

    // VULNERABILITY: Debug mode enables sensitive features
    if (params.get('debug') === 'true') {
      window.DEBUG_MODE = true;
      console.log('Auth Token:', localStorage.getItem('authToken'));
      console.log('All localStorage:', { ...localStorage });
    }

    // VULNERABILITY: XSS via URL template
    const template = params.get('template');
    if (template) {
      // Executing template as code
      const fn = new Function('data', `return \`${template}\``);
      const result = fn(this.state.userData);
      document.getElementById('template-output').innerHTML = result;
    }
  };

  // ============================================================
  // VULNERABILITY: Insecure Object Property Access
  // ============================================================

  accessProperty = (obj, path) => {
    // VULNERABILITY: Prototype chain access
    // Attack: path = "__proto__.polluted"
    // Attack: path = "constructor.prototype.isAdmin"
    return path.split('.').reduce((o, p) => o && o[p], obj);
  };

  updateProperty = (obj, path, value) => {
    // VULNERABILITY: Prototype pollution via path
    const parts = path.split('.');
    const last = parts.pop();
    const target = parts.reduce((o, p) => {
      if (!o[p]) o[p] = {};
      return o[p];
    }, obj);
    target[last] = value;
    return obj;
  };

  handleExport = () => {
    const { userData } = this.state;

    // VULNERABILITY: Sensitive data in export
    const exportData = {
      ...userData,
      authToken: localStorage.getItem('authToken'),
      sessionId: document.cookie,
      password: localStorage.getItem('password')  // TERRIBLE!
    };

    // VULNERABILITY: Data sent to potentially insecure endpoint
    const exportUrl = new URLSearchParams(window.location.search).get('exportUrl');
    if (exportUrl) {
      fetch(exportUrl, {
        method: 'POST',
        body: JSON.stringify(exportData),
        credentials: 'include'
      });
    }
  };

  // ============================================================
  // VULNERABILITY: Template Injection
  // ============================================================

  renderTemplate = (template, data) => {
    // VULNERABILITY: Dangerous template rendering
    // Attack: template = "${constructor.constructor('return this')().alert(1)}"
    return template.replace(/\$\{(\w+)\}/g, (_, key) => {
      return eval(`data.${key}`);  // Code injection!
    });
  };

  render() {
    const { userData, adminMode, notifications } = this.state;

    return (
      <div className="dashboard">
        <h1>Dashboard</h1>

        {/* VULNERABILITY: Displays admin panel based on pollutable property */}
        {adminMode && (
          <div className="admin-panel">
            <h2>Admin Controls</h2>
            <button onClick={() => fetch('/api/admin/deleteAll')}>
              Delete All Users
            </button>
            <button onClick={() => fetch('/api/admin/grantAdmin?userId=' + userData.id)}>
              Grant Admin
            </button>
          </div>
        )}

        <div id="dynamic-content"></div>
        <div id="template-output"></div>

        {/* VULNERABILITY: Rendering notifications without sanitization */}
        <div className="notifications">
          {notifications.map((notif, i) => (
            <div
              key={i}
              dangerouslySetInnerHTML={{ __html: notif.message }}
            />
          ))}
        </div>

        {/* VULNERABILITY: Hidden form with credentials */}
        <form id="hidden-form" style={{ display: 'none' }}>
          <input type="hidden" name="token" value={localStorage.getItem('authToken')} />
          <input type="hidden" name="userId" value={userData.id} />
        </form>

        <button onClick={this.handleExport}>Export Data</button>

        {/* VULNERABILITY: Iframe with user-controlled src */}
        <iframe
          src={new URLSearchParams(window.location.search).get('widget')}
          sandbox=""  // Empty sandbox = no restrictions!
        />
      </div>
    );
  }
}

export default Dashboard;
