# Reference Points for Testing & Learning

Este documento lista as branches e tags imutáveis que servem como pontos de referência para testes comparativos e estudos educacionais.

---

## 📌 Tags de Referência

### v1.0-baseline
**Baseline: Sem Indexes Secundários**

- **Branch:** `missing-index-performance-baseline`
- **Commit:** (veja `git show v1.0-baseline`)
- **Propósito:** Estado do código SEM otimizações de index para servir como baseline
- **Testado em:** 2026-06-30
- **Stack:** Kotlin, Java 21, Spring Boot 4.1, PostgreSQL, H2 (testes)
- **Documentação:** [docs/load-test-results-baseline.md](docs/load-test-results-baseline.md)
- **Como clonar:**
  ```bash
  git clone --branch v1.0-baseline https://github.com/fabianofsc/nexus-shopping.git
  ```
- **Propósito de aprendizado:** Ver como queries sem index afetam performance em tabelas grandes

---

### v1.1-indexes
**Otimização: Indexes em category_id e name**

- **Branch:** `add-product-query-indexes`
- **Commit:** (veja `git show v1.1-indexes`)
- **Propósito:** Primeira otimização: adicionar indexes secundários
- **Testado em:** 2026-06-30
- **Melhorias em relação a v1.0:**
  - Index B-tree em `products(category_id)` — acelera buscas por categoria
  - Index B-tree em `products(name)` — acelera buscas por nome (prefix range lookup)
- **Documentação:** [docs/load-test-results-indexes.md](docs/load-test-results-indexes.md)
- **Como clonar:**
  ```bash
  git clone --branch v1.1-indexes https://github.com/fabianofsc/nexus-shopping.git
  ```
- **Propósito de aprendizado:** Ver impacto de indexes em queries seletivas

---

### v1.2-pagination
**Otimização: Paginação sem COUNT(*)**

- **Branch:** `add-products-pagination`
- **Commit:** (veja `git show v1.2-pagination`)
- **Propósito:** Segunda otimização: implementar paginação Slice
- **Testado em:** 2026-06-30
- **Melhorias em relação a v1.1:**
  - Uso de `Slice<T>` em vez de `Page<T>` — elimina `COUNT(*)`
  - Query lê `size + 1` linhas para calcular `hasNext` sem contar
  - Endpoint retorna `{content, page, size, count, hasNext}` sem overhead de contagem
- **Documentação:** [docs/load-test-results-pagination.md](docs/load-test-results-pagination.md)
- **Como clonar:**
  ```bash
  git clone --branch v1.2-pagination https://github.com/fabianofsc/nexus-shopping.git
  ```
- **Propósito de aprendizado:** Ver como eliminar queries custosas (COUNT) mantendo paginação funcional

---

### v2.0-hexagonal
**Arquitetura: Hexagonal (Ports & Adapters)**

- **Branch:** `hexagonal-architecture`
- **Commit:** (veja `git show v2.0-hexagonal`)
- **Propósito:** Refatoração completa para arquitetura hexagonal
- **Merged em:** 2026-06-30
- **Mudanças arquiteturais:**
  - Estrutura: `domain/` → `application/` → `adapter/`
  - Dependency inversion: adapter → application → domain
  - Zero imports de framework em `domain/` e `application/`
  - DTOs conversão: `DTO.toCommand()` → `Command.toEntity()` → `Entity.toDomain()`
  - Validação centralizada em use cases
- **Documentação:** [docs/superpowers/specs/2026-06-30-hexagonal-architecture-design.md](docs/superpowers/specs/2026-06-30-hexagonal-architecture-design.md)
- **Como clonar:**
  ```bash
  git clone --branch v2.0-hexagonal https://github.com/fabianofsc/nexus-shopping.git
  ```
- **Propósito de aprendizado:** Entender Ports & Adapters, inversão de dependência, isolamento de domínio

---

## 🔒 Proteção de Branches

As branches que contêm essas tags estão protegidas no GitHub para evitar modificações acidentais:

- ✅ Require pull request reviews before merging
- ✅ Require status checks to pass
- ✅ Require branches to be up to date before merging
- ✅ Restrict direct pushes (só via PR)

**Para modificar uma branch protegida:**
1. Criar uma branch nova baseada nela
2. Fazer mudanças
3. Abrir PR com justificativa
4. Aprovar e fazer merge (requiere review)

---

## 📊 Relação entre versões

```
v1.0-baseline (sem otimizações)
    ↓
v1.1-indexes (+ indexes)
    ↓
v1.2-pagination (+ paginação sem COUNT)
    ↓ (refatoração completa)
v2.0-hexagonal (nova arquitetura)
```

**Comparar performance:**
```bash
# Clonar v1.0 e rodar JMeter
git clone --branch v1.0-baseline ...
make start-baseline
scripts/jmeter.sh products-by-name

# Depois, v1.1
git clone --branch v1.1-indexes ...
make start-indexes
scripts/jmeter.sh products-by-name

# Comparar resultados em docs/load-test-results-*.md
```

---

## 🎓 Para Alunos

Use estas tags como pontos de estudo:

1. **Estudar v1.0:** Entender a base do projeto e performance sem otimizações
2. **Comparar v1.0 vs v1.1:** Ver impacto de indexes em SQL
3. **Comparar v1.1 vs v1.2:** Entender eliminar queries custosas
4. **Estudar v2.0:** Aprender arquitetura hexagonal na prática

Cada tag tem:
- ✅ Código compilável e testável
- ✅ Documentação de design no `docs/superpowers/specs/`
- ✅ Resultados de testes em `docs/load-test-results-*.md`
- ✅ Histórico de mudanças em commits

---

## 📝 Criando nova versão de referência

Quando uma nova otimização ou arquitetura for estável:

```bash
# Após mesclar para main
git checkout <branch-otimizacao>
git tag -a v<major>.<minor>-<descritor> \
  -m "Descrição clara da mudança. Testado <data>. Ver docs/..."
git push origin v<major>.<minor>-<descritor>

# Adicionar linha neste arquivo com documentação
```

---

## 🔍 Verificar uma tag

```bash
# Ver informações da tag
git show v1.1-indexes

# Ver commit de uma tag
git rev-list -n 1 v1.1-indexes

# Verificar checksum (para reprodutibilidade)
git rev-parse v1.1-indexes
```

---

**Última atualização:** 2026-06-30
**Responsible:** Fabiano Góes
**Tags ativas:** v1.0-baseline, v1.1-indexes, v1.2-pagination, v2.0-hexagonal
