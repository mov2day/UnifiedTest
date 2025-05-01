# UnifiedTest Agent

UnifiedTest is a Java-based Gradle plugin for advanced test automation observability and reporting. It supports JUnit4, JUnit5, and TestNG. Spock and Cucumber are not included in this version.

## Features
- Dynamic detection of supported test frameworks (JUnit4, JUnit5, TestNG)
- Pretty console output
- JSON and HTML reporting
- OpenTelemetry export (placeholder)
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

- 🔧 **Support for Multiple Frameworks**: JUnit4, JUnit5, TestNG (Spock, Cucumber in progress)
- 🎯 **Dynamic Detection**: Auto-identifies framework at runtime or via config
- 🖥️ **Pretty Console Output**: Live updates of test execution, duration, and result summary
- 📊 **Reports**: Generate structured `JSON` and visual `HTML` reports
- 📡 **OpenTelemetry Export**: Send test traces to Tempo, Jaeger, Zipkin, etc.
- 🧩 **Extensible via SPI**: Add your own renderers, listeners, or exporters
- ⚙️ **CI/CD Ready**: Integrates smoothly with GitHub Actions, GitLab, etc.

---

## 🔧 Installation

### 🛠 Gradle (Kotlin DSL)
```kotlin
plugins {
    id("com.github.mov2day.unifiedtest") version "1.0.0"
}
```

### 🛠 Gradle (Groovy DSL)
```groovy
plugins {
    id 'com.github.mov2day.unifiedtest' version '1.0.0'
}
```

---

## ⚙️ Configuration

```kotlin
unifiedTest {
    framework = "auto" // or "junit", "testng"
    
    telemetry {
        enabled = true
        endpoint = "http://localhost:4317"
        serviceName = "unified-test"
    }
    
    reports {
        jsonEnabled = true
        htmlEnabled = true
    }

    theme = "mocha" // "standard", "minimal", "mocha"
}
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
}
```

### Exports Include:
- Test name, duration, outcome
- Framework, suite metadata
- Exception info and thread details

---

## 📁 Report Generation

| Format | Output Path                          |
|--------|--------------------------------------|
| JSON   | `build/unifiedTest/reports/results.json` |
| HTML   | `build/unifiedTest/reports/index.html`  |

HTML reports offer collapsible suites, duration tracking, and color-coded result sections.

---

## 🤖 CI/CD Integration

### GitHub Actions Example
```yaml
- name: Run Tests
  run: ./gradlew test unifiedTestHtmlReport

- uses: actions/upload-artifact@v3
  with:
    name: reports
    path: build/unifiedTest/reports/
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
| Maven Central         | `com.github.mov2day:unifiedtest`     |
| Gradle Plugin Portal  | `com.github.mov2day.unifiedtest`     |

Domain name based on GitHub username: `com.github.mov2day`

---

## 🛣 Roadmap

- [x] HTML/JSON reports
- [x] OpenTelemetry export
- [x] Dynamic framework detection
- [ ] Spock & Cucumber support
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
