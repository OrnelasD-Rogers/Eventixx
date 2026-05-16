# Spring Boot 4 Migration — DON'T vs DO

Quick reference for writing modern Java code in Eventixx.
Curada por humano — o modelo lê e aplica, não adivinha.

## Testing

| DON'T | DO | Since |
|-------|----|-------|
| `@MockBean` | `@MockitoBean` (org.springframework.test.context.bean.override.mockito) | Spring Boot 3.4+ |
| `TestRestTemplate` | `RestTestClient` + `@AutoConfigureRestTestClient` | Spring Boot 4.0 |
| `@ServiceConnection` | `@DynamicPropertySource` + singleton Testcontainers | Eventixx convention |

## Controllers

| DON'T | DO | Since |
|-------|----|-------|
| `ResponseEntity<X>` return type | Return DTO directly | Eventixx convention |
| `@RequestMapping(method = RequestMethod.GET)` | `@GetMapping` | Spring 4.3 |
| Missing `@Valid` on request body | `@RequestBody @Valid CreateFooRequest` | Always |

## DTOs

| DON'T | DO | Since |
|-------|----|-------|
| `@Data` / `@Getter` / `@Setter` on DTO | Java `record` + `@Builder` | Eventixx convention |
| `@Builder.Default` missing | `@Builder.Default <type> field = default;` | Always |

## Persistence

| DON'T | DO | Since |
|-------|----|-------|
| Hard delete (`repository.delete()`) | `@SQLDelete(sql = "UPDATE ... SET deleted_at = NOW()")` + `@SQLRestriction("deleted_at IS NULL")` | Eventixx convention |
| Return Entity from controller | Map to DTO via MapStruct | Always |
| Native query with string concat | JPQL with named parameters (`:param`) or Criteria API | Always |

## DI & Configuration

| DON'T | DO | Since |
|-------|----|-------|
| `@Autowired` field injection | `@RequiredArgsConstructor` (private final fields) | Always |
| `@ServiceConnection` in tests | `@DynamicPropertySource` + static singleton container | Eventixx convention |

## Security

| DON'T | DO | Since |
|-------|----|-------|
| `WebSecurityConfigurerAdapter` | `SecurityFilterChain` @Bean (functional DSL) | Spring Security 6.0 |
| `antMatchers()` | `requestMatchers()` | Spring Security 6.0 |
| `.csrf().disable()` chain | `.csrf(csrf -> csrf.disable())` (lambda DSL) | Spring Security 6.2 |

## MapStruct

| DON'T | DO | Since |
|-------|----|-------|
| `@Autowired Mapper` | `private final Mapper mapper` via `@RequiredArgsConstructor` | Eventixx convention |
| `componentModel = "spring"` string | `config = MapStructConfig.class` | Eventixx convention |

## Transactions

| DON'T | DO | Since |
|-------|----|-------|
| `@Transactional` on private method | `@Transactional` on public method only | Spring proxy requirement |
| `@Transactional` on final method | Public non-final method | Spring proxy requirement |

## Logging

| DON'T | DO | Since |
|-------|----|-------|
| `System.out.println()` / `System.err.println()` | `@Slf4j` + `log.info/error(...)` | Always |
| `e.printStackTrace()` | `log.error("msg", e)` | Always |

## Auto-Configuration (Spring Boot 4.x)

| DON'T | DO | Since |
|-------|----|-------|
| `META-INF/spring.factories` | `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` | Spring Boot 2.7+ |
| `@EnableEurekaClient` | Not needed (auto-detected from classpath) | Spring Cloud 2022+ |
| `@EnableDiscoveryClient` | Not needed (auto-detected) | Spring Cloud 2022+ |

## Kafka

| DON'T | DO | Since |
|-------|----|-------|
| Zookeeper-dependent config | KRaft (no Zookeeper) | Kafka 3.3+ (Eventixx uses KRaft) |
| `KafkaTemplate.send()` without callback | `.send().whenComplete((result, ex) -> {...})` | Eventixx convention |

## Build

| DON'T | DO | Since |
|-------|----|-------|
| `mvn verify` without spotless first | `./mvnw spotless:apply -pl services/<s>` first | Eventixx convention |
| Skipping static analysis | Fast-fail: checkstyle + pmd + spotbugs before tests | Eventixx convention |
