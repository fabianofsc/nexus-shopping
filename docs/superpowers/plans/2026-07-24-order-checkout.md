# Order checkout idempotente

## Objetivo

Implementar o contexto Order como o registro historico criado pelo checkout de
um carrinho ativo. Esta entrega termina em WAITING_PAYMENT e nao integra
Payment nem Notification.

## Regras globais

- Manter a arquitetura hexagonal: adapter -> application -> domain.
- Domain e application nao importam JPA, Hibernate, Spring Data ou HTTP.
- Usar TDD: cada comportamento novo precisa de teste em vermelho antes do
  codigo de producao correspondente.
- O checkout exige Idempotency-Key, e a chave e unica por customerId.
- Mesma chave e mesmo payload retornam o pedido original; payload diferente
  retorna 409. O pedido e criado uma unica vez sob concorrencia.
- O checkout bloqueia o carrinho ACTIVE, cria snapshots historicos e o marca
  CHECKED_OUT na mesma transacao. Carrinho vazio, ausente ou nao ACTIVE falha.
- O cancelamento so permite WAITING_PAYMENT -> CANCELLED e nao reabre o
  carrinho.
- Payment, confirmacao, falha de pagamento e Notification estao fora de
  escopo.

## Task 1: nucleo Order e contratos de aplicacao

Criar os tipos de dominio Order, snapshots, itens e estados, mais commands,
excecoes, portas e use cases puros para checkout, consulta, listagem e
cancelamento. Cobrir regras de estado, total derivado e idempotencia com fakes
em testes unitarios. Nao criar adaptadores HTTP, JPA ou migration nesta task.

## Task 2: persistencia e transacao de checkout

Criar a migration portavel de orders e order_items e os adaptadores JPA de
Order. Introduzir a porta de transacao e os metodos de carrinho necessarios
para bloquear um ACTIVE e concluir checkout de forma atomica. Garantir unicidade
de cartId e (customerId, idempotencyKey), replay e conflito de fingerprint.
Cobrir a persistencia e concorrencia com H2 real.

## Task 3: API HTTP e integracao com Cart

Criar DTOs, controllers e mapeamentos para checkout, leitura, listagem e
cancelamento. Mapear erros de conflito para Problem Details 409 e impedir
mutacoes de itens em carrinhos fora de ACTIVE. Cobrir os contratos HTTP e o
fluxo completo.

## Verificacao final

Executar ktlintCheck e build com o Gradle Wrapper. Revisar a branch completa
contra main e confirmar que Payment e Notification continuam desacoplados.
