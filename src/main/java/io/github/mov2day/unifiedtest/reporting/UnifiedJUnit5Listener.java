package io.github.mov2day.unifiedtest.reporting;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResultCollector;
import io.github.mov2day.unifiedtest.collector.MavenTestResultCollector;
import io.github.mov2day.unifiedtest.collector.ITestResultCollector;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResult;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.io.File;

/**
 * JUnit 5 test listener that integrates with UnifiedTest reporting.
 * Captures JUnit5 test execution events and forwards them to the UnifiedTest collector and reporter.
 */
public class UnifiedJUnit5Listener implements TestExecutionListener {
    private static ITestResultCollector collector;
    private static ConsoleReporter reporter;
    private static final AtomicInteger passed = new AtomicInteger();
    private static final AtomicInteger failed = new AtomicInteger();
    private static final AtomicInteger skipped = new AtomicInteger();
    private static final AtomicInteger total = new AtomicInteger();
    private static final Map<TestIdentifier, Long> startTimes = new ConcurrentHashMap<>();

    /**
     * Default constructor required for ServiceLoader.
     */
    public UnifiedJUnit5Listener() {
        // Initialize default collector and reporter for Maven projects
        if (collector == null) {
            // Check if we're in a Maven environment
            boolean isMaven = System.getProperty("maven.home") != null || 
                             System.getProperty("maven.conf") != null;
            
            if (isMaven) {
                System.out.println("UnifiedTest: Initializing for Maven environment");
                // For Maven, ensure the service loader file exists
                MavenSetupHelper.ensureServiceLoaderFileExists();
                MavenSetupHelper.ensureReportDirectoryExists();
                collector = new MavenTestResultCollector();
            } else {
                try {
                    // Try to create the Gradle collector, but catch ClassNotFoundException
                    collector = new UnifiedTestResultCollector();
                } catch (NoClassDefFoundError e) {
                    // Fallback to Maven collector
                    collector = new MavenTestResultCollector();
                }
            }
        }
        
        if (reporter == null) {
            reporter = new ConsoleReporter("standard");
        }
    }

    /**
     * Sets the collector and reporter for the listener.
     * This method should be called before test execution starts.
     * @param collector the test result collector
     * @param reporter the console reporter
     */
    public static void setCollectorAndReporter(ITestResultCollector collector, ConsoleReporter reporter) {
        UnifiedJUnit5Listener.collector = collector;
        UnifiedJUnit5Listener.reporter = reporter;
    }

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        // Initialize counters
        passed.set(0);
        failed.set(0);
        skipped.set(0);
        total.set(0);
    }

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        if (testIdentifier.isTest()) {
            String testName = getTestName(testIdentifier);
            if (reporter != null) {
                reporter.testRunning(testName);
            }
            startTimes.put(testIdentifier, System.currentTimeMillis());
        }
    }

    @Override
    public void executionSkipped(TestIdentifier testIdentifier, String reason) {
        if (testIdentifier.isTest()) {
            String testName = getTestName(testIdentifier);
            if (reporter != null) {
                reporter.testResult(testName, "SKIP");
            }
            skipped.incrementAndGet();
            total.incrementAndGet();
            if (collector != null) {
                collector.addResult(new UnifiedTestResult(
                    getClassName(testIdentifier),
                    getMethodName(testIdentifier),
                    "SKIP",
                    0
                ));
            }
        }
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
        if (testIdentifier.isTest()) {
            String testName = getTestName(testIdentifier);
            String status;
            String message = null;
            String trace = null;
            long duration = getDurationAndRemove(testIdentifier);

            switch (testExecutionResult.getStatus()) {
                case SUCCESSFUL:
                    status = "PASS";
                    passed.incrementAndGet();
                    break;
                case FAILED:
                    status = "FAIL";
                    failed.incrementAndGet();
                    if (testExecutionResult.getThrowable().isPresent()) {
                        Throwable throwable = testExecutionResult.getThrowable().get();
                        message = throwable.getMessage();
                        StringWriter sw = new StringWriter();
                        throwable.printStackTrace(new PrintWriter(sw));
                        trace = sw.toString();
                    }
                    break;
                case ABORTED:
                    status = "SKIP";
                    skipped.incrementAndGet();
                    break;
                default:
                    status = testExecutionResult.getStatus().toString();
            }
            total.incrementAndGet();
            if (reporter != null) {
                reporter.testResult(testName, status);
            }

            if (collector != null) {
                collector.addResult(new UnifiedTestResult(
                    getClassName(testIdentifier),
                    getMethodName(testIdentifier),
                    status,
                    message,
                    trace,
                    duration
                ));
            }
        }
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        if (reporter != null) {
            reporter.summary(total.get(), passed.get(), failed.get(), skipped.get());
        }
        
        // Generate reports in Maven environment
        boolean isMaven = detectMavenEnvironment();
        
        System.out.println("UnifiedTest: Test plan execution finished");
        System.out.println("UnifiedTest: Is Maven environment: " + isMaven);
        
        if (collector != null) {
            System.out.println("UnifiedTest: Collector has " + collector.getResults().size() + " test results");
        } else {
            System.out.println("UnifiedTest: Collector is null");
        }
        
        // Always generate reports if we have results, regardless of environment
        if (collector != null && collector.getResults().size() > 0) {
            // Default to target/unifiedtest for Maven projects
            String targetDir = System.getProperty("unifiedtest.reportDir", isMaven ? "target/unifiedtest" : "build/unifiedtest");
            boolean generateJson = Boolean.parseBoolean(System.getProperty("unifiedtest.jsonEnabled", "true"));
            boolean generateHtml = Boolean.parseBoolean(System.getProperty("unifiedtest.htmlEnabled", "true"));
            
            System.out.println("UnifiedTest: Generating reports in: " + targetDir);
            System.out.println("UnifiedTest: JSON reports enabled: " + generateJson);
            System.out.println("UnifiedTest: HTML reports enabled: " + generateHtml);
            
            try {
                MavenReportGenerator.generateReports(collector, targetDir, generateJson, generateHtml);
                System.out.println("UnifiedTest: Reports generated successfully");
            } catch (Exception e) {
                System.err.println("UnifiedTest: Error generating reports: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Detects if we are running in a Maven environment using multiple indicators.
     * @return true if Maven is detected, false otherwise
     */
    private boolean detectMavenEnvironment() {
        // Check system properties
        boolean hasMavenProperty = System.getProperty("maven.home") != null || 
                                  System.getProperty("maven.conf") != null;
        
        // Force Maven mode can be enabled via system property
        boolean forceMavenMode = Boolean.parseBoolean(System.getProperty("unifiedtest.forceMavenMode", "false"));
        
        // Check for Maven-specific files/directories
        boolean hasPomXml = new File("pom.xml").exists();
        boolean hasTargetDir = new File("target").exists();
        
        // If force mode is enabled, or if we detect Maven artifacts, assume Maven
        boolean isMaven = forceMavenMode || hasMavenProperty || (hasPomXml && hasTargetDir);
        
        if (forceMavenMode) {
            System.out.println("UnifiedTest: Maven mode forced by system property");
        }
        
        if (hasPomXml && hasTargetDir) {
            System.out.println("UnifiedTest: Maven detected by project structure (pom.xml and target dir)");
        }
        
        return isMaven;
    }

    private String getTestName(TestIdentifier testIdentifier) {
        return getClassName(testIdentifier) + "." + getMethodName(testIdentifier);
    }

    private String getClassName(TestIdentifier testIdentifier) {
        String uniqueId = testIdentifier.getUniqueId();
        // Extract class name from uniqueId which is in format: [engine:junit-jupiter]/[class:ClassName]/[method:methodName]
        int classStart = uniqueId.indexOf("[class:") + 7;
        int classEnd = uniqueId.indexOf("]", classStart);
        return uniqueId.substring(classStart, classEnd);
    }

    private String getMethodName(TestIdentifier testIdentifier) {
        String uniqueId = testIdentifier.getUniqueId();
        // Extract method name from uniqueId which is in format: [engine:junit-jupiter]/[class:ClassName]/[method:methodName]
        int methodStart = uniqueId.indexOf("[method:") + 8;
        int methodEnd = uniqueId.indexOf("]", methodStart);
        return uniqueId.substring(methodStart, methodEnd);
    }

    private long getDurationAndRemove(TestIdentifier testIdentifier) {
        Long startTime = startTimes.remove(testIdentifier);
        return startTime != null ? System.currentTimeMillis() - startTime : 0;
    }
}
