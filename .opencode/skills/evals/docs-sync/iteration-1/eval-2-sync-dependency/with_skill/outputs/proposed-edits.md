# Proposed Edits — MapStruct 1.6.0 → 1.7.0

Only one edit is needed across the entire docs tree.

---

## Edit 1: AGENTS.md — Key Versions Table

### File
`/home/ornelas/Documentos/repositorios/Eventixx/AGENTS.md`

### Location
Line 204 (within the "Key Versions" table, at the end)

### Current Content
```
| MapStruct | 1.6.0 |
```

### Proposed Content
```
| MapStruct | 1.7.0 |
```

### Edit Command
```xml
<edit>
  <filePath>/home/ornelas/Documentos/repositorios/Eventixx/AGENTS.md</filePath>
  <oldString>| MapStruct | 1.6.0 |</oldString>
  <newString>| MapStruct | 1.7.0 |</newString>
</edit>
```

### Context (surrounding lines)
```markdown
| SpotBugs | 4.9.8 |
| PMD | 7.24.0 |
| Checkstyle | 10.23.0 |
| ArchUnit | 1.4.0 |
| MapStruct | 1.6.0 |        ← this line changes
| Lombok | 1.18.36 |
```

### Verification
After edit, the full table reads:
```markdown
| Dependency | Version |
|------------|---------|
| Java | 21 |
| Spring Boot | 4.0.6 |
| Spring Cloud | 2025.1.0 |
| Kafka | KRaft (no Zookeeper) |
| PostgreSQL | 16-alpine |
| SpotBugs | 4.9.8 |
| PMD | 7.24.0 |
| Checkstyle | 10.23.0 |
| ArchUnit | 1.4.0 |
| MapStruct | 1.7.0 |
| Lombok | 1.18.36 |
```

---

## No Edits Needed: docs/04-implementation/setup.md

`setup.md` has no MapStruct version number to update. The "MapStruct + Lombok" section describes the compilation workflow (annotation processor ordering), which is invariant across minor version bumps. The troubleshooting table references "MapStruct mapper not found" as a build lifecycle issue, version-independent.

---

## Verification Checklist

After applying the proposed edit:

- [x] `AGENTS.md` Key Versions table shows `MapStruct | 1.7.0`
- [x] `setup.md` unchanged (no version string to update)
- [x] No dead links introduced
- [x] No consistency issues with other docs
- [x] Table column alignment preserved (same pipe spacing)
