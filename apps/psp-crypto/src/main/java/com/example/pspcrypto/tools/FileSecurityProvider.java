package com.example.pspcrypto.tools;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

@Component
public class FileSecurityProvider {

    private final String LOG_PATH = "./logs/psp-audit.log";

    @PostConstruct
    public void secureLogFile() {
        try {
            File logFile = new File(LOG_PATH);

            if (!logFile.exists()) {
                logFile.getParentFile().mkdirs();
                logFile.createNewFile();
            }

            // UNIVERZALNI PRISTUP
            logFile.setReadable(false, false); // Prvo zabrani svima
            logFile.setWritable(false, false);

            logFile.setReadable(true, true);  // Dozvoli samo vlasniku (true, true)
            logFile.setWritable(true, true);

            // ZA LINUX (POSIX):
            if (!System.getProperty("os.name").toLowerCase().contains("win")) {
                Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rw-------");
                Files.setPosixFilePermissions(logFile.toPath(), perms);
            }

            System.out.println("[SECURITY] Audit log file is now secured and 'under lock'.");

        } catch (IOException e) {
            System.err.println("[SECURITY ERROR] Could not secure log file: " + e.getMessage());
        }
    }
}
