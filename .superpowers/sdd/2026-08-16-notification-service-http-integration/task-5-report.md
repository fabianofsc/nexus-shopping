# Relatorio da Tarefa 5: aprovacao e reserva atomicas

## Escopo entregue

- O checkout aprovado aplica o resultado de pagamento e reserva a submissao de notificacao na mesma `TransactionPort`.
- Apos o commit, executa Billing, Shipping e dispatch nessa ordem.
- A reconciliacao recebeu `TransactionPort`: aprovacao/reserva sao atomicas e a liberacao de estoque de rejeicoes permanece transacional.
- Billing ou Shipping continuam propagando erros e interrompem o dispatch; o dispatch remoto conserva o isolamento de falhas do journal.
- O replay preserva uma unica submissao persistida pela `notification_key`.

## TDD

- RED observado no checkout: os testes de ordem e de falha de Billing falharam porque o fluxo ainda chamava a notificacao legada apos aplicar o pagamento.
- RED observado na reconciliacao: a compilacao falhou porque o use case ainda nao aceitava `TransactionPort`.
- GREEN observado: a suite focada passou para checkout, reconciliacao e a integracao H2 do journal.

## Verificacao

Passou a verificacao focada:

```sh
rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew cleanTest test --tests '*NotificationSubmissionCheckoutIntegrationTest' --tests '*ExecuteCheckoutUseCaseTest' --tests '*PaymentReconciliationUseCaseTest'
```

`ktlintCheck` permanece bloqueado por violacoes pre-existentes nos arquivos das Tarefas 1 e 3. Nao foram alterados fora do escopo desta tarefa.
