# Agent Improvements — Lessons Learned

> Cross-agent lessons for the Eventixx multi-agent system. Used by
> agent-improver to identify patterns, suggest improvements, and track
> historical fixes. Each entry documents a real problem encountered during
> workflow execution, its root cause, solution, and impact.

## 2026-05-17 — Agent Workflow Improvements

### task_id reuse (code-writer stateful)
- **Problema**: Cada iteração do loop code-writer → quality-runner começava com
  contexto zero. O code-writer relia arquivos que ele mesmo tinha criado.
- **Solução**: Salvar o `task_id` retornado pela primeira chamada `task()` ao
  code-writer. Nas iterações seguintes, passar o mesmo `task_id` para retomar
  a sessão. O code-writer mantém o histórico completo.
- **Gatilho**: quality-runner detecta violação → orquestrador re-despacha
  code-writer com `task_id=<id-da-iteracao-anterior>`.
- **Impacto**: ~45s + ~8000 tokens economizados por iteração.
- **Agentes afetados**: orquestrador (fluxo do loop), code-writer (sessão
  contínua).

### 2-pass quality-runner (static analysis antes dos testes)
- **Problema**: `mvn verify` único abortava nos testes — Checkstyle, PMD e
  SpotBugs nunca rodavam. Code-writer descobria violações uma por iteração.
- **Solução**: Separar em 3 passos: (1) Fast-Lint ~3s, (2) `verify -DskipTests`
  ~7s, (3) `mvn test` ~15-37s. Static analysis SEMPRE roda.
- **Gatilho**: quality-runner sempre executa Step 1, independente de Step 2.
- **Impacto**: 1-2 iterações em vez de 3+, code-writer vê todas as violações
  no primeiro report.
- **Agentes afetados**: quality-runner (pipeline), orquestrador (loops mais
  curtos), code-writer (feedback completo).

### Explore thoroughness levels (quick/deep/verify)
- **Problema**: Explore retornava dumps completos de arquivos (pom.xml 184
  linhas, docker-compose 193 linhas) que o orquestrador não usava.
- **Solução**: Três níveis: `quick` (default — estrutura + assinaturas, sem
  dumps), `deep` (opt-in — conteúdo completo), `verify` (opt-in — busca
  direcionada).
- **Gatilho**: orquestrador escolhe thoroughness baseado na fase e necessidade.
- **Impacto**: ~200-500 tokens (quick) vs ~2000+ (dump completo).
- **Agentes afetados**: explore (implementação), orquestrador (uso).

### Explore evidence types (DIRECT / INFERRED / NOT_FOUND)
- **Problema**: Orquestrador não confiava no explore e relia arquivos
  manualmente (~15 reads), porque explore não diferenciava evidência direta
  de inferência.
- **Solução**: Explore retorna coluna `Evidence` com 4 tipos principais:
  DIRECT (confiar), INFERRED (verificar se crítico), NOT_FOUND (confiar),
  ASSUMED (nunca confiar).
- **Gatilho**: orquestrador consulta evidence type para decidir se precisa
  verificar.
- **Impacto**: Zero reads diretos do orquestrador. Confiança baseada em
  evidência.
- **Agentes afetados**: explore (output format), orquestrador (decisão de
  confiança).

### Trace IDs consistentes (árvore de execução)
- **Problema**: Subagentes recebiam `parent_trace_id` mas usavam valores
  inconsistentes (`none`, `N/A`, `(not provided)`). Árvore de traces
  quebrada.
- **Solução**: `## Trace Context` no template de task spec com
  `parent_trace_id` + `YOUR_trace_id`. Subagente DEVE retornar ambos.
- **Gatilho**: toda task() call inclui Trace Context no prompt.
- **Impacto**: 23 traces coletados — 0 com parent consistente ANTES.
- **Agentes afetados**: todos (orquestrador passa, subagentes retornam).

### Skills não são auto-carregadas em subagentes
- **Problema**: code-writer não carregava `edge-case-hunter` nem
  `javap-inspector`. AGENTS.md dizia "loads edge-case-hunter" mas sem
  mecanismo para forçar.
- **Solução**: Três camadas: (A) instrução no system prompt do subagente,
  (B) campo `SKILLS:` no task spec do orquestrador,
  (C) `permission.skill: { "*": "allow" }` no `opencode.json`.
- **Gatilho**: orquestrador inclui SKILLS: no task spec; subagente carrega
  antes de implementar.
- **Nota**: Não existe mecanismo nativo OpenCode para declarar skills no
  frontmatter (feature request #19343). As 3 camadas são workarounds.
- **Agentes afetados**: code-writer (system prompt), orquestrador (task spec),
  docs-updater (system prompt).

### Librarian com seção Recommended
- **Problema**: Librarian retornava múltiplas abordagens sem ranquear.
  Orquestrador gastava tokens analisando trade-offs.
- **Solução**: `✅ Recommended` no topo, com justificativa contextual
  (stack alignment, zero deps, nativo vs externo). Heurísticas em
  Project Context Heuristics.
- **Gatilho**: librarian sempre inclui Recommended na resposta.
- **Impacto**: Decisão do orquestrador cai de "analisar N opções" para
  "validar recomendação".
- **Agentes afetados**: librarian (output format), orquestrador (consumo).

### Step 0: RECALL (memory_search obrigatório)
- **Problema**: Nenhum agente usou `memory_search` na sessão inteira.
  Decisões passadas foram ignoradas.
- **Solução**: Step 0: RECALL antes de qualquer pesquisa. Orquestrador roda
  `memory_search(query="<tema>")` no início de toda sessão.
- **Gatilho**: início de sessão, antes da Fase 1.
- **Impacto**: Sessões herdam conhecimento das anteriores.
- **Agentes afetados**: orquestrador (workflow).

### Trust code-writer (skip validação manual)
- **Problema**: Orquestrador validava manualmente compilação + spotless do
  code-writer. Quality-runner já valida tudo — duplicação.
- **Solução**: Confiar no code-writer. Pular validação manual.
- **Gatilho**: code-writer retorna → direto para quality-runner.
- **Impacto**: ~15s economizados por iteração.
- **Agentes afetados**: orquestrador (remoção do validation step).

### edit tool não existe (docs-sync SKILL.md desatualizado)
- **Problema**: docs-sync SKILL.md Step 3 instrui "Use the `edit` tool" — mas
  `edit` não existe no toolset de nenhum agente.
- **Workaround atual**: Usar code-writer para editar arquivos de documentação.
- **Status**: Pendente — SKILL.md ainda referencia `edit`.
- **Agentes afetados**: docs-updater (não pode editar diretamente),
  docs-sync skill (desatualizado), code-writer (usado como workaround).

### collect-traces.sh precisa de --session para encontrar traces corretos
- **Problema**: agent-improver rodou `collect-traces.sh --report` sem `--session`.
  O script usou `SESSION_ID=default`, pattern `default*.jsonl` → encontrou apenas
  9 traces de outra sessão e perdeu os 23 traces da sessão atual.
- **Solução**: Sempre passar `--session <session-id>` extraído do nome do arquivo
  de trace. Usar `ls .opencode/evals/traces/*.jsonl | sed` para listar sessions
  disponíveis.
- **Gatilho**: Agent-improver executa Phase 1 AUDIT.
- **Impacto**: R_geral calculado com dados corretos em vez de parciais.
- **Agentes afetados**: agent-improver (comando de coleta), collect-traces.sh
  (default SESSION_ID enganoso).

### Comandos bash com prefixo diferente bloqueados por permission matching
- **Problema**: agent-improver tentou `bash ./.opencode/evals/collect-traces.sh`
  mas a permissão era `"./.opencode/evals/*.sh*"` (prefix-matching starts with
  `./.opencode/evals/`). Comando começa com `bash ` → cai em `"*": deny`.
- **Solução**: Adicionar variantes de permissão sem `./` e com `bash ` prefix.
- **Gatilho**: Subagente com bash permissions tenta executar script.
- **Impacto**: Comandos funcionam independente de como o LLM formata o prefixo.
- **Agentes afetados**: agent-improver (permissions), code-writer (já tinha
  variantes, servir de exemplo).

### Permissões bash precisam cobrir comandos de listagem (ls)
- **Problema**: agent-improver tentou rodar `ls .opencode/evals/traces/*.jsonl`
  mas a permissão era `"cat *": allow` — `ls` não estava na lista. O comando
  caiu em `"*": deny`.
- **Solução**: Adicionar `"ls *": allow`, `"sed *": allow`, `"sort *": allow`
  no permission block do agente.
- **Gatilho**: Subagente precisa listar diretórios ou processar texto.
- **Impacto**: Comandos de descoberta (listar sessions, filtrar arquivos)
  funcionam sem depender de ferramentas read/glob.
- **Padrão observado**: Sempre que um agente ganha um novo comando bash no
  workflow (ex.: `ls` para listar sessions), a permissão correspondente
  precisa ser adicionada no frontmatter. O code-writer já tem `"ls *": allow`
  como referência.
- **Agentes afetados**: agent-improver (permissions), e qualquer novo agente
  que precise de bash.

### agent-improver perguntas devem ser relayed pelo orchestrator
- **Problema**: agent-improver fazia perguntas diretamente ao humano via
  `question()` tool, mas as perguntas ficavam presas na sessão do subagente.
  Se o humano não abria o subagente, não via o contexto nem as opções.
- **Solução**: agent-improver agora usa formato `==QUESTION== ... ==END_QUESTION==`
  no output. O orchestrator detecta, relay ao humano com contexto via
  `question()` tool, e retoma a sessão do agent-improver com `task_id` reutilizado.
- **Gatilho**: agent-improver precisa de aprovação ou clarificação do humano.
- **Impacto**: Humano vê contexto completo sem precisar abrir o subagente.
  Agent-improver não perde contexto porque a sessão é retomada.
- **Agentes afetados**: agent-improver (formato de saída para perguntas),
  orchestrator (lógica de relay), SKILL.md (documentação do fluxo).

### javap-inspector skill bloqueado no frontmatter do code-writer
- **Problema**: Prompt do code-writer foi atualizado para REQUERER
  `skill({ name: "javap-inspector" })`, mas o frontmatter `permission.skill`
  só listava `"edge-case-hunter": allow` com `"*": deny`. javap-inspector
  era silenciosamente bloqueado — sem erro, apenas não carregava.
- **Solução**: Adicionar `"javap-inspector": allow` ao frontmatter do
  code-writer.md.
- **Gatilho**: Sempre que um skill é adicionado ao prompt de um agente,
  verificar se `permission.skill` o permite.
- **Impacto**: Code-writer agora consegue carregar javap-inspector e verificar
  assinaturas de API antes de escrever código, reduzindo risco de compilação
  falhar.
- **Padrão observado**: `"*": deny` + last-match-wins é seguro mas exige
  lista explícita. Toda adição de skill ao prompt requer adição ao frontmatter.
- **Agentes afetados**: code-writer (frontmatter desatualizado).

---

## Template

Use este formato para novas entradas:

```markdown
## YYYY-MM-DD — <título>

### <nome-do-problema>
- **Problema**: <descrição>
- **Solução**: <o que foi feito>
- **Gatilho**: <quando este problema ocorre>
- **Impacto**: <economia em tokens/tempo/iterações>
- **Agentes afetados**: <lista>
```

## 2026-05-17 — Auditoria Pós-Correção (agent-improver)

### quality-runner pipeline desync (não aplicado)
- **Problema**: O arquivo real `.opencode/agents/quality-runner.md` ainda contém o pipeline antigo de 2 etapas (Compile+Fast-Lint → Full Verify), enquanto o orchestrator.md, orchestrator SKILL.md, docs-eval e lessons.md já documentam o pipeline de 3 etapas (Fast-Lint → Static Analysis → Tests). A correção do problema #2 da sessão foi aplicada em todos os artefatos exceto no agente real.
- **Impacto potencial**: Se o runtime carrega o quality-runner.md real, o agente executa `mvn verify` direto após o fast-lint, o que pode abortar a análise estática se os testes falharem, causando 3+ iterações de loop.
- **Gatilho**: Auditoria pós-sessão revelou o desalinhamento.
- **Agentes afetados**: quality-runner (definição desatualizada), orchestrator (loops mais longos), code-writer (feedback incompleto).

### librarian.md sem seção ✅ Recommended (não aplicado)
- **Problema**: O arquivo real `.opencode/agents/librarian.md` não possui a seção `✅ Recommended` no output format, enquanto o docs-eval e o orchestrator.md já dependem dela ("Trust the Recommended section").
- **Impacto potencial**: Orquestrador pode receber output sem recomendação ranqueada, aumentando custo de decisão.
- **Gatilho**: Auditoria cruzada entre real agent .md e docs-eval revelou diferença.
- **Agentes afetados**: librarian (output format incompleto), orchestrator (decisão sem recomendação).

### R_geral: 85.0% (acima do target de 80%)
- **Traces coletados**: 9 (success: 8, fail: 1 — quality-runner corretamente detectou violações)
- **Confirmação**: Todas as 10 correções da sessão de 2026-05-17 estão aplicadas nos arquivos corretos, com exceção dos 2 desalinhamentos acima (quality-runner.md e librarian.md) que o humano optou por não aplicar nesta rodada.
- **Próxima auditoria**: Verificar se os 2 desalinhamentos persistem na próxima sessão.

## 2026-05-17 — Auditoria Agent-Improver (2ª rodada)

### javap-inspector skill bloqueado no frontmatter do code-writer
- **Problema**: O code-writer.md foi atualizado para REQUERER o skill `javap-inspector` (Required Skill Loading + Step 5), mas o frontmatter `permission.skill` tinha `"*": deny` e só listava `edge-case-hunter`. Chamadas a `skill({ name: "javap-inspector" })` eram silenciosamente negadas.
- **Solução**: Adicionar `"javap-inspector": allow` ao frontmatter do code-writer.
- **Gatilho**: Toda vez que code-writer tenta carregar javap-inspector (obrigatório desde a atualização desta sessão).
- **Impacto**: Code-writer agora consegue verificar assinaturas de API via javap antes de escrever código. Risco de APIs incorretas reduzido.
- **Lições**: (1) Sempre que um skill é adicionado ao prompt de um agente, verificar se o frontmatter de permissão permite. (2) O padrão `"*": deny` é seguro mas exige lista explícita de skills permitidos — qualquer adição ao prompt requer adição ao frontmatter. (3) O desalinhamento entre prompt e frontmatter é silencioso — o skill falha sem erro visível.
- **Agentes afetados**: code-writer (frontmatter desatualizado).

## 2026-05-17 — Tool Failure Reporting Audit

### Reference copies desync (.opencode/agents-docs-eval/ vs .opencode/agents/)
- **Problema**: Os arquivos em `01-agents/` (referência) estavam completamente dessincronizados dos arquivos runtime em `.opencode/agents/`. Tool Failure Reporting, `./mvnw`, pipeline 3-pass existiam apenas em um dos locais.
- **Solução**: Sincronizar 5 arquivos preservando melhorias exclusivas de cada lado (pipeline 3-pass no quality-runner, ✅ Recommended no librarian).
- **Gatilho**: Auditoria revelou 2 diretórios com conteúdo diferente.
- **Impacto**: Todos os 7 agentes têm Tool Failure Reporting em ambos os locais.
- **Agentes afetados**: Todos.

### agent-improver sem Tool Failure Reporting
- **Problema**: agent-improver.md não tinha Tool Failure Reporting nem Trace section. Falhas silenciosas não eram registradas.
- **Solução**: Adicionar Tool Failure Reporting + Trace com `bash_commands_run/denied/failed` e `tool_failures[]`.
- **Gatilho**: Auditoria revelou que todos os outros agentes runtime tinham, exceto agent-improver.
- **Impacto**: Meta-agent agora rastreia falhas de ferramentas.
- **Agentes afetados**: agent-improver.

### quality-runner runtime com pipeline 2-pass
- **Problema**: Runtime quality-runner.md ainda usava pipeline 2-pass, referência já tinha 3-pass.
- **Solução**: Substituir pipeline para 3-pass + Static Analysis Detail + Tool Details.
- **Impacto**: Análise estática sempre roda antes dos testes.
- **Agentes afetados**: quality-runner, orchestrator.

### librarian runtime sem ✅ Recommended
- **Problema**: Runtime librarian.md não tinha ✅ Recommended, Heuristics, nem Limitations.
- **Solução**: Adicionar ao Output Format + Workflow (consider project context).
- **Impacto**: Librarian runtime agora retorna recomendação ranqueada.
- **Agentes afetados**: librarian, orchestrator.
