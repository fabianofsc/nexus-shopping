# Relatorio - Task 1: nucleo Order e contratos de aplicacao

## Escopo entregue

- Contexto `order` com `Order`, snapshots imutaveis de cliente/endereco/item,
  moeda e todos os estados previstos para evolucoes futuras.
- Total derivado dos itens e cancelamento restrito a
  `WAITING_PAYMENT -> CANCELLED`.
- Command de checkout, excecoes tipadas, porta outbound pequena e use cases
  puros para checkout, consulta, listagem e cancelamento.
- Idempotencia por `(customerId, idempotencyKey)`: a porta cria ou devolve o
  pedido existente atomicamente; o use case compara o fingerprint para
  distinguir replay de conflito.
- Nenhum adaptador HTTP/JPA, migration ou alteracao de Cart foi criado.

## TDD

1. `OrderTest` foi escrito antes do dominio. O primeiro teste falhou na
   compilacao por tipos `Order`/snapshots/status inexistentes; apos a
   implementacao minima, passou.
2. `OrderUseCasesTest` foi escrito antes de commands, porta, excecoes e use
   cases. O ciclo RED falhou por referencias inexistentes; o GREEN cobriu
   criacao, replay, conflito de fingerprint, consulta, listagem e
   cancelamento com fake em memoria.
3. Casos para carrinho snapshot vazio e snapshot de outro cliente falharam em
   RED e passaram apos as validacoes correspondentes.
4. O caso de chave de idempotencia em branco falhou em RED e passou apos a
   validacao explicita.

## Verificacao

- Focado: `./gradlew test --tests 'com.nexus.shopping.order.OrderUseCasesTest' --tests 'com.nexus.shopping.order.domain.OrderTest'` - passou (12 testes).
- Lint: `./gradlew ktlintCheck` - passou.
- Suite completa: `./gradlew test --console=plain` - passou em aproximadamente
  um minuto.
- Revisao manual: sem imports de Spring, JPA, Hibernate, Spring Data ou HTTP
  em `order/domain` e `order/application`; diff sem espacos invalidos; escopo
  limitado ao nucleo Order.

## Observacao de execucao

Uma tentativa anterior da suite completa falhou ao finalizar com
`NoSuchFileException` para `build/test-results/test/binary/in-progress-results-generic.bin`,
causada por tentativas Gradle sobrepostas durante a verificacao. A repeticao
serial posterior concluiu com `BUILD SUCCESSFUL`.
