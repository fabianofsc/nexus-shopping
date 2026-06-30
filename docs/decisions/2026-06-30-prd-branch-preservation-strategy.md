# ADR: Estratégia de Preservação de Branches de Referência para Testes

**Status:** Proposto para implementação
**Data:** 2026-06-30
**Contexto:** Branches educacionais que servem como baseline para testes de performance e comparações arquiteturais

---

## Problema

Existem 4 branches que funcionam como **pontos de referência estáveis** para testes comparativos:
- `missing-index-performance-baseline` — sem indexes secundários
- `add-product-query-indexes` — com indexes
- `add-products-pagination` — com paginação
- `hexagonal-architecture` — arquitetura hexagonal

Atualmente, estas branches estão "travadas" manualmente para evitar modificações. Há necessidade de uma estratégia formal do GitHub para:
1. **Proteger contra mudanças acidentais**
2. **Fazer referência clara ao estado** (qual versão do código foi usada em qual teste)
3. **Permitir reprodutibilidade** (clonar um estado específico sem precisar entender branches)
4. **Documentar propósito** (por que essa branch existe e quando usá-la)

---

## Opções avaliadas

### Opção 1: Apenas branches protegidas (Status Quo melhorado)

**O que fazer:**
- Configurar regras de proteção de branch no GitHub Settings
- Exigir pull requests + aprovações para merge
- Bloquear force pushes
- Exigir status checks passando

**Vantagens:**
- ✅ Previne mudanças acidentais
- ✅ Mantém histórico de git legível
- ✅ Fácil de reverter se necessário
- ✅ GitHub nativo, sem ferramentas externas

**Desvantagens:**
- ❌ Não indica claramente quando/como o código foi testado
- ❌ Requer saber que a branch existe e é "especial"
- ❌ Sem versionamento claro (qual estado exatamente foi usado?)
- ❌ Mudanças futuras não são rastreáveis

**Exemplo de uso:**
```bash
git clone --branch missing-index-performance-baseline https://github.com/user/repo.git
# Usuário não sabe: qual versão de Java? qual JMeter version? quando foi testado?
```

---

### Opção 2: Tags anotadas + branches protegidas (Recomendado)

**O que fazer:**
1. Criar tags anotadas para cada milestone de teste:
```bash
git tag -a v1.0-baseline-no-indexes -m "Baseline: performance without indexes. Tested: 2026-06-30. JMeter: 5.6"
git tag -a v1.1-with-indexes -m "Index optimization: category_id, name. Tested: 2026-06-30"
git tag -a v1.2-with-pagination -m "Pagination via Slice. Tested: 2026-06-30"
git tag -a v2.0-hexagonal-architecture -m "Hexagonal architecture. Tested: 2026-06-30"
```

2. Proteger as branches no GitHub
3. Adicionar arquivo `REFERENCE_POINTS.md` no root documentando cada tag

**Vantagens:**
- ✅ **Versionamento claro** — `v1.0`, `v1.1`, etc. indicam evolução
- ✅ **Imutável** — tags anotadas não podem ser movidas sem força
- ✅ **Rastreabilidade** — mensagem da tag documenta contexto (data, como foi testado)
- ✅ **Reprodutibilidade** — `git checkout v1.1-with-indexes` é inequívoco
- ✅ **GitHub Releases** pode expor tags como "Downloads"
- ✅ **Integração com CI/CD** — pipelines podem ser acionadas por tags
- ✅ **Documentação automática** — GitHub mostra tags no histórico de releases

**Desvantagens:**
- ⚠️ Requer disciplina de criar tags quando branches se estabilizam
- ⚠️ Tag e branch precisam estar sincronizados manualmente

**Exemplo de uso:**
```bash
# Usuário sabe exatamente qual versão clonar
git clone --branch v1.1-with-indexes https://github.com/user/repo.git

# Ou via releases
# "Download" button oferece zip de v1.1-with-indexes
```

---

### Opção 3: GitHub Releases (Recomendado + Tags)

**O que fazer:**
1. Criar tags anotadas (como Opção 2)
2. Converter cada tag em um GitHub Release com:
   - Título descritivo (`Baseline Without Indexes`)
   - Notas detalhadas (ambiente, JMeter version, resultados)
   - Links para documentação de testes
   - Binários (Docker image, jar compilado)
   - Checksum para reprodutibilidade

**Exemplo:**
```
Release: v1.1-with-indexes
Title: Optimization with Query Indexes (category_id, name)

Description:
- **Test Date:** 2026-06-30
- **JMeter Version:** 5.6
- **Scenario:** 50 threads, 120s duration
- **Results:** [link to docs/load-test-results-indexes.md]
- **Environment:** Java 21, PostgreSQL 16, Spring Boot 4.1
- **Docker Image:** fabianofsc/nexus-shopping:v1.1-indexes

Artifacts:
- nexus-shopping-v1.1.jar
- docker-compose.yml
```

**Vantagens:**
- ✅ Tudo da Opção 2, mais:
- ✅ **Documentação centralizada** — notas de release no GitHub
- ✅ **Artefatos anexados** — jar/docker image pré-compilado
- ✅ **Discoverabilidade** — "Releases" tab no GitHub é visível
- ✅ **Histórico público** — qualquer pessoa vê evolução e testes
- ✅ **Integração com downloads** — zip disponível para cada release

**Desvantagens:**
- ⚠️ Mais overhead — manter notas de release
- ⚠️ Requer publicar artefatos (ou gerar sob demanda)

---

### Opção 4: Milestones + Issues (Não recomendado para este caso)

GitHub Milestones agrupam issues/PRs. Não é apropriado para "preservar branches" porque:
- Milestones são sobre planejamento de features, não para "congelar" código
- Não oferece proteção de branch
- Falta de rastreabilidade do código exato usado

---

## Decisão: Opção 2 + Opção 3

**Implementar Tags anotadas + GitHub Releases** por ser:

1. **Minimamente invasivo** — nenhuma mudança em branches atuais
2. **Escalável** — funciona para N branches de referência
3. **Educacional** — alunos veem versionamento profissional
4. **Reprodutível** — SHA256 de commit está na tag para verificação
5. **Futuro-proof** — fácil adicionar mais releases conforme testes evoluem

---

## Plano de implementação

### Fase 1: Criar tags para branches existentes

```bash
# Baseline (sem indexes)
git checkout missing-index-performance-baseline
git tag -a v1.0-baseline \
  -m "Baseline: no secondary indexes. Tested 2026-06-30. See docs/load-test-results-baseline.md"
git push origin v1.0-baseline

# Com indexes
git checkout add-product-query-indexes
git tag -a v1.1-indexes \
  -m "Optimization: indexes on category_id and name. Tested 2026-06-30. See docs/load-test-results-indexes.md"
git push origin v1.1-indexes

# Com paginação
git checkout add-products-pagination
git tag -a v1.2-pagination \
  -m "Pagination: Slice without COUNT(*). Tested 2026-06-30. See docs/load-test-results-pagination.md"
git push origin v1.2-pagination

# Hexagonal
git checkout hexagonal-architecture
git tag -a v2.0-hexagonal \
  -m "Hexagonal architecture: ports, adapters, domain isolation. Merged 2026-06-30. See docs/superpowers/specs/2026-06-30-hexagonal-architecture-design.md"
git push origin v2.0-hexagonal
```

### Fase 2: Proteger branches no GitHub

Settings → Branches → Add rule:
- Pattern: `missing-index-performance-baseline`, `add-product-query-indexes`, etc.
- ✅ Require pull request reviews before merging
- ✅ Require status checks to pass
- ✅ Require branches to be up to date before merging
- ✅ Include administrators
- ✅ Restrict who can push to matching branches

### Fase 3: Criar arquivo `REFERENCE_POINTS.md`

```markdown
# Reference Points for Testing & Learning

Esta página documenta branches/tags imutáveis para testes comparativos.

## v1.0-baseline
- **Branch:** `missing-index-performance-baseline`
- **Propósito:** Baseline de performance SEM indexes secundários
- **Testado:** 2026-06-30
- **Clonar:** `git clone --branch v1.0-baseline ...`
- **Resultados:** docs/load-test-results-baseline.md

[Similar para v1.1-indexes, v1.2-pagination, v2.0-hexagonal]
```

### Fase 4: Criar GitHub Releases (opcional, mas recomendado)

Para cada tag, criar Release no GitHub com:
- Título: `v1.0: Baseline (No Indexes)`
- Descrição com contexto, ambiente, links para documentação
- Considerar anexar Docker image ou jar compilado

---

## Convenção de versionamento

```
v<major>.<minor>[-descriptor]

v1.0-baseline           # Baseline, sem otimizações
v1.1-indexes            # v1 + indexes
v1.2-pagination         # v1 + indexes + paginação
v2.0-hexagonal          # v2: nova arquitetura

Alternativamente:
v1.0.0-no-indexes
v1.1.0-with-indexes
v1.2.0-with-pagination
v2.0.0-hexagonal-architecture
```

---

## Impacto e consequências

**Positivos:**
- Branches estão formalmente protegidas (não acidental)
- Alunos veem melhor prática de versionamento
- Reprodutibilidade garantida (hash de commit na tag)
- Histórico de testes centralizado em GitHub Releases
- Fácil citar em documentação: "veja v1.1-indexes para código"

**Trade-offs:**
- Requer manutenção de tags quando branches se movem
- Releases precisam de notas (automático se bem documentado)

**Alternativa descartada:**
- Deixar como está (apenas branches) — falta clareza e rastreabilidade

---

## Próximos passos

1. Implementar tags anotadas para as 4 branches (hoje)
2. Proteger branches via GitHub Settings (hoje)
3. Criar `REFERENCE_POINTS.md` (hoje)
4. Considerar GitHub Releases (próxima semana, quando testes forem mais consolidados)
5. Documentar esta estratégia no README ou na seção "For Students"
