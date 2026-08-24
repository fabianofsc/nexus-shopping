# Relatorio de cobertura da Tarefa 5

## Escopo

- Checkout aprovado com dispatch que retorna submissao `FAILED`: a resposta permanece `CONFIRMED` e o journal observado pelo gateway fica `FAILED`.
- Checkout aprovado com falha de Billing ou de Shipping: o erro continua propagado, a submissao reservada permanece `PENDING` e nao ha dispatch.
- Reconciliacao aprovada com dispatch `FAILED`: a submissao retornada pelo journal fica `FAILED`.
- Reconciliacao com falha de Billing ou Shipping: a submissao daquele pedido permanece `PENDING`, nao ha dispatch e o outcome rejeitado seguinte ainda libera o estoque.

## TDD e limite da cobertura

Esta alteracao cobre ramos que ja estavam implementados na Tarefa 5. Por isso, os testes de caracterizacao passaram na primeira execucao apos serem escritos; nao houve codigo de producao a implementar. As assercoes observam estado do journal simulado pelo `NotificationGateway`, nao apenas a ordem de chamadas dos fakes.

## Verificacao

Passou a suite focada:

```sh
rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests '*ExecuteCheckoutUseCaseTest' --tests '*PaymentReconciliationUseCaseTest'
```
