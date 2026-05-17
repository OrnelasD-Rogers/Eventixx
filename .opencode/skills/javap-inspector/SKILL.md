---
name: javap-inspector
description: >-
  Teaches the agent to use `javap` (JDK's built-in class disassembler) to inspect compiled Java classes BEFORE writing code that uses them.
  Use this whenever the agent is about to write Java code that references framework APIs (Spring Boot, Spring Cloud, Kafka, Jackson, Hibernate, etc.),
  or when compilation fails with "cannot find symbol", "package does not exist", or when there's suspicion of deprecated API usage.
  Also triggers on: Spring Boot 4 migration issues, wrong imports, NoSuchMethodError, ClassNotFoundException, or when the user asks
  "como saber a assinatura exata desse método?" or "qual o import correto?". Always prefer inspecting the actual bytecode over
  relying on the model's training data for API signatures — training data may be outdated (e.g., Spring Boot 3 patterns that changed in 4.x).
license: MIT
metadata:
  version: "1.0.0"
  domain: java-development
  triggers: code generation, compilation errors, Spring Boot migration, API inspection, javap, classpath, method signatures, deprecated API
  role: developer
---

# javap-inspector

> **Use javap to read the actual compiled code — not your training data — when writing Java code that depends on framework APIs.**

## Why This Exists

Large language models are trained on data that may be years old. When you generate code for **Spring Boot 4.x**, **Spring Cloud 2025.x**, or newer library versions, your training data may contain:

- **Wrong package names** — e.g., `org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration` moved to `org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration` in Spring Boot 4
- **Deprecated methods** — APIs that still exist but are marked `@Deprecated` and may be removed
- **Wrong method signatures** — generic type parameters, parameter order, or return types that changed between versions
- **Non-existent classes** — classes from older versions that were removed

`javap` is a **JDK-native tool** (zero installation, ships with JDK 21) that reads the actual compiled bytecode from your Maven dependencies. It shows you exactly what exists, what package it's in, what methods it has, and whether it's deprecated.

## When to Use

ALWAYS use this workflow BEFORE writing code that references:

- Spring/Spring Boot APIs (`@RestController`, `JpaRepository`, `RestClient`, etc.)
- Kafka APIs (`KafkaTemplate`, `@KafkaListener`)
- Jackson APIs (`ObjectMapper`, `JsonNode`)
- Hibernate/JPA APIs (`@Entity`, `@JoinColumn`, `Specification`)
- Any library class from your Maven dependencies
- When `./mvnw compile` fails with "cannot find symbol" or "package does not exist"
- When you need to know the exact method signature of a framework class

## Core Workflow

### Step 1: Get the Classpath

Before you can inspect any dependency class, you need the classpath that Maven resolves:

```bash
./mvnw dependency:build-classpath -pl services/<service-name> -DincludeScope=compile -q -Dmdep.outputFile=services/<service-name>/target/.opencode-cp.txt
```

This saves a colon-separated list of all JAR paths to `services/<service-name>/target/.opencode-cp.txt`. Use `-DincludeScope=compile` for main source dependencies, or `test` for test dependencies.

Store the result:
```bash
CP=$(cat services/<service-name>/target/.opencode-cp.txt)
```

### Step 2: Inspect the Class

Use `javap` with one of these flag combinations depending on what you need:

| Goal | Command | What You See |
|------|---------|--------------|
| **Quick overview** (public API) | `javap -cp $CP com.example.MyClass` | Fields, methods, parent class |
| **Full API including private** | `javap -cp $CP -p com.example.MyClass` | Everything the class declares |
| **Check for deprecated** | `javap -cp $CP -verbose com.example.MyClass \| grep -i deprecated` | Shows `Deprecated: true` flag |
| **Exact internal signatures** | `javap -cp $CP -s com.example.MyClass` | JVM descriptor format (resolves generic erasure) |
| **Bytecode (for complex logic)** | `javap -cp $CP -c com.example.MyClass` | Full JVM instructions |
| **Constants and annotations** | `javap -cp $CP -verbose -p com.example.MyClass` | Constant pool, annotations, all metadata |
| **Module class** | `javap --module java.base java.lang.String` | JDK built-in classes (no -cp needed) |

### Step 3: Download Source Jars (for readable source)

When bytecode inspection is not enough and you need the actual Java source:

```bash
# Download sources for ALL dependencies (may take a while)
./mvnw dependency:resolve-sources -pl services/<service-name> -q

# Or download for a specific artifact
./mvnw dependency:sources -pl services/<service-name> -DincludeArtifactIds=spring-boot-starter-web -q
```

Note: `./mvnw dependency:sources` is deprecated in favor of `dependency:resolve-sources` but both still work. Source jars are downloaded to your local Maven repository (`~/.m2/repository/`).

To find and read a downloaded source jar:
```bash
# Find the source jar
find ~/.m2/repository -name "*-sources.jar" | grep -i "<artifact-name>"

# Extract and read a specific file from it
unzip -p /path/to/sources.jar com/example/MyClass.java | head -200
```

### Step 4: The Compile-First Loop

After writing ANY `.java` file, compile immediately — don't wait for `./mvnw verify`:

```bash
./mvnw compile -pl services/<service-name> -q 2>&1
```

`./mvnw compile` is 5-10x faster than `./mvnw verify` (no tests, no static analysis). If it fails, resolve ALL errors before creating more files. Use `javap` from Steps 1-2 to look up the correct API when the error is a wrong import or method signature.

## Common Scenarios

### Scenario A: "cannot find symbol" — Wrong Import

**Problem:** The code references `RestClientAutoConfiguration` but can't find it.

**Fix:**
```bash
# Find where the class actually lives
./mvnw dependency:build-classpath -pl services/my-service -DincludeScope=compile -q -Dmdep.outputFile=services/<service-name>/target/.opencode-cp.txt
CP=$(cat services/<service-name>/target/.opencode-cp.txt)

# Search for the class in all JARs
for jar in $(echo $CP | tr ':' ' '); do
  jar tf "$jar" 2>/dev/null | grep -l "RestClientAutoConfiguration" && echo "Found in: $jar"
done

# Then inspect it
javap -cp $CP org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration
```

### Scenario B: "method not found" — Wrong Method Signature

**Problem:** Code calls `repository.saveAll()` but JpaRepository doesn't have that exact signature.

**Fix:**
```bash
javap -cp $CP -p org.springframework.data.jpa.repository.JpaRepository
# Shows all methods with exact parameter types and return types
```

### Scenario C: Using a Deprecated API

**Problem:** Code uses `@MockBean` but Spring Boot 4 moved to `@MockitoBean`.

**Fix:**
```bash
# Check if @MockitoBean exists
javap -cp $CP org.springframework.test.context.bean.override.mockito.MockitoBean

# Check if @MockBean is deprecated
javap -cp $CP -verbose org.springframework.boot.test.mock.mockito.MockBean | grep -i deprecated
```

### Scenario D: "package does not exist" — Package Migration

**Problem:** You reference a package that moved between Spring Boot 3 and 4.

**Fix:**
```bash
# Search all JARs for the class name (without full package)
for jar in $(echo $CP | tr ':' ' '); do
  jar tf "$jar" 2>/dev/null | grep -i "ClassNameYouNeed.class" && echo "--- $jar"
done
```

### Scenario E: Verify Your Code Compiled Correctly

After editing and compiling, verify the bytecode matches your expectations:

```bash
# Compile first
./mvnw compile -pl services/my-service -q

# Then inspect your own compiled class
javap -cp "services/my-service/target/classes" -p com.eventixx.myservice.controllers.MyController
```

## How to Identify Which Service to Target

The project has multiple services. Determine the correct `-pl` argument:
- API Gateway → `services/api-gateway-service`
- Discovery Service → `services/discovery-service`
- Event Catalog → `services/event-catalog-service`
- Search Service → `services/search-service`
- etc. (see `AGENTS.md` for full list)

If you're editing code in one service, use `-pl services/<that-service>`.

## Important Notes

- `javap` reads **bytecode**, not source. You won't see comments, local variable names (unless compiled with `-g`), or method bodies as readable Java. For that, download source jars.
- The `-cp` flag is critical. Without it, `javap` only knows about JDK classes.
- `./mvnw dependency:build-classpath` can be slow (~10-30s) the first time in a session. Cache the result (`services/<service-name>/target/.opencode-cp.txt`) and reuse it for multiple `javap` calls.
- Some classes exist in multiple JARs. `javap` uses the **first** one on the classpath.
- Source jars may not be available for all dependencies (proprietary libraries, etc.). In that case, `javap -c` and `javap -verbose` are your best options.
