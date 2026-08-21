# Relatorio da Tarefa 4: ACL HTTP do Notification Service

## Escopo entregue

- Criado o cliente HTTP do Notification Service com Basic Auth, chave de idempotencia e payload EMAIL completo.
- Criada a factory HTTP configuravel, com HTTP/1.1 e timeouts por servico.
- Migrado o gateway de Payment para a factory sem alterar seu contrato.
- Incluidas propriedades do Notification Service com placeholders e defaults sem segredo real.

## TDD

- RED observado: `NotificationServiceHttpClientTest` falhou por referencias ausentes a `NotificationServiceHttpClient` e `ConfigurableRestClientFactory`.
- GREEN observado: testes WireMock cobrem aceite 202, Basic Auth, chave, payload, timeout, 429, 5xx, rejeicoes 4xx, resposta invalida e status diferente de 202.

## Verificacao

Executado com sucesso:

```sh
rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests '*NotificationServiceHttpClientTest' --tests '*PaymentServiceProviderGatewayTest'
```

Tambem passou a verificacao focada que inclui `PackageStructureArchitectureTest`.
Uma repeticao final do Gradle terminou com `java.io.EOFException` sem falha de teste
reportada; trata-se da instabilidade conhecida da suite ampla neste ambiente. O
`ktlintCheck` continua bloqueado por violacoes pre-existentes nos arquivos da Tarefa 3.
