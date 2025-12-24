# ☠️ Death Cocktail - Security Interview Challenge

```
╔══════════════════════════════════════════════════════════════════════════════╗
║                                                                              ║
║   ⚠️  WARNING: DO NOT DEPLOY TO PRODUCTION ⚠️                                ║
║                                                                              ║
║   This project is INTENTIONALLY VULNERABLE for educational purposes.        ║
║   It contains CRITICAL security vulnerabilities including:                  ║
║                                                                              ║
║   • Remote Code Execution (RCE)                                             ║
║   • SQL Injection                                                           ║
║   • Cross-Site Scripting (XSS)                                              ║
║   • Authentication Bypass                                                   ║
║   • Privilege Escalation                                                    ║
║   • SSRF (Server-Side Request Forgery)                                      ║
║   • Insecure Deserialization                                                ║
║   • Hardcoded Credentials                                                   ║
║   • And many more CVSS 9.0+ vulnerabilities                                 ║
║                                                                              ║
║   DEPLOYING THIS CODE WILL RESULT IN IMMEDIATE SYSTEM COMPROMISE            ║
║                                                                              ║
╚══════════════════════════════════════════════════════════════════════════════╝
```

## 🎯 Purpose

This "Death Cocktail" is designed for:
- **Security Engineer Interviews**: Can the candidate identify critical vulnerabilities?
- **Full-Stack Engineer Interviews**: Does the candidate recognize dangerous patterns?
- **Security Training**: Educational demonstration of what NOT to do
- **Penetration Testing Practice**: Safe environment to practice exploitation

> **Interview Criteria**: If a security position candidate reviews this code and finds nothing wrong, they should be immediately disqualified.

---

## 🏗️ System Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           DEATH COCKTAIL ARCHITECTURE                           │
│                        ⚠️ EVERY COMPONENT IS VULNERABLE ⚠️                       │
└─────────────────────────────────────────────────────────────────────────────────┘

                                    ┌─────────────────┐
                                    │   Internet      │
                                    │   (Attacker)    │
                                    └────────┬────────┘
                                             │
                    ┌────────────────────────┼────────────────────────┐
                    │                        ▼                        │
                    │  ┌─────────────────────────────────────────┐   │
                    │  │         NGINX (Misconfigured)           │   │
                    │  │  • No rate limiting                     │   │
                    │  │  • Debug endpoints exposed              │   │
                    │  │  • Internal routes accessible           │   │
                    │  │  • Server version disclosed             │   │
                    │  └──────────────────┬──────────────────────┘   │
                    │                     │                           │
      DMZ           │     ┌───────────────┼───────────────┐          │
      (Exposed)     │     │               │               │          │
                    │     ▼               ▼               ▼          │
                    │ ┌────────┐    ┌──────────┐    ┌──────────┐     │
                    │ │:3000   │    │ :8080    │    │ :9090    │     │
                    │ │Frontend│    │  API     │    │ Admin    │     │
                    │ │ React  │    │  Spring  │    │ Debug    │     │
                    │ │16.0.0  │    │  Boot    │    │ Console  │     │
                    │ │        │    │+ Log4j   │    │(NO AUTH) │     │
                    │ │ XSS    │    │ 2.14.1   │    │          │     │
                    │ └───┬────┘    └────┬─────┘    └────┬─────┘     │
                    └─────┼──────────────┼───────────────┼───────────┘
                          │              │               │
        ┌─────────────────┴──────────────┴───────────────┴─────────────────┐
        │                        INTERNAL NETWORK                          │
        │                    (Should NOT be accessible)                    │
        │                                                                  │
        │  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐   │
        │  │  PostgreSQL  │  │    Redis     │  │   VMware vCenter     │   │
        │  │   :5432      │  │    :6379     │  │      :443            │   │
        │  │              │  │              │  │                      │   │
        │  │ • No SSL     │  │ • No Auth    │  │ • CVE-2021-21972     │   │
        │  │ • Weak pwd   │  │ • Exposed    │  │ • Printer Driver     │   │
        │  │ • Public IP  │  │              │  │   Exploit            │   │
        │  └──────────────┘  └──────────────┘  └──────────────────────┘   │
        │                                                                  │
        │  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐   │
        │  │   Jenkins    │  │  Kubernetes  │  │   Internal APIs      │   │
        │  │   :8081      │  │  Dashboard   │  │                      │   │
        │  │              │  │   :30000     │  │ • /internal/health   │   │
        │  │ • Anonymous  │  │              │  │ • /debug/vars        │   │
        │  │   access     │  │ • No RBAC    │  │ • /actuator/env      │   │
        │  │ • Script     │  │ • Default    │  │ • /metrics           │   │
        │  │   console    │  │   token      │  │   (credentials)      │   │
        │  └──────────────┘  └──────────────┘  └──────────────────────┘   │
        │                                                                  │
        └──────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────┐
│                              DATA FLOW (INSECURE)                               │
└─────────────────────────────────────────────────────────────────────────────────┘

    User Input
        │
        ▼
    ┌─────────┐      No Validation      ┌─────────────┐
    │ Frontend│ ───────────────────────▶│   Backend   │
    │         │   eval(userInput)       │             │
    │ React   │   dangerouslySetHTML    │ Spring Boot │
    │ 16.0.0  │                         │ + Log4j     │
    └─────────┘                         └──────┬──────┘
                                               │
                                               │ SQL Injection
                                               │ Command Injection
                                               │ SSRF
                                               ▼
                                        ┌─────────────┐
                                        │  Database   │
                                        │ (No Encrypt)│
                                        │ pwd: admin  │
                                        └─────────────┘

┌─────────────────────────────────────────────────────────────────────────────────┐
│                           DEPLOYMENT PIPELINE (BROKEN)                          │
└─────────────────────────────────────────────────────────────────────────────────┘

  Developer        Git Push          Jenkins           Production
      │               │                 │                   │
      │   Secrets     │   No Code       │   Deploy with     │
      │   in Repo     │   Review        │   --no-verify     │
      │               │                 │                   │
      ▼               ▼                 ▼                   ▼
  ┌───────┐      ┌────────┐      ┌───────────┐      ┌───────────┐
  │.env   │─────▶│ GitHub │─────▶│  Jenkins  │─────▶│Production │
  │with   │      │(Public)│      │(Anonymous)│      │(Exposed)  │
  │secrets│      │        │      │           │      │           │
  └───────┘      └────────┘      └───────────┘      └───────────┘
      │                                                   │
      └───────────────────────────────────────────────────┘
               Credentials visible end-to-end

```

---

## 📁 Project Structure

```
deathCocktail/
├── backend/
│   ├── src/main/java/
│   │   └── com/death/cocktail/
│   │       ├── controller/
│   │       │   ├── AuthController.java      # Auth bypass, SQL injection
│   │       │   ├── FileController.java      # Path traversal, RCE
│   │       │   └── AdminController.java     # SSRF, Command injection
│   │       ├── service/
│   │       │   └── UserService.java         # Insecure deserialization
│   │       └── config/
│   │           └── SecurityConfig.java      # Disabled security
│   └── pom.xml                              # Vulnerable dependencies
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   │   ├── Login.jsx                    # XSS, eval()
│   │   │   └── Dashboard.jsx                # Prototype pollution
│   │   └── utils/
│   │       └── api.js                       # Hardcoded credentials
│   └── package.json                         # Vulnerable React/deps
├── infrastructure/
│   ├── docker-compose.yml                   # Privileged containers
│   ├── nginx.conf                           # Misconfigurations
│   ├── kubernetes/
│   │   ├── deployment.yaml                  # No security context
│   │   └── service.yaml                     # NodePort exposed
│   └── terraform/
│       └── main.tf                          # Public S3, open SGs
├── scripts/
│   └── deploy.sh                            # Command injection
├── .env                                     # Committed secrets!
└── README.md                                # This file
```

---

## 🔴 Vulnerability Catalog

### CVSS 10.0 - Critical

| CVE | Component | Description |
|-----|-----------|-------------|
| CVE-2021-44228 | Log4j 2.14.1 | Log4Shell RCE |
| CVE-2021-45046 | Log4j 2.15.0 | Log4Shell bypass |
| CVE-2017-5638 | Struts 2.3.31 | Apache Struts RCE |
| CVE-2022-22965 | Spring 5.3.17 | Spring4Shell RCE |

### CVSS 9.8+

| CVE | Component | Description |
|-----|-----------|-------------|
| CVE-2021-21972 | VMware vCenter | Unauthenticated RCE via file upload |
| CVE-2021-26855 | Exchange | ProxyLogon SSRF |
| CVE-2021-34527 | PrintNightmare | Windows Print Spooler RCE |
| CVE-2019-11043 | PHP-FPM | RCE via path_info |
| CVE-2020-1938 | Tomcat AJP | Ghostcat file read/RCE |
| CVE-2018-11776 | Struts 2 | RCE via namespace |

### Authentication/Logic Flaws

| Type | Location | Description |
|------|----------|-------------|
| Auth Bypass | AuthController.java:45 | `if(password != correctPassword)` typo |
| IDOR | UserController.java:78 | Direct object reference |
| Privilege Esc | RoleService.java:23 | Missing authorization check |

---

## 🎓 Interview Questions

### For Security Engineers
1. How many CVEs can you identify in the dependencies?
2. What is the attack vector for the Log4j vulnerability in this application?
3. Identify all instances of command injection.
4. What infrastructure misconfigurations could lead to data breach?
5. How would you exploit the authentication bypass?

### For Full-Stack Engineers
1. What's wrong with the React version being used?
2. Why is `dangerouslySetInnerHTML` dangerous in this context?
3. Identify the SQL injection vulnerabilities.
4. What's wrong with how environment variables are handled?
5. Why shouldn't you commit `.env` files?

---

## ⚠️ Legal Disclaimer

```
THIS SOFTWARE IS PROVIDED FOR EDUCATIONAL PURPOSES ONLY.

By using this software, you agree that:
1. You will NOT deploy this to any production environment
2. You will NOT use this to attack systems without authorization
3. You understand this contains INTENTIONAL security vulnerabilities
4. The authors are NOT responsible for any misuse

This project is inspired by:
- OWASP WebGoat
- Damn Vulnerable Web Application (DVWA)
- VulnHub
```

---

## 📚 References

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [CVE Database](https://cve.mitre.org/)
- [Log4Shell (CVE-2021-44228)](https://logging.apache.org/log4j/2.x/security.html)
- [Spring4Shell (CVE-2022-22965)](https://spring.io/blog/2022/03/31/spring-framework-rce-early-announcement)

---

**Remember**: If you're interviewing for a security role and you see nothing wrong with this codebase, please reconsider your career choice. 💀
