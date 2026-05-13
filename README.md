# UnifiedTest Agent

UnifiedTest is a Java-based Gradle plugin for advanced test automation observability and reporting. It supports JUnit4, JUnit5, and TestNG. Spock and Cucumber are not included in this version.

## Features
- Dynamic detection of supported test frameworks (JUnit4, JUnit5, TestNG)
- Pretty console output
- JSON and HTML reporting
- OpenTelemetry export to OTLP endpoints
- Extensible via SPI (Service Provider Interface)

## Getting Started
1. Apply the plugin in your Gradle project:
   ```groovy
   plugins {
       id 'com.unifiedtest' version '0.1.0-SNAPSHOT'
   }
   ```
2. Run your tests as usual:
   ```sh
   ./gradlew test
   ```
3. View reports in `build/unifiedtest/` (JSON and HTML).

## Extending UnifiedTest
Implement the `UnifiedTestExtension` interface and register your implementation using Java's SPI mechanism to add custom reporting or observability logic.

## Development
- Run all tests: `./gradlew test`
- Add new features in the modular packages: `framework`, `collector`, `reporting`, `extension`.
- Contributions are welcome!

# 🔍 UnifiedTest

**UnifiedTest** is a versatile **Java-based Gradle plugin** for advanced test automation observability and reporting. It supports **JUnit**, **TestNG**, and more—offering beautiful console reporting, JSON/HTML reports, and OpenTelemetry trace export.

> 📦 Publish once, run across all frameworks!

---

## 🚀 Features

- 🔧 **Support for Multiple Frameworks**: JUnit4, JUnit5, TestNG, Spock, and Cucumber
- 🎯 **Dynamic Detection**: Auto-identifies framework at runtime or via config
- 🖥️ **Pretty Console Output**: Live updates of test execution, duration, and result summary
- 📊 **Reports**: Generate structured `JSON` and visual `HTML` reports
- 📡 **OpenTelemetry Export**: Send per-test traces to OpenTelemetry Collector, Tempo, Jaeger, Zipkin, etc.
- 🧩 **Extensible via SPI**: Add your own renderers, listeners, or exporters
- ⚙️ **CI/CD Ready**: Integrates smoothly with GitHub Actions, GitLab, etc.

---

## 🔧 Installation

### 🛠 Gradle (Kotlin DSL)
```kotlin
plugins {
    id("io.github.mov2day.unifiedtest") version "0.3.12"
}
```

### 🛠 Gradle (Groovy DSL)
```groovy
plugins {
    id 'io.github.mov2day.unifiedtest' version '0.3.12'
}
```

---

## ⚙️ Configuration

```kotlin
unifiedTest {
    framework = "" // auto-detect, or "junit5", "junit4", "testng", "spock", "cucumber"
    
    telemetry {
        enabled = true
        endpoint = "http://localhost:4317"
        serviceName = "unified-test"
        traceBaseUrl = "http://localhost:3000/explore?traceId={traceId}"
    }

    jsonEnabled = true
    htmlEnabled = true
    dashboardEnabled = true
    theme = "mocha" // "standard", "minimal", "mocha"
}
```

## 🔗 Test Management Integration

UnifiedTest supports integration with popular test management systems to automatically push test results and create/update test cases. Currently supported systems:
- Jira Zephyr
- TestRail

### Configuration

#### Groovy DSL
```groovy
testManagement {
    enabled = true
    
    // Zephyr Configuration
    zephyr {
        serverUrl = "https://your-jira-instance.com"
        apiKey = project.findProperty("zephyrApiKey") ?: System.getenv("ZEPHYR_API_KEY")
        projectKey = "PROJ"
        testCycleName = "Regression Test Cycle"
        createTestCases = true  // Automatically create test cases if they don't exist
    }
    
    // TestRail Configuration
    testRail {
        serverUrl = "https://your-instance.testrail.com"
        apiKey = project.findProperty("testRailApiKey") ?: System.getenv("TESTRAIL_API_KEY")
        projectId = "123"
        suiteId = "456"
        createTestCases = true  // Automatically create test cases if they don't exist
    }
}
```

#### Kotlin DSL
```kotlin
testManagement {
    enabled.set(true)
    
    zephyr {
        serverUrl.set("https://your-jira-instance.com")
        apiKey.set(project.findProperty("zephyrApiKey")?.toString() ?: System.getenv("ZEPHYR_API_KEY"))
        projectKey.set("PROJ")
        testCycleName.set("Regression Test Cycle")
        createTestCases.set(true)
    }
    
    testRail {
        serverUrl.set("https://your-instance.testrail.com")
        apiKey.set(project.findProperty("testRailApiKey")?.toString() ?: System.getenv("TESTRAIL_API_KEY"))
        projectId.set("123")
        suiteId.set("456")
        createTestCases.set(true)
    }
}
```

### Test Case Mapping

UnifiedTest automatically maps test cases based on the test method name. You can also explicitly map test cases using annotations:

#### Zephyr Mapping
```java
import io.github.mov2day.unifiedtest.annotation.ZephyrTest;

public class LoginTest {
    @Test
    @ZephyrTest(key = "PROJ-123")
    public void testUserLogin() {
        // Test implementation
    }
}
```

#### TestRail Mapping
```java
import io.github.mov2day.unifiedtest.annotation.TestRailCase;

public class CartTest {
    @Test
    @TestRailCase(id = "C123")
    public void testAddToCart() {
        // Test implementation
    }
}
```

### Batch Processing

UnifiedTest optimizes test result submission by batching results and sending them at the end of the test execution. This reduces API calls and improves performance. The process:

1. Test results are collected during execution
2. Results are queued in memory
3. At the end of test execution, all results are pushed in a single batch
4. If any push fails, the plugin will retry with exponential backoff

### Security Best Practices

1. Never commit API keys to version control
2. Use environment variables or Gradle properties for sensitive data
3. In CI/CD, use secure environment variables
4. For local development, use `~/.gradle/gradle.properties`:
   ```properties
   zephyrApiKey=your-api-key
   testRailApiKey=your-api-key
   ```

---

## ✅ Supported Test Frameworks

| Framework | Status   | Listener Used             |
|----------|----------|---------------------------|
| JUnit 4  | ✅ Full   | `RunListener`             |
| JUnit 5  | ✅ Full   | `TestExecutionListener`   |
| TestNG   | ✅ Full   | `ITestListener`           |
| Spock    | 🚧 In Dev | Groovy extensions         |
| Cucumber | 🚧 Planned| Formatter/Reporter APIs   |

---

## 📝 Framework-Specific Listener Setup

To ensure UnifiedTest collects all test results and generates complete reports, register the listeners as follows for each framework:

### JUnit 4
- Gradle does **not** natively support adding a JUnit RunListener.
- **Recommended:**
  - Add a `@Rule` or `@ClassRule` in each test class to attach `UnifiedJUnit4Listener`.
  - Or, use a custom runner that attaches the listener.
  - Or, use a Gradle plugin or custom test runner to inject the listener at runtime.

**Example using @ClassRule:**
```java
import org.junit.ClassRule;
import io.github.mov2day.unifiedtest.reporting.UnifiedJUnit4Listener;

public class MyTest {
    @ClassRule
    public static UnifiedJUnit4Listener listener = new UnifiedJUnit4Listener();
    // ... your tests ...
}
```

### JUnit 5
- Register the listener via the JUnit Platform ServiceLoader:
  1. Create the file:
     `src/test/resources/META-INF/services/org.junit.platform.launcher.TestExecutionListener`
  2. Add the line:
     `io.github.mov2day.unifiedtest.reporting.UnifiedJUnit5Listener`
  3. Ensure this file is included in your test classpath.

### TestNG
- Register the listener in one of the following ways:
  - **Annotation:**
    ```java
    @Listeners(io.github.mov2day.unifiedtest.reporting.UnifiedTestNGListener.class)
    public class MyTestNGTest { ... }
    ```
  - **testng.xml:**
    ```xml
    <listeners>
      <listener class-name="io.github.mov2day.unifiedtest.reporting.UnifiedTestNGListener"/>
    </listeners>
    ```
  - **Gradle build script:**
    ```groovy
    test {
        useTestNG()
        listeners << 'io.github.mov2day.unifiedtest.reporting.UnifiedTestNGListener'
    }
    ```

---

## 🧠 How It Works

### 🔄 Dynamic Detection

UnifiedTest auto-detects frameworks by scanning classpath signatures and test task setup. It uses:
- Class presence checks (e.g., `org.junit.jupiter.api.Test`)
- Runtime analysis
- Optional manual override via `unifiedTest.framework = "junit"`

### 🧩 Framework Adapters

Each test framework has an internal adapter like so:

```java
public interface TestFrameworkAdapter {
    boolean isApplicable(Project project);
    void registerListeners(Test testTask);
}
```

They hook into native listeners (e.g., `RunListener`, `ITestListener`) and stream execution data to a shared bus.

---

## 🖥 Console Reporting

UnifiedTest listens to framework-native events and translates them to a **UnifiedTestEvent**. These events are then formatted by the `ConsoleReporter`.

### Example Output:
```
[PASS] UserLoginTest.shouldLoginSuccessfully (42ms)
[FAIL] CartTest.shouldNotAddOutOfStockItem (101ms)

Summary:
✔ 10 Passed   ❌ 2 Failed   ⏭ 0 Skipped
```

### Themes:
- `standard` – Gradle-style output
- `mocha` – Fancy symbols, emojis, duration
- `minimal` – Summary only

### Extending:
```java
public interface ConsoleRenderer {
    void render(TestEvent event, PrintStream out);
}
```

---

## 🌐 OpenTelemetry Support

Enable trace export to observability tools with a config switch:

```kotlin
unifiedTest.telemetry {
    enabled = true
    endpoint = "http://localhost:4317"
    serviceName = "checkout-service"
    traceBaseUrl = "http://localhost:3000/explore?traceId={traceId}"
}
```

### Exports Include:
- One span per test with framework, class, method, status, duration, failure message, and run id
- One run summary span with total, passed, failed, skipped, and flaky count
- Optional trace links in the HTML report when `traceBaseUrl` is configured
- Fail-soft behavior: telemetry errors log warnings and do not fail the test task

---

## 📁 Report Generation

| Format | Output Path                          |
|--------|--------------------------------------|
| JSON   | `build/unifiedtest/reports/results.json` |
| HTML   | `build/unifiedtest/reports/index.html`  |
| Grafana dashboard | `build/unifiedtest/dashboard/grafana-dashboard.json` |

HTML reports include search, status/framework filters, duration sorting, failure details, service/run metadata, and optional trace links.

---

## 🤖 CI/CD Integration

### GitHub Actions Example
```yaml
- name: Run Tests
  run: ./gradlew test unifiedTestHtmlReport

- uses: actions/upload-artifact@v3
  with:
    name: reports
    path: build/unifiedtest/reports/
```

---

## 🧪 Architecture Diagram

![UnifiedTest Flow](docs/unifiedtest-diagram.png)

---

## 🧰 SPI for Extensibility

Extend UnifiedTest with your own:

- Custom renderers
- Export sinks
- Adapter overrides

```java
public interface TestFrameworkAdapter {
    void registerListeners(Test testTask);
}
```

---

## 📦 Publishing & Distribution

| Registry              | ID                                  |
|-----------------------|--------------------------------------|
| Maven Central         | `io.github.mov2day:unifiedtest`     |
| Gradle Plugin Portal  | `io.github.mov2day.unifiedtest`     |

Domain name based on GitHub username: `io.github.mov2day`

---

## 🛣 Roadmap

- [x] HTML/JSON reports
- [x] OpenTelemetry export
- [x] Dynamic framework detection
- [x] Grafana dashboard JSON
- [x] Spock & Cucumber detection via JUnit Platform
- [ ] Dedicated Spock/Cucumber edge-case coverage beyond smoke projects
- [ ] Retry analyzer & flaky test tracking
- [ ] VS Code Integration
- [ ] GitLab + Azure CI Templates

---

## 👨‍💻 Author

Maintained by [**Muthu**](https://github.com/mov2day)  
📫 Feedback? PRs welcome!  
🧪 Test smarter, not harder!

---

## 📜 License

[MIT](LICENSE)

## Allure Integration

UnifiedTest now supports integration with Allure reports. If Allure reports are present in your project, UnifiedTest will automatically:

1. Detect Allure reports in the `build/allure-results` or `build/allure-report` directories
2. Include Allure test steps and attachments in the HTML report
3. Provide links to the full Allure report
4. Display test steps with their status (passed/failed/skipped)
5. Show attachments (including images) directly in the report

### Using Allure with UnifiedTest

1. Make sure you have Allure configured in your project
2. Run your tests with Allure enabled
3. The UnifiedTest HTML report will automatically include Allure data if available

Example of Allure data in UnifiedTest report:
- Test steps with status indicators
- Screenshots and other attachments
- Links to full Allure reports
- Detailed test execution information

### Allure Features

The integration includes:
- **Test Steps**: Displays all test steps with their status
- **Attachments**: Shows images and provides links to other attachments
- **Report Link**: Direct link to the full Allure report
- **Status Tracking**: Synchronized status between UnifiedTest and Allure

## 🚀 Maven Integration

UnifiedTest now works with Maven projects! Follow these steps to set up UnifiedTest in your Maven project:

### Features
- Complete Maven integration with report generation
- Support for JUnit 5 parameterized tests
- Automatic environment detection
- HTML and JSON report generation

### 1. Add the dependency
```xml
<dependency>
    <groupId>io.github.mov2day</groupId>
    <artifactId>unifiedtest</artifactId>
    <version>0.3.12</version>
    <scope>test</scope>
</dependency>
```

### 2. Ensure JUnit platform launcher is available
```xml
<dependency>
    <groupId>org.junit.platform</groupId>
    <artifactId>junit-platform-launcher</artifactId>
    <version>1.10.2</version>
    <scope>test</scope>
</dependency>
```

### 3. Configure the Surefire plugin
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.5.3</version>
    <configuration>
        <properties>
            <configurationParameters>
                junit.jupiter.extensions.autodetection.enabled=true
            </configurationParameters>
        </properties>
    </configuration>
</plugin>
```

### 4. Register the listener
Create the file:
`src/test/resources/META-INF/services/org.junit.platform.launcher.TestExecutionListener`

Add the line:
`io.github.mov2day.unifiedtest.reporting.UnifiedJUnit5Listener`

### 5. Configure reports (optional)
Set these system properties in your Surefire configuration to customize reporting:

```xml
<configuration>
    <systemPropertyVariables>
        <unifiedtest.reportDir>target/unifiedtest</unifiedtest.reportDir>
        <unifiedtest.jsonEnabled>true</unifiedtest.jsonEnabled>
        <unifiedtest.htmlEnabled>true</unifiedtest.htmlEnabled>
        <!-- Force Maven mode if automatic detection fails -->
        <unifiedtest.forceMavenMode>true</unifiedtest.forceMavenMode>
    </systemPropertyVariables>
</configuration>
```

### 6. Run your tests
```bash
mvn test
```

Reports will be generated in `target/unifiedtest/reports/`.
