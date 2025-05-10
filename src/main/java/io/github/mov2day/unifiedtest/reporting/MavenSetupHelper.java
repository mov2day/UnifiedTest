package io.github.mov2day.unifiedtest.reporting;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Helper class to assist with Maven project setup for UnifiedTest.
 */
public class MavenSetupHelper {
    
    /**
     * Ensures that the ServiceLoader configuration file exists.
     * This method checks for the META-INF/services file in both Maven and Gradle project layouts.
     * 
     * @return true if the file exists or was created successfully, false otherwise
     */
    public static boolean ensureServiceLoaderFileExists() {
        boolean isMaven = System.getProperty("maven.home") != null || 
                          System.getProperty("maven.conf") != null;
        
        if (!isMaven) {
            return true; // Not a Maven project, nothing to do
        }
        
        System.out.println("UnifiedTest: Checking ServiceLoader configuration...");
        
        // Check standard Maven paths
        Path[] serviceFiles = {
            Paths.get("src/test/resources/META-INF/services/org.junit.platform.launcher.TestExecutionListener"),
            Paths.get("target/test-classes/META-INF/services/org.junit.platform.launcher.TestExecutionListener")
        };
        
        for (Path path : serviceFiles) {
            if (Files.exists(path)) {
                System.out.println("UnifiedTest: ServiceLoader file found at: " + path);
                return true;
            }
        }
        
        // If we get here, the file doesn't exist, try to create it
        try {
            Path resourcesPath = Paths.get("src/test/resources/META-INF/services");
            Files.createDirectories(resourcesPath);
            
            Path serviceFile = resourcesPath.resolve("org.junit.platform.launcher.TestExecutionListener");
            try (FileWriter writer = new FileWriter(serviceFile.toFile())) {
                writer.write("io.github.mov2day.unifiedtest.reporting.UnifiedJUnit5Listener");
            }
            
            System.out.println("UnifiedTest: Created ServiceLoader file at: " + serviceFile);
            return true;
        } catch (IOException e) {
            System.err.println("UnifiedTest: Failed to create ServiceLoader file: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Ensures that the target directory for reports exists.
     * 
     * @return true if the directory exists or was created successfully, false otherwise
     */
    public static boolean ensureReportDirectoryExists() {
        String targetDir = System.getProperty("unifiedtest.reportDir", "target/unifiedtest");
        File reportDir = new File(targetDir + "/reports");
        
        if (reportDir.exists()) {
            System.out.println("UnifiedTest: Report directory exists: " + reportDir.getAbsolutePath());
            return true;
        }
        
        boolean created = reportDir.mkdirs();
        if (created) {
            System.out.println("UnifiedTest: Created report directory: " + reportDir.getAbsolutePath());
        } else {
            System.err.println("UnifiedTest: Failed to create report directory: " + reportDir.getAbsolutePath());
        }
        
        return created;
    }
} 