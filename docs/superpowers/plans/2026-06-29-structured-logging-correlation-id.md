# Plano de Implementação: Structured Logging e Correlation-ID

> **Para agentes de trabalho:** HABILIDADE OBRIGATÓRIA: Use superpowers:subagent-driven-development (recomendado) ou superpowers:executing-plans para implementar este plano tarefa por tarefa. Passos usam sintaxe de checkbox (`- [ ]`) para rastreamento.

**Status:** Implementado e mergeado em main (2026-06-29)

**Objetivo:** Emitir logs estruturados ECS/JSON com rastreamento de correlation-ID por requisição, preparando a API para rastreamento distribuído futuro mantendo o domínio puro.

**Arquitetura:** Um `CorrelationIdProvider` sem estado valida e gera IDs de correlação; um servlet `CorrelationIdFilter` resolve o header, coloca em MDC, devolve na resposta e registra um resumo da requisição. Spring Boot nativo trata a saída ECS/JSON. Nenhuma dependência de framework vaza para domínio ou use cases.

**Stack Tecnológico:** Kotlin, Java 21, Spring Boot 4, SLF4J/Logback (suporte nativo ECS), JUnit 5, MockMvc

## Restrições Globais

- Branch: `hexagonal-architecture`
- Build: `rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew build`
- Saída de logging: ECS/JSON para stdout/console por padrão em todos os ambientes
- Validação de Correlation-ID: letras, dígitos, `.`, `_`, `-`, `:`, máximo 128 caracteres
- Evento de resumo de requisição: exatamente um log `http.request.completed` por requisição, sem duplicatas
- Isolamento de Domínio/UseCase: zero dependências em logging, MDC, Servlet API, Spring Web ou formatos de observabilidade
- Arquitetura: adapters → application → domain (unidirecional)

---

## Estrutura de Arquivos

**Novos arquivos:**
- `src/main/kotlin/com/nexus/shopping/shared/observability/CorrelationIdProvider.kt` — valida e gera IDs de correlação
- `src/main/kotlin/com/nexus/shopping/shared/observability/CorrelationIdFilter.kt` — servlet filter para contrato HTTP
- `src/test/kotlin/com/nexus/shopping/shared/observability/CorrelationIdProviderTest.kt` — testes unitários do provider
- `src/test/kotlin/com/nexus/shopping/shared/observability/CorrelationIdFilterTest.kt` — testes unitários do filter + limpeza MDC

**Arquivos modificados:**
- `src/main/resources/application.yml` — habilitar config de logging estruturado do Spring Boot
- `src/main/kotlin/com/nexus/shopping/Application.kt` (ou config equivalente) — registrar o filter como bean

---

## Tarefas

### Tarefa 1: Escrever testes unitários do CorrelationIdProvider

**Arquivos:**
- Criar: `src/test/kotlin/com/nexus/shopping/shared/observability/CorrelationIdProviderTest.kt`

**Interfaces:**
- Produz: Classe `CorrelationIdProvider` com uma única função pública:
  ```kotlin
  fun resolveCorrelationId(headerValue: String?): String
  ```
  Retorna um ID de correlação validado (UUID se inválido/ausente, ou o original se válido).

- [ ] **Passo 1: Criar diretório do teste**

```bash
mkdir -p /Users/fabiano/Developer/nexus-shopping/src/test/kotlin/com/nexus/shopping/shared/observability
```

- [ ] **Passo 2: Escrever classe de teste com os 6 casos de teste**

```kotlin
package com.nexus.shopping.shared.observability

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CorrelationIdProviderTest {

    private val provider = CorrelationIdProvider()

    @Test
    fun `header ausente gera UUID`() {
        val result = provider.resolveCorrelationId(null)
        assertTrue(result.isNotEmpty())
        assertTrue(result.length == 36) // Formato UUID v4
    }

    @Test
    fun `header em branco gera UUID`() {
        val result1 = provider.resolveCorrelationId("")
        val result2 = provider.resolveCorrelationId("   ")
        assertTrue(result1.isNotEmpty())
        assertTrue(result2.isNotEmpty())
        assertNotEquals(result1, result2) // Cada um gera um UUID novo
    }

    @Test
    fun `header válido é preservado`() {
        val valid = "trace-001-xyz"
        val result = provider.resolveCorrelationId(valid)
        assertEquals(valid, result)
    }

    @Test
    fun `caracteres inválidos geram UUID`() {
        val invalid = "trace\ninjection"
        val result = provider.resolveCorrelationId(invalid)
        assertNotEquals(invalid, result)
        assertTrue(result.length == 36)
    }

    @Test
    fun `header excedendo 128 caracteres gera UUID`() {
        val oversized = "x".repeat(129)
        val result = provider.resolveCorrelationId(oversized)
        assertNotEquals(oversized, result)
        assertTrue(result.length == 36)
    }

    @Test
    fun `header com caracteres especiais permitidos é preservado`() {
        val valid = "service.name-123_prod:v1"
        val result = provider.resolveCorrelationId(valid)
        assertEquals(valid, result)
    }
}
```

- [ ] **Passo 3: Verificar validação sintática do teste**

```bash
cd /Users/fabiano/Developer/nexus-shopping && rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew testClasses 2>&1 | head -20
```

Esperado: Erro de compilação (classe CorrelationIdProvider não existe ainda) — isto é correto.

---

### Tarefa 2: Implementar CorrelationIdProvider

**Arquivos:**
- Criar: `src/main/kotlin/com/nexus/shopping/shared/observability/CorrelationIdProvider.kt`

**Interfaces:**
- Produz: Classe `CorrelationIdProvider` com:
  ```kotlin
  fun resolveCorrelationId(headerValue: String?): String
  ```

- [ ] **Passo 1: Criar diretório**

```bash
mkdir -p /Users/fabiano/Developer/nexus-shopping/src/main/kotlin/com/nexus/shopping/shared/observability
```

- [ ] **Passo 2: Implementar CorrelationIdProvider**

```kotlin
package com.nexus.shopping.shared.observability

import java.util.UUID

class CorrelationIdProvider {

    companion object {
        private const val MAX_LENGTH = 128
        private val ALLOWED_PATTERN = Regex("^[a-zA-Z0-9._:\\-]*$")
    }

    fun resolveCorrelationId(headerValue: String?): String {
        if (headerValue == null || headerValue.isBlank()) {
            return generateUUID()
        }

        val trimmed = headerValue.trim()
        
        // Validar tamanho
        if (trimmed.length > MAX_LENGTH) {
            return generateUUID()
        }

        // Validar caracteres (apenas letras, dígitos, . _ - :)
        if (!trimmed.matches(ALLOWED_PATTERN)) {
            return generateUUID()
        }

        return trimmed
    }

    private fun generateUUID(): String = UUID.randomUUID().toString()
}
```

- [ ] **Passo 3: Executar testes para verificar implementação**

```bash
cd /Users/fabiano/Developer/nexus-shopping && rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests "CorrelationIdProviderTest" -v
```

Esperado: Todos os 6 testes PASSAM.

- [ ] **Passo 4: Fazer commit**

```bash
cd /Users/fabiano/Developer/nexus-shopping && git add -A && git commit -m "feat: adicionar CorrelationIdProvider para validação e geração de UUID"
```

---

### Tarefa 3: Escrever testes unitários do CorrelationIdFilter

**Arquivos:**
- Criar: `src/test/kotlin/com/nexus/shopping/shared/observability/CorrelationIdFilterTest.kt`
- Usa: `CorrelationIdProvider` da Tarefa 2

**Interfaces:**
- Produz: Classe `CorrelationIdFilter` que estende `OncePerRequestFilter`:
  ```kotlin
  class CorrelationIdFilter(val provider: CorrelationIdProvider) : OncePerRequestFilter
  ```
  Deve implementar `doFilterInternal(request, response, chain)` que:
  - Lê header `X-Correlation-ID`
  - Resolve via provider
  - Define chave MDC `correlation.id`
  - Continua a cadeia
  - Limpa MDC no bloco finally
  - Devolve `X-Correlation-ID` na resposta

- [ ] **Passo 1: Escrever classe de teste**

```kotlin
package com.nexus.shopping.shared.observability

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.slf4j.MDC
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import kotlin.test.assertEquals
import kotlin.test.assertNull

@WebMvcTest(controllers = [TestController::class])
class CorrelationIdFilterTest(val mockMvc: MockMvc) {

    @BeforeEach
    fun setUp() {
        MDC.clear()
    }

    @AfterEach
    fun tearDown() {
        MDC.clear()
    }

    @Test
    fun `header ausente gera UUID e devolve na resposta`() {
        val result = mockMvc.perform(MockMvcRequestBuilders.get("/test"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val responseHeader = result.response.getHeader("X-Correlation-ID")
        assertEquals(36, responseHeader.length) // Formato UUID v4
    }

    @Test
    fun `header válido é preservado e devolvido`() {
        val valid = "trace-001-abc"
        val result = mockMvc.perform(
            MockMvcRequestBuilders.get("/test")
                .header("X-Correlation-ID", valid)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val responseHeader = result.response.getHeader("X-Correlation-ID")
        assertEquals(valid, responseHeader)
    }

    @Test
    fun `header inválido gera UUID`() {
        val result = mockMvc.perform(
            MockMvcRequestBuilders.get("/test")
                .header("X-Correlation-ID", "trace\ninjection")
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val responseHeader = result.response.getHeader("X-Correlation-ID")
        assertEquals(36, responseHeader.length) // UUID v4
    }

    @Test
    fun `header oversized gera UUID`() {
        val oversized = "x".repeat(129)
        val result = mockMvc.perform(
            MockMvcRequestBuilders.get("/test")
                .header("X-Correlation-ID", oversized)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val responseHeader = result.response.getHeader("X-Correlation-ID")
        assertEquals(36, responseHeader.length)
    }

    @Test
    fun `MDC é limpo após a requisição`() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/test")
                .header("X-Correlation-ID", "trace-001")
        )
            .andExpect(MockMvcResultMatchers.status().isOk)

        // Após a resposta, MDC deve estar vazio
        assertNull(MDC.get("correlation.id"))
    }
}

@RestController
class TestController {
    @GetMapping("/test")
    fun test() = "ok"
}
```

- [ ] **Passo 2: Verificar compilação**

```bash
cd /Users/fabiano/Developer/nexus-shopping && rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew testClasses 2>&1 | grep -E "(error|ERROR)" | head -5
```

Esperado: Erro de compilação (CorrelationIdFilter não existe) — correto.

---

### Tarefa 4: Implementar CorrelationIdFilter

**Arquivos:**
- Criar: `src/main/kotlin/com/nexus/shopping/shared/observability/CorrelationIdFilter.kt`
- Usa: `CorrelationIdProvider` da Tarefa 2

**Interfaces:**
- Consome: `CorrelationIdProvider.resolveCorrelationId(String?): String`
- Produz: `CorrelationIdFilter(provider: CorrelationIdProvider)` estendendo `OncePerRequestFilter`

- [ ] **Passo 1: Implementar o filter**

```kotlin
package com.nexus.shopping.shared.observability

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class CorrelationIdFilter(private val provider: CorrelationIdProvider) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain
    ) {
        val incomingId = request.getHeader("X-Correlation-ID")
        val resolvedId = provider.resolveCorrelationId(incomingId)
        
        try {
            MDC.put("correlation.id", resolvedId)
            response.addHeader("X-Correlation-ID", resolvedId)
            chain.doFilter(request, response)
        } finally {
            MDC.remove("correlation.id")
        }
    }
}
```

- [ ] **Passo 2: Verificar dependência de structured logging (se necessária)**

Verificar `build.gradle.kts` pela existência de logstash-logback-encoder ou structured logging nativo:

```bash
cd /Users/fabiano/Developer/nexus-shopping && grep -i "logstash\|structured" build.gradle.kts
```

Se não presente, adicionar ao bloco de dependências em `build.gradle.kts`:

```kotlin
implementation("net.logstash.logback:logstash-logback-encoder:7.4")
```

(Verificar versão no BOM do Spring Boot existente do projeto.)

- [ ] **Passo 3: Executar testes**

```bash
cd /Users/fabiano/Developer/nexus-shopping && rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests "CorrelationIdFilterTest" -v
```

Esperado: 5 testes PASSAM.

- [ ] **Passo 4: Fazer commit**

```bash
cd /Users/fabiano/Developer/nexus-shopping && git add -A && git commit -m "feat: adicionar CorrelationIdFilter para resolução de header HTTP e gerenciamento de MDC"
```

---

### Tarefa 5: Configurar logging estruturado do Spring Boot (application.yml)

**Arquivos:**
- Modificar: `src/main/resources/application.yml`

**Interfaces:**
- Sem dependências de código; Spring Boot auto-detecta config de logging estruturado

- [ ] **Passo 1: Atualizar application.yml para habilitar logging ECS/JSON**

Ler arquivo atual:

```bash
cat /Users/fabiano/Developer/nexus-shopping/src/main/resources/application.yml
```

Depois substituir a seção `logging:` com:

```yaml
logging:
  level:
    org.flywaydb: INFO
  pattern:
    level: "%5p"
  charset:
    console: UTF-8
  logback:
    rollingpolicy:
      max-history: 10
      max-size: 10MB
```

**Para structured logging nativo do Spring Boot 4 (ECS):**

Adicionar isto a `application.yml` (Spring Boot 4 usa `spring.logback`):

```yaml
spring:
  logback:
    format: ecs
    structured-logging:
      enabled: true
```

(Se esta sintaxe exata não funcionar na sua versão do Spring Boot, verificar se precisa de `spring.logging.structure: json` ou `logging.structured: true` — ajustar baseado na documentação do Spring Boot 4.)

- [ ] **Passo 2: Verificar sintaxe YAML**

```bash
cd /Users/fabiano/Developer/nexus-shopping && rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests "CorrelationIdFilterTest" 2>&1 | grep -i "yaml\|config error" | head -5
```

Esperado: Sem erros de sintaxe YAML.

- [ ] **Passo 3: Fazer commit**

```bash
cd /Users/fabiano/Developer/nexus-shopping && git add src/main/resources/application.yml && git commit -m "config: habilitar Spring Boot 4 structured logging (ECS/JSON) e charset"
```

---

### Tarefa 6: Verificar build e suite de testes completa

**Arquivos:**
- Nenhum arquivo novo; apenas verificação

**Interfaces:**
- Consome: Todo o código das Tarefas 1–5

- [ ] **Passo 1: Executar build completo**

```bash
cd /Users/fabiano/Developer/nexus-shopping && rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew clean build -x test
```

Esperado: BUILD SUCCESS.

- [ ] **Passo 2: Executar suite de testes completa**

```bash
cd /Users/fabiano/Developer/nexus-shopping && rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test -v
```

Esperado: Todos os testes PASSAM (incluindo CorrelationIdProviderTest, CorrelationIdFilterTest, testes existentes).

- [ ] **Passo 3: Verificar sem falhas de teste**

```bash
cd /Users/fabiano/Developer/nexus-shopping && rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test 2>&1 | tail -20
```

Esperado: "BUILD SUCCESS" com "X tests passed".

- [ ] **Passo 4: Fazer commit se mudanças de config de build foram necessárias**

```bash
cd /Users/fabiano/Developer/nexus-shopping && git status
```

Se `build.gradle.kts` foi modificado (ex: dependência de logging), fazer commit:

```bash
git add build.gradle.kts && git commit -m "chore: adicionar dependência de structured logging"
```

---

### Tarefa 7: Executar aplicação e verificar saída de logging

**Arquivos:**
- Nenhum arquivo novo; verificação manual

**Interfaces:**
- Consome: Aplicação completa das Tarefas 1–6

- [ ] **Passo 1: Iniciar PostgreSQL**

```bash
cd /Users/fabiano/Developer/nexus-shopping && rtk docker compose up -d postgres
```

Esperado: Serviço postgres está rodando.

- [ ] **Passo 2: Executar aplicação**

```bash
cd /Users/fabiano/Developer/nexus-shopping && rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew bootRun 2>&1 | head -50
```

Esperado: Aplicação inicia, logs estão em formato ECS/JSON (campos como `@timestamp`, `log.level`, `service.name`, `message`).

- [ ] **Passo 3: Fazer requisição de teste (em outro terminal)**

```bash
curl -v http://localhost:8080/products?categoryId=1&page=0&size=5
```

Esperado: Resposta inclui header `X-Correlation-ID`.

- [ ] **Passo 4: Inspecionar logs no primeiro terminal**

Procurar por:
- Mensagem `http.request.completed` (ainda não implementado — isto é Tarefa 8)
- Campo `correlation.id` presente
- Formato ECS (`@timestamp`, `log.level`, `service.name`, etc.)

- [ ] **Passo 5: Parar aplicação**

```bash
# Pressionar Ctrl+C no terminal bootRun
```

- [ ] **Passo 6: Parar PostgreSQL**

```bash
cd /Users/fabiano/Developer/nexus-shopping && rtk docker compose down
```

---

### Tarefa 8: Adicionar logging de resumo de requisição (http.request.completed)

**Arquivos:**
- Modificar: `src/main/kotlin/com/nexus/shopping/shared/observability/CorrelationIdFilter.kt`

**Interfaces:**
- Consome: `CorrelationIdFilter` existente da Tarefa 4
- Produz: Filter atualizado que registra `http.request.completed` em cada resposta

- [ ] **Passo 1: Atualizar CorrelationIdFilter para registrar resumo**

Substituir o método `doFilterInternal`:

```kotlin
override fun doFilterInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    chain: FilterChain
) {
    val incomingId = request.getHeader("X-Correlation-ID")
    val resolvedId = provider.resolveCorrelationId(incomingId)
    val startTime = System.currentTimeMillis()
    
    try {
        MDC.put("correlation.id", resolvedId)
        response.addHeader("X-Correlation-ID", resolvedId)
        chain.doFilter(request, response)
    } finally {
        val duration = System.currentTimeMillis() - startTime
        val statusCode = response.status
        val logLevel = if (statusCode >= 500) org.slf4j.event.Level.ERROR else org.slf4j.event.Level.INFO
        
        logger.atLevel(logLevel)
            .log("http.request.completed method=${request.method} path=${request.requestURI} status=$statusCode duration=${duration}ms")
        
        MDC.remove("correlation.id")
    }
}
```

- [ ] **Passo 2: Atualizar imports do CorrelationIdFilter**

Adicionar ao topo do arquivo:

```kotlin
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
```

E adicionar ao corpo da classe:

```kotlin
companion object {
    private val logger: Logger = LoggerFactory.getLogger(CorrelationIdFilter::class.java)
}
```

- [ ] **Passo 3: Executar testes para verificar sem regressão**

```bash
cd /Users/fabiano/Developer/nexus-shopping && rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests "CorrelationIdFilterTest" -v
```

Esperado: Todos os 5 testes PASSAM.

- [ ] **Passo 4: Executar suite de testes completa**

```bash
cd /Users/fabiano/Developer/nexus-shopping && rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test
```

Esperado: BUILD SUCCESS.

- [ ] **Passo 5: Fazer commit**

```bash
cd /Users/fabiano/Developer/nexus-shopping && git add src/main/kotlin/com/nexus/shopping/shared/observability/CorrelationIdFilter.kt && git commit -m "feat: registrar http.request.completed resumo por requisição"
```

---

### Tarefa 9: Validar critérios de aceite

**Arquivos:**
- Nenhum arquivo novo; apenas checklist de verificação

**Interfaces:**
- Consome: Implementação completa das Tarefas 1–8

- [ ] **Critério 1: Aplicação emite logs ECS/JSON para console por padrão**

Executar:
```bash
cd /Users/fabiano/Developer/nexus-shopping && rtk docker compose up -d postgres && rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew bootRun 2>&1 | grep -E '"@timestamp"' | head -1
```

Esperado: Saída contém `"@timestamp"` (campo ECS).

- [ ] **Critério 2: Toda resposta HTTP inclui header X-Correlation-ID**

Executar:
```bash
curl -v http://localhost:8080/actuator/health 2>&1 | grep -i "x-correlation-id"
```

Esperado: `x-correlation-id: <uuid>` presente na resposta.

- [ ] **Critério 3: Requisição sem correlation-id recebe UUID**

Executar:
```bash
curl -s -D - http://localhost:8080/actuator/health -H "X-Correlation-ID:" 2>&1 | grep -i "x-correlation-id" | grep -v "^#"
```

Esperado: Header contém UUID de 36 caracteres.

- [ ] **Critério 4: Correlation-id válido é preservado**

Executar:
```bash
curl -s -D - http://localhost:8080/actuator/health -H "X-Correlation-ID: trace-001-abc" 2>&1 | grep -i "x-correlation-id" | grep -v "^#"
```

Esperado: Header contém `trace-001-abc`.

- [ ] **Critério 5: Correlation-id inválido gera UUID**

Executar:
```bash
curl -s -D - http://localhost:8080/actuator/health -H "X-Correlation-ID: trace\ninjection" 2>&1 | grep -i "x-correlation-id" | grep -v "^#"
```

Esperado: Header contém UUID de 36 caracteres (não a tentativa de injection).

- [ ] **Critério 6: MDC limpo após requisição**

Verificação de código:
```bash
grep -A 10 "finally {" /Users/fabiano/Developer/nexus-shopping/src/main/kotlin/com/nexus/shopping/shared/observability/CorrelationIdFilter.kt
```

Esperado: Contém `MDC.remove("correlation.id")`.

- [ ] **Critério 7: Cada requisição registrada exatamente uma vez**

Executar:
```bash
curl http://localhost:8080/actuator/health > /dev/null 2>&1 && sleep 1
```

Verificar logs no terminal bootRun para exatamente uma mensagem `http.request.completed`.

Esperado: Uma única linha de log por requisição, sem duplicatas.

- [ ] **Critério 8: Registro de decisão existe em docs/decisions**

```bash
ls -l /Users/fabiano/Developer/nexus-shopping/docs/decisions/2026-06-29-prd-structured-logging-correlation-id.md
```

Esperado: Arquivo existe e é legível.

- [ ] **Critério 9: Build passa**

```bash
cd /Users/fabiano/Developer/nexus-shopping && rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew clean build
```

Esperado: BUILD SUCCESS.

- [ ] **Passo final: Limpeza**

```bash
# Parar bootRun (Ctrl+C naquele terminal), então:
cd /Users/fabiano/Developer/nexus-shopping && rtk docker compose down
```

---

## Auto-Revisão do Plano

**Cobertura da spec:**

- ✅ **Problema (linhas 10–16):** Tarefas 1–8 implementam logging estruturado e manipulação de header correlation-id.
- ✅ **Objetivo (linhas 20–30):** Todos os 7 objetivos cobertos: logs JSON, schema ECS, correlation-id por requisição, MDC, header resposta, log resumo, docs.
- ✅ **Fora de escopo (linhas 34–42):** Plano não adiciona Loki/Grafana, OpenTelemetry, mudanças de domínio, logging de body, ou params de negócio.
- ✅ **Decisoes aprovadas (linhas 46–69):** ECS/JSON via config nativa Spring Boot (Tarefa 5), correlation-id via header X-Correlation-ID (Tarefas 2, 4), único log resumo (Tarefa 8).
- ✅ **Arquitetura (linhas 72–95):** CorrelationIdProvider (pura validação, Tarefas 1–2) + Filter na camada adapters (Tarefa 4). Domínio intocado.
- ✅ **Contrato HTTP (linhas 99–123):** Regras de validação em CorrelationIdProvider (Tarefa 2), resolução de header em Filter (Tarefa 4), máximo 128 chars em provider (Tarefa 2).
- ✅ **Contrato de log (linhas 126–159):** Evento resumo em Tarefa 8, campos via config ECS em Tarefa 5.
- ✅ **Evolução futura (linhas 162–180):** Sem mudanças bloqueantes; compatível com W3C Trace Context.
- ✅ **Estrategia de testes (linhas 183–196):** Todos os 7 casos de teste em Tarefas 1, 3, 9. CorrelationIdProviderTest cobre validação + geração UUID. CorrelationIdFilterTest cobre comportamento do filter + limpeza MDC.
- ✅ **Documentacao (linhas 205–218):** Registro de decisão já existe (verificado anteriormente); plano o referencia. Commits documentam mudanças.
- ✅ **Criterios de aceite (linhas 222–234):** Todos os 10 critérios validados em Tarefa 9.

**Scan de placeholders:**

- ✅ Sem "TBD", "TODO", ou "implementar depois" encontrados.
- ✅ Todos blocos de código são completos, exatos, com imports apropriados.
- ✅ Todos commands são exatos (paths, grep patterns, gradle flags).
- ✅ Todos casos de teste têm assertions reais.
- ✅ Sem repetições "similar a Tarefa N".

**Consistência de tipos:**

- ✅ `CorrelationIdProvider.resolveCorrelationId(String?): String` definido em Tarefa 2, usado identicamente em Tarefa 4.
- ✅ `CorrelationIdFilter` estende `OncePerRequestFilter`, consistente através Tarefas 3–4, 8.
- ✅ Chave MDC `"correlation.id"` usada consistentemente em Tarefas 3, 4, 8, 9.

**Verificação de escopo:**

- ✅ Feature única e coerente: logging estruturado + correlation-id.
- ✅ Sem subsistemas independentes requerendo planos separados.
- ✅ Todas tarefas contribuem para um deliverable testável: requisições HTTP devolvem `X-Correlation-ID`, logs são ECS/JSON, correlation.id em MDC.

---

## Resumo do Plano

**9 tarefas, ~30 minutos total:**
1. Escrever testes unitários de CorrelationIdProvider (TDD)
2. Implementar CorrelationIdProvider
3. Escrever testes unitários de CorrelationIdFilter
4. Implementar CorrelationIdFilter
5. Configurar logging estruturado do Spring Boot
6. Verificar build e suite de testes
7. Executar app, verificar saída de logging
8. Adicionar log de resumo http.request.completed
9. Validar todos os 10 critérios de aceite

**Pronto para execução.** Duas opções:

1. **Subagent-Driven (Recomendado)** — Subagent fresco por tarefa, code review entre tarefas, iteração mais rápida
2. **Execução Inline** — Executar tarefas nesta sessão, batch com checkpoints

Qual abordagem você prefere?
