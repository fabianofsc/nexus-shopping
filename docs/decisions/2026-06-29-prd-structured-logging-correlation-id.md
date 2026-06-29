# ADR: Usar ECS JSON e X-Correlation-ID para logs agregaveis

**Status:** Aceita
**Data:** 2026-06-29
**Spec relacionada:** `docs/superpowers/specs/2026-06-29-structured-logging-correlation-id-design.md`

---

## Contexto

O Nexus Shopping e um laboratorio didatico de performance e arquitetura. A aplicacao atual e uma API Spring Boot/Kotlin, mas o projeto deve evoluir para demonstrar microservices com apps em stacks e linguagens diferentes.

Hoje a aplicacao ainda nao tem um contrato de logs voltado para agregacao. Isso dificulta consultas futuras no Grafana e tambem deixa aberta uma decisao importante: como correlacionar eventos de uma mesma operacao quando a arquitetura evoluir para servicos distribuidos.

Queremos preparar a app atual para esse futuro sem adicionar uma stack de observabilidade completa nesta etapa.

---

## Objetivos

1. Emitir logs em formato estruturado e agregavel.
2. Usar um schema portavel entre linguagens.
3. Ter um identificador de correlacao por requisicao HTTP.
4. Manter a solucao alinhada com a stack Spring Boot/Kotlin atual.
5. Evitar introduzir tracing distribuido antes de haver multiplos servicos.
6. Deixar um caminho claro para Grafana/Loki e OpenTelemetry no futuro.

---

## Alternativas consideradas

### 1. Spring Boot structured logging com ECS e filtro de correlation-id

Usar o suporte nativo do Spring Boot para logs estruturados no console em formato ECS. Adicionar um filtro HTTP pequeno para resolver `X-Correlation-ID`, colocar o valor no MDC, devolve-lo no response e registrar um resumo da requisicao.

Pontos positivos:

- Aproveita a stack atual.
- Evita dependencia extra de encoder na primeira etapa.
- ECS e um schema conhecido e replicavel por outras linguagens.
- MDC permite incluir `correlation.id` no JSON sem acoplar dominio ou use cases.
- O PR fica pequeno e focado.

Trade-off:

- Ainda nao ha trace/span IDs nem visualizacao no Grafana local.

### 2. OpenTelemetry e Micrometer Tracing agora

Adicionar tracing distribuido completo ja nesta etapa, com propagacao W3C Trace Context e possivel exportacao OTLP.

Pontos positivos:

- E o caminho tecnico mais completo para microservices.
- Ja prepararia `trace.id` e `span.id`.

Trade-offs:

- Introduz conceitos e configuracao antes de existir mais de um servico.
- Aumenta o escopo do PR.
- Pode desviar o foco didatico inicial, que e log estruturado agregavel.

### 3. Logback customizado com schema proprio

Criar `logback-spring.xml` e definir manualmente um JSON do projeto.

Pontos positivos:

- Controle total sobre nomes e formato dos campos.

Trade-offs:

- Cria um padrao proprietario sem necessidade clara.
- Aumenta manutencao.
- Dificulta a repeticao em outras stacks.
- Ignora suporte nativo ja existente no Spring Boot.

---

## Decisao

Usar a alternativa 1: **Spring Boot structured logging em ECS/JSON por padrao, com `X-Correlation-ID` resolvido por filtro HTTP e publicado no MDC como `correlation.id`**.

A aplicacao deve emitir logs JSON no stdout/console por padrao. Isso favorece a futura coleta por containers e ferramentas como Loki/Grafana sem exigir mudanca no codigo.

O contrato HTTP inicial de correlacao sera:

```http
X-Correlation-ID: <valor>
```

Regras:

- Se o request trouxer `X-Correlation-ID` valido, preservar o valor.
- Se o header estiver ausente, branco ou invalido, gerar UUID.
- Sempre devolver `X-Correlation-ID` na resposta.
- Colocar o valor resolvido no MDC como `correlation.id`.
- Aceitar apenas valores seguros para log: letras, numeros, `.`, `_`, `-` e `:`, com ate 128 caracteres.

A aplicacao tambem deve emitir um unico log de resumo por requisicao concluida, com mensagem estavel:

```text
http.request.completed
```

Campos minimos esperados:

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

- `INFO` para respostas menores que 500.
- `ERROR` para respostas 5xx.

O resumo da requisicao nao deve incluir body, query string, headers sensiveis, stack trace, SQL ou parametros de negocio.

---

## Relacao com W3C Trace Context

Esta decisao nao implementa tracing distribuido completo agora.

Mesmo assim, o desenho deve ser compativel com W3C Trace Context. Quando OpenTelemetry for introduzido, a aplicacao podera aceitar e propagar `traceparent` e `tracestate`, alem de registrar `trace.id` e `span.id`.

O `correlation.id` nao deve ser removido nesse futuro. Ele continua sendo um identificador didatico e simples para consultas, enquanto trace/span IDs representam a telemetria distribuida completa.

---

## Consequencias

Pontos positivos:

- Logs ficam prontos para agregacao futura.
- A experiencia padrao da app ja ensina o formato usado em producao.
- O contrato `X-Correlation-ID` e simples para clientes e outras apps.
- A solucao preserva a arquitetura: dominio e use cases nao conhecem logging, MDC ou HTTP.
- O caminho para OpenTelemetry fica claro sem aumentar o escopo atual.

Trade-offs:

- Logs JSON sao menos confortaveis para leitura manual no terminal.
- Ainda nao havera dashboard ou consulta local no Grafana.
- ECS define nomes de campos que podem parecer menos naturais do que um schema proprio.

Alternativas rejeitadas:

- OpenTelemetry agora foi rejeitado por antecipar uma complexidade que pertence a uma etapa com multiplos servicos.
- Schema proprio em Logback foi rejeitado porque o Spring Boot ja oferece structured logging e porque o projeto quer um contrato replicavel por outras linguagens.

---

## Fora de escopo

- Subir Grafana, Loki, Promtail ou Alloy.
- Exportar logs, traces ou metricas para um backend externo.
- Implementar spans, trace IDs ou sampling.
- Logar payloads HTTP.
- Logar query string ou parametros de negocio.
- Criar uma taxonomia completa de eventos de negocio.

---

## Evolucao futura

Quando o projeto adicionar mais apps, cada servico deve seguir o mesmo contrato minimo:

- JSON no stdout.
- ECS como schema base.
- `service.name` preenchido.
- `correlation.id` presente.
- `X-Correlation-ID` propagado entre servicos.
- Request summary HTTP equivalente.

Quando a stack de observabilidade for adicionada, Loki/Grafana deve consumir os logs dos containers sem exigir mudancas no codigo da aplicacao.

Quando tracing distribuido for necessario, OpenTelemetry deve adicionar `trace.id`, `span.id` e propagacao W3C Trace Context mantendo `correlation.id` como campo de correlacao simples.
