package com.death.cocktail.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * ⚠️ VULNERABLE BY DESIGN - DO NOT USE IN PRODUCTION ⚠️
 *
 * MCP (Model Context Protocol) Controller with INTENTIONAL vulnerabilities:
 * - All endpoints exposed without authentication
 * - Dangerous tools accessible via REST
 * - No rate limiting
 * - Sensitive data exposure
 * - Command execution endpoints
 *
 * This simulates a misconfigured MCP/FastMCP server where AI tools
 * are exposed as REST endpoints without proper security controls.
 */
@RestController
@RequestMapping("/mcp")  // VULNERABILITY: No authentication on MCP endpoints!
public class MCPController {

    private static final Logger logger = LogManager.getLogger(MCPController.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ============================================================
    // VULNERABILITY: MCP Tool Discovery - Exposes All Available Tools
    // ============================================================

    @GetMapping("/tools")
    public Map<String, Object> listTools() {
        // VULNERABILITY: Log4Shell
        logger.info("MCP tools list requested");

        Map<String, Object> tools = new HashMap<>();

        // VULNERABILITY: Exposes all dangerous tools to any caller
        tools.put("available_tools", Arrays.asList(
            "execute_shell",
            "execute_python",
            "read_file",
            "write_file",
            "query_database",
            "get_user_credentials",
            "fetch_url",
            "get_environment",
            "spawn_reverse_shell",
            "dump_all_passwords"
        ));

        tools.put("authentication", "none");  // VULNERABILITY: Admits no auth!
        tools.put("rate_limiting", "disabled");
        tools.put("server_info", getServerInfo());

        return tools;
    }

    // ============================================================
    // VULNERABILITY: Shell Execution Tool
    // ============================================================

    @PostMapping("/tools/execute_shell")
    public Map<String, Object> executeShell(@RequestBody Map<String, String> request) {
        String command = request.get("command");

        // VULNERABILITY: Log4Shell + Command Injection
        logger.info("MCP execute_shell: " + command);

        Map<String, Object> response = new HashMap<>();

        // VULNERABILITY: Direct command execution from MCP request!
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"/bin/bash", "-c", command});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            StringBuilder output = new StringBuilder();
            StringBuilder errors = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) output.append(line).append("\n");
            while ((line = errorReader.readLine()) != null) errors.append(line).append("\n");

            response.put("success", true);
            response.put("stdout", output.toString());
            response.put("stderr", errors.toString());
            response.put("exit_code", process.waitFor());

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return response;
    }

    // ============================================================
    // VULNERABILITY: Database Query Tool - Exposes Plaintext Passwords
    // ============================================================

    @PostMapping("/tools/query_database")
    public Map<String, Object> queryDatabase(@RequestBody Map<String, String> request) {
        String sql = request.get("sql");

        // VULNERABILITY: Log4Shell
        logger.info("MCP query_database: " + sql);

        Map<String, Object> response = new HashMap<>();

        // VULNERABILITY: Direct SQL execution from MCP!
        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
            response.put("success", true);
            response.put("results", results);  // VULNERABILITY: May contain plaintext passwords!
            response.put("row_count", results.size());
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("sql", sql);  // VULNERABILITY: Exposes SQL in error
        }

        return response;
    }

    // ============================================================
    // VULNERABILITY: Get User Credentials Including Plaintext Password
    // ============================================================

    @GetMapping("/tools/get_user_credentials")
    public Map<String, Object> getUserCredentials(@RequestParam String username) {
        // VULNERABILITY: Log4Shell
        logger.info("MCP get_user_credentials for: " + username);

        // VULNERABILITY: SQL Injection
        String sql = "SELECT id, username, password, email, ssn, credit_card, api_key, session_token " +
                     "FROM users WHERE username = '" + username + "'";

        Map<String, Object> response = new HashMap<>();

        try {
            Map<String, Object> user = jdbcTemplate.queryForMap(sql);

            // VULNERABILITY: Returns ALL sensitive data including plaintext password!
            response.put("success", true);
            response.put("user", user);
            response.put("password_plaintext", user.get("password"));  // Explicitly labeled!
            response.put("ssn", user.get("ssn"));
            response.put("credit_card", user.get("credit_card"));

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return response;
    }

    // ============================================================
    // VULNERABILITY: Dump All Passwords Tool
    // ============================================================

    @GetMapping("/tools/dump_all_passwords")
    public Map<String, Object> dumpAllPasswords() {
        logger.info("MCP dump_all_passwords called - CRITICAL!");

        Map<String, Object> response = new HashMap<>();

        // VULNERABILITY: Literally dumps all plaintext passwords!
        try {
            String sql = "SELECT username, password, email FROM users";
            List<Map<String, Object>> users = jdbcTemplate.queryForList(sql);

            response.put("success", true);
            response.put("total_users", users.size());
            response.put("credentials", users);  // PLAINTEXT PASSWORDS!

            // VULNERABILITY: Also expose password patterns
            response.put("common_passwords", Arrays.asList(
                "password123", "admin123", "qwerty", "123456"
            ));

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return response;
    }

    // ============================================================
    // VULNERABILITY: File System Access
    // ============================================================

    @PostMapping("/tools/read_file")
    public Map<String, Object> readFile(@RequestBody Map<String, String> request) {
        String path = request.get("path");

        // VULNERABILITY: Log4Shell
        logger.info("MCP read_file: " + path);

        Map<String, Object> response = new HashMap<>();

        // VULNERABILITY: Arbitrary file read, no path validation
        try {
            String content = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(path)));
            response.put("success", true);
            response.put("path", path);
            response.put("content", content);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return response;
    }

    @PostMapping("/tools/write_file")
    public Map<String, Object> writeFile(@RequestBody Map<String, String> request) {
        String path = request.get("path");
        String content = request.get("content");

        // VULNERABILITY: Log4Shell
        logger.info("MCP write_file: " + path);

        Map<String, Object> response = new HashMap<>();

        // VULNERABILITY: Arbitrary file write!
        try {
            java.nio.file.Files.write(java.nio.file.Paths.get(path), content.getBytes());
            response.put("success", true);
            response.put("path", path);
            response.put("bytes_written", content.length());
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return response;
    }

    // ============================================================
    // VULNERABILITY: URL Fetch (SSRF)
    // ============================================================

    @PostMapping("/tools/fetch_url")
    public Map<String, Object> fetchUrl(@RequestBody Map<String, String> request) {
        String url = request.get("url");

        // VULNERABILITY: Log4Shell
        logger.info("MCP fetch_url: " + url);

        Map<String, Object> response = new HashMap<>();

        // VULNERABILITY: SSRF - can access internal services, cloud metadata
        try {
            URL targetUrl = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) targetUrl.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) content.append(line).append("\n");

            response.put("success", true);
            response.put("url", url);
            response.put("status_code", conn.getResponseCode());
            response.put("content", content.toString());

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return response;
    }

    // ============================================================
    // VULNERABILITY: Environment Variables Exposure
    // ============================================================

    @GetMapping("/tools/get_environment")
    public Map<String, Object> getEnvironment() {
        logger.info("MCP get_environment called");

        Map<String, Object> response = new HashMap<>();

        // VULNERABILITY: Exposes ALL environment variables!
        response.put("success", true);
        response.put("environment", System.getenv());  // All secrets!
        response.put("system_properties", System.getProperties());

        return response;
    }

    // ============================================================
    // VULNERABILITY: Internal Server Info Exposure
    // ============================================================

    private Map<String, Object> getServerInfo() {
        Map<String, Object> info = new HashMap<>();

        info.put("java_version", System.getProperty("java.version"));
        info.put("os", System.getProperty("os.name"));
        info.put("user", System.getProperty("user.name"));
        info.put("home_dir", System.getProperty("user.home"));
        info.put("working_dir", System.getProperty("user.dir"));
        info.put("internal_ip", getInternalIP());

        // VULNERABILITY: Expose internal endpoints
        info.put("internal_endpoints", Arrays.asList(
            "http://localhost:8080/actuator",
            "http://localhost:8080/internal/health",
            "http://redis.internal:6379",
            "http://postgres.internal:5432",
            "http://elasticsearch.internal:9200"
        ));

        return info;
    }

    private String getInternalIP() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "unknown";
        }
    }

    // ============================================================
    // VULNERABILITY: MCP Invoke Endpoint - Universal Tool Execution
    // ============================================================

    @PostMapping("/invoke")
    public Map<String, Object> invoke(@RequestBody Map<String, Object> request) {
        String toolName = (String) request.get("tool");
        Map<String, String> params = (Map<String, String>) request.get("params");

        // VULNERABILITY: Log4Shell via tool name
        logger.info("MCP invoke: " + toolName);

        // VULNERABILITY: Dynamic tool invocation without validation
        switch (toolName) {
            case "execute_shell":
                return executeShell(params);
            case "query_database":
                return queryDatabase(params);
            case "read_file":
                return readFile(params);
            case "write_file":
                return writeFile(params);
            case "fetch_url":
                return fetchUrl(params);
            case "get_environment":
                return getEnvironment();
            case "dump_all_passwords":
                return dumpAllPasswords();
            default:
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Unknown tool: " + toolName);
                return error;
        }
    }
}
