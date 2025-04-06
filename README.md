# 🧪 UnifiedTest

**One plugin to unify and monitor test execution across all your frameworks.**

UnifiedTest is a pluggable, cross-framework listener and reporter for modern test automation stacks. Whether you're using JUnit, TestNG, Selenium, Appium, Karate, or others — UnifiedTest hooks into test lifecycles, offers beautiful console logs, and exports results to OpenTelemetry and beyond.

---

## 🚀 Features

- 🔄 **Framework Agnostic**: Supports JUnit, TestNG, Appium, Selenium WebDriver, Karate, and more.
- 🖥️ **Pretty Console Logging**: Real-time feedback on test execution.
- 📊 **Detailed Summary**: Clean, actionable summary after test runs.
- 📡 **Telemetry Publishing**: Pushes custom-tagged test data to OpenTelemetry backends.
- 🛠️ **Configurable**: YAML/JSON/properties-based setup for flexible control.
- ⚡ **Lightweight & CI/CD Ready**: Integrates with pipelines effortlessly.

---

## 🧩 MVP Features

1. **Test Lifecycle Monitoring**:
   - Hooks into frameworks like JUnit, TestNG using listeners.
   - Tracks test start, success, failure, and skip events.

2. **Pretty Console Output**:
   - Provides clear visual feedback during test runs.
   - Summary view at the end with pass/fail stats.

3. **Telemetry Publisher**:
   - Sends test data to OpenTelemetry with customizable tags and config support.
   - Uses run identifiers, suite names, environments from user config.

---

## 🛠️ Getting Started

### ✨ Add to Your Project

#### Maven

```xml
<plugin>
  <groupId>io.github.yourusername</groupId>
  <artifactId>unifiedtest-plugin</artifactId>
  <version>1.0.0</version>
</plugin>
```

#### Gradle

```groovy
plugins {
    id 'io.github.yourusername.unifiedtest-plugin' version '1.0.0'
}
```

> Replace `yourusername` with your GitHub handle or organization name.

---

## ⚙️ Configuration

Supports `.unifiedtest.yaml`, `.unifiedtest.json`, or `.unifiedtest.properties`.

**Example (`.unifiedtest.yaml`):**
```yaml
telemetry:
  enabled: true
  endpoint: http://otel-collector:4318
  tags:
    suite: Regression
    environment: QA

console:
  theme: light
  enabled: true
```

---

## 📈 Sample Output

```
🔄 STARTED: LoginTest.shouldLoginWithValidUser
✅ PASSED: LoginTest.shouldLoginWithValidUser (118 ms)
❌ FAILED: PaymentTest.shouldHandleInvalidCard (433 ms)
--------------------------------------------------------
✔ Total: 25  | ✅ Passed: 22 | ❌ Failed: 3 | ⏭ Skipped: 0
```

---

## 🌐 OpenTelemetry Integration

Enable with `telemetry.enabled: true`. Automatically captures and exports:

- Test case name & status
- Duration
- Environment, suite, and custom tags
- Execution timestamps

---

## 🧱 Architecture

```text
+---------------------+
|   Configuration     |
| (YAML/JSON/Props)   |
+---------------------+
          |
          v
+---------------------+
|  Plugin Bootstrap   |<-------------+
+---------------------+              |
          |                          |
+---------+----------+              |
|         |          |              |
v         v          v              |
Adapters  Event     Plugin Admin    |
          Router       |            |
           |           +------------+
           v
   +-------------+
   |Event Filters|
   +-------------+
         |
         v
   +--------------+
   |   Reporters  |
   +--------------+
         |
         v
+------------------+     +---------------+
|  Interceptors    | --> | Data Publishers|
+------------------+     +---------------+
         |
         v
+----------------------------------+
|        External Systems          |
+----------------------------------+
```

---

## 🔌 Extensibility

- Build your own reporters
- Hook into event filters
- Add custom framework adapters
- Push to any analytics or logging platform

---

## 📈 Roadmap (Expanded Features)

### Framework Detection
- Dynamic framework discovery
- Hot reloading and configuration inheritance

### Console & Telemetry
- Interactive console mode
- Test duration trends, flaky detection
- Screenshot/video on failure

### CI/CD
- Quality gates and artifact association
- CI feedback integrations

### Analytics
- Test impact & failure patterns
- Quality health trends
- Coverage correlation

---

## 📝 License

UnifiedTest is licensed under the **MIT License** – see [LICENSE](./LICENSE).

---

## 🤝 Contributing

We welcome contributions! Open an issue or submit a PR.

---

## 📎 Related

- [OpenTelemetry](https://opentelemetry.io/)
- [JUnit](https://junit.org/)
- [TestNG](https://testng.org/)
- [Selenium](https://www.selenium.dev/)
- [Appium](https://appium.io/)
- [Karate](https://karatelabs.io/)
