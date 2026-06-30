# Structured logging e correlation-id

**Status:** Implementado e mergeado em main (2026-06-29)
**Data:** 2026-06-29
**Branch alvo:** `hexagonal-architecture`
**Decision record relacionado:** `docs/decisions/2026-06-29-prd-structured-logging-correlation-id.md`

---

## Problema

O projeto hoje usa a configuracao padrao de logging do Spring Boot, com apenas um nivel explicito para Flyway em `application.yml`. Isso e suficiente para desenvolvimento local simples, mas nao prepara a aplicacao para consultas agregadas no Grafana no futuro.

O laboratorio tambem vai evoluir para demonstrar microservices em stacks e linguagens diversas. Por isso, o formato de log nao deve depender de detalhes especificos de Kotlin ou Spring. A aplicacao atual deve inaugurar um contrato simples, portavel e didatico que outras apps possam repetir.

Tambem precisamos preparar a API para correlacionar operacoes distribuidas no futuro. A primeira etapa deve oferecer um `correlation-id` estavel por requisicao, sem implementar tracing distribuido completo agora.

---

## Objetivo

Preparar a aplicacao para logs agregaveis por ferramentas como Grafana/Loki no futuro, mantendo a entrega pequena:

1. Emitir logs estruturados em JSON no console por padrao.
2. Usar ECS (Elastic Common Schema) como base do formato.
3. Resolver um `X-Correlation-ID` por requisicao HTTP.
4. Colocar o correlation-id no MDC para aparecer nos logs estruturados.
5. Devolver `X-Correlation-ID` em toda resposta HTTP.
6. Registrar um unico log de resumo por requisicao concluida.
7. Documentar as motivacoes e escolhas em `docs/decisions`.

---

## Fora de escopo

- Adicionar Loki, Grafana, Promtail, Alloy ou qualquer stack local de agregacao.
- Implementar OpenTelemetry, exportacao OTLP, spans ou tracing distribuido real.
- Alterar regras de dominio, use cases, repositorios JDBC ou migrations.
- Logar corpo de requests/responses.
- Logar query string ou parametros de negocio como `sku`, `name`, `priceAmount` ou `categoryId`.
- Criar logs de negocio dentro dos use cases nesta primeira etapa.
- Criar um schema proprio completo de logs para o projeto.

---

## Decisoes aprovadas

### Escopo

A primeira entrega prepara a app para logs agregaveis, mas nao adiciona infraestrutura de observabilidade. Isso mantem o PR pequeno e didatico.

### Formato

Usar ECS/JSON por padrao no console. O Spring Boot 4 ja oferece suporte a structured logging no console, incluindo pares do MDC no JSON. A implementacao deve favorecer configuracao nativa em vez de `logback-spring.xml` customizado.

### Correlacao

Usar `X-Correlation-ID` como contrato HTTP imediato. A app aceita o valor quando ele vem seguro, gera um UUID quando ele esta ausente ou invalido, coloca o valor no MDC e devolve o mesmo header na resposta.

O desenho deve continuar compativel com W3C Trace Context no futuro. Quando OpenTelemetry entrar, `traceparent`, `trace.id` e `span.id` devem complementar o `correlation.id`, nao substituir o contrato didatico do projeto.

### Volume de logs

Registrar apenas um request summary por requisicao concluida. Nao logar body, query string, nem parametros de negocio nesta primeira etapa.

### Padrao por ambiente

Emitir JSON por padrao em todos os ambientes. O objetivo do projeto e ensinar logs agregaveis, entao a experiencia padrao deve mostrar a app ja no formato correto.

---

## Arquitetura

A mudanca fica no adaptador HTTP e na configuracao da aplicacao. Dominio e use cases permanecem sem dependencia de logging, MDC, Servlet API, Spring Web ou formatos de observabilidade.

Pacotes propostos:

```text
src/main/kotlin/com/nexus/shopping/infra/correlation/
  CorrelationIdProvider.kt
src/main/kotlin/com/nexus/shopping/infra/http/
  CorrelationIdFilter.kt
```

O filtro deve:

1. Receber a requisicao HTTP.
2. Ler `X-Correlation-ID`.
3. Validar se o valor e seguro para log.
4. Gerar UUID quando o valor estiver ausente, branco ou invalido.
5. Colocar o valor no MDC com a chave `correlation.id`.
6. Adicionar `X-Correlation-ID` no response.
7. Executar a cadeia HTTP.
8. Registrar um summary log no final.
9. Limpar o MDC em bloco `finally`.

A configuracao em `application.yml` deve habilitar o formato estruturado ECS para console. Se o Spring Boot usar nomes de propriedades diferentes para campos customizados, a implementacao deve seguir o suporte oficial da versao usada no projeto.

---

## Contrato HTTP

Header de entrada:

```http
X-Correlation-ID: <valor>
```

Header de saida:

```http
X-Correlation-ID: <valor-resolvido>
```

Regras:

- Header ausente gera UUID.
- Header em branco gera UUID.
- Header invalido gera UUID.
- Header valido e preservado.
- O tamanho maximo aceito e 128 caracteres.
- Caracteres aceitos: letras, numeros, `.`, `_`, `-` e `:`.

Essa regra evita quebra de linha, caracteres de controle e log injection.

---

## Contrato de log

Cada requisicao concluida deve emitir um evento:

```text
http.request.completed
```

Campos esperados no log estruturado:

- `@timestamp`
- `log.level`
- `service.name`
- `message`
- `correlation.id`
- `http.request.method`
- `url.path`
- `http.response.status_code`
- `event.duration` ou campo equivalente de duracao

Nivel:

- `INFO` para respostas menores que 500, incluindo 4xx.
- `ERROR` para respostas 5xx.

O request summary nao deve conter:

- body
- query string
- headers sensiveis
- stack trace
- SQL
- parametros de negocio

---

## Evolucao futura

Quando o projeto adicionar microservices, cada app deve seguir o mesmo contrato minimo:

- logs JSON no stdout
- ECS como schema base
- `service.name`
- `correlation.id`
- request summary HTTP equivalente
- propagacao de `X-Correlation-ID`

Quando tracing distribuido entrar, o caminho esperado e OpenTelemetry com W3C Trace Context:

- aceitar e propagar `traceparent`
- registrar `trace.id` e `span.id`
- manter `correlation.id` para consultas didaticas e compatibilidade

Quando Grafana/Loki entrar, a stack deve consumir stdout dos containers sem exigir mudanca no codigo da aplicacao.

---

## Estrategia de testes

Testes recomendados:

1. Request sem `X-Correlation-ID` retorna `X-Correlation-ID` com UUID gerado.
2. Request com `X-Correlation-ID` valido preserva o valor no response.
3. Request com `X-Correlation-ID` em branco retorna UUID novo.
4. Request com `X-Correlation-ID` inseguro retorna UUID novo.
5. A unidade que resolve correlation-id rejeita valores acima de 128 caracteres.
6. O filtro limpa o MDC depois da requisicao, quando isso puder ser validado de forma simples.
7. Se capturar logs for simples no setup atual, validar que `http.request.completed` inclui `correlation.id`.

Nao e necessario testar todo o JSON gerado pelo Spring Boot campo a campo. A prioridade e validar o contrato HTTP, a resolucao segura do correlation-id e a configuracao documentada.

Comando principal:

```bash
rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew build
```

---

## Documentacao

Criar `docs/decisions/2026-06-29-prd-structured-logging-correlation-id.md` com:

- problema
- objetivo
- alternativas avaliadas
- decisao
- contrato minimo de campos
- consequencias
- itens fora de escopo
- evolucao futura para Grafana/Loki/OpenTelemetry

O documento deve ficar em portugues e ASCII, seguindo o estilo dos registros existentes em `docs/decisions`.

---

## Criterios de aceite

- A aplicacao emite logs estruturados ECS/JSON no console por padrao.
- Toda resposta HTTP inclui `X-Correlation-ID`.
- Requests sem correlation-id recebem UUID gerado.
- Requests com correlation-id valido preservam o valor.
- Requests com correlation-id invalido recebem UUID novo.
- O MDC contem `correlation.id` durante a requisicao e e limpo ao final.
- Cada request concluida gera um summary log unico.
- O summary log nao inclui body, query string nem parametros de negocio.
- A decisao arquitetural esta documentada em `docs/decisions`.
- O build passa com o Gradle Wrapper documentado no projeto.
