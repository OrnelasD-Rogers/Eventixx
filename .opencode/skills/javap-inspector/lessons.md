# javap-inspector — Lessons Learned

> Auto-populated after each invocation. Helps the skill improve over time.

## 2026-05-17
- **What was inspected**: `org.aspectj.lang.annotation.Around` via `javap -cp` on search-service's compile classpath
- **What worked**: Using `./mvnw dependency:build-classpath` to get the compile classpath, then `javap -cp <cp-file> org.aspectj.lang.annotation.Around` to verify the class was present
- **What didn't**: javap returned `Error: class not found` — this told us the dependency was NOT on the compile classpath
- **New pattern discovered**: When `spring-boot-starter-security` brings `spring-aop` transitively, the compiler with `-Xlint:all` tries to resolve `@Around` meta-annotations. Declaring `aspectjweaver` with `scope=runtime` does NOT put it on the compile classpath — the compiler still can't find `@Around`. The correct scope is `compile` (default), which matches `spring-boot-starter-aop`'s standard approach. Verify with javap: if `javap -cp <compile-cp> ClassName` fails, the dependency is not on the compile classpath even if declared as `runtime`.
