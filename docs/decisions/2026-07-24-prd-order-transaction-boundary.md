# ADR: Manter o limite transacional do checkout atras de uma porta

**Status:** Aceita
**Data:** 2026-07-24

---

## Contexto

O checkout de `Order` executa uma unica operacao de negocio que envolve os contextos `Cart` e `Order`:

```text
bloquear o carrinho ACTIVE
-> verificar idempotencia
-> criar o pedido
-> marcar o carrinho como CHECKED_OUT
```

Essas etapas precisam confirmar ou reverter juntas. O pedido nao pode permanecer criado se o carrinho nao puder ser fechado, e o carrinho nao pode ser fechado sem que exista o pedido correspondente.

Os adapters JPA possuem metodos anotados com `@Transactional`, mas essas anotacoes, quando chamadas sem uma transacao externa, definem limites para cada operacao de persistencia. Elas nao estabelecem por si so uma transacao unica para toda a orquestracao do checkout.

## Alternativas consideradas

### 1. Confiar apenas em `@Transactional` nos metodos dos repositories/adapters

Cada chamada de persistencia abre e encerra sua propria transacao quando nao existe uma transacao externa.

Trade-off: uma falha depois de criar o pedido e antes de fechar o carrinho pode deixar o fluxo parcialmente confirmado.

### 2. Anotar diretamente o metodo do use case com `@Transactional`

Essa opcao cria o limite correto para toda a operacao, desde que o metodo seja chamado pelo proxy Spring.

Trade-off: o pacote `application` passa a conhecer a API transacional do Spring. Isso mistura a orquestracao do caso de uso com a tecnologia que realiza a transacao e dificulta executar o caso de uso sem Spring nos testes unitarios.

### 3. Expor uma porta de transacao e implementa-la no adapter JPA

O caso de uso recebe `TransactionPort` e delimita a unidade atomica por meio de `inTransaction { ... }`. O adapter `JpaTransactionAdapter` implementa a porta e aplica `@Transactional` no ponto em que Spring/JPA sao conhecidos.

Trade-off: ha uma interface e um adapter adicionais para uma responsabilidade pequena.

## Decisao

Usar a alternativa 3.

`TransactionPort` pertence a `order/application/port/outbound` e expressa a necessidade da aplicacao: executar uma sequencia de trabalho de modo atomico. `JpaTransactionAdapter` pertence a `order/adapter/outbound/jpa` e fornece essa garantia usando Spring:

```kotlin
@Component
class JpaTransactionAdapter : TransactionPort {
    @Transactional
    override fun <T> inTransaction(block: () -> T): T = block()
}
```

O checkout inicia uma unica transacao ao entrar nessa porta. Os metodos transacionais dos adapters de `Cart` e `Order` participam da mesma transacao existente. Se qualquer etapa falhar, Spring faz rollback de todas as alteracoes da unidade de trabalho.

O `@Transactional` nao e substituido nem ignorado: ele e usado no adapter que materializa a porta. A decisao e sobre onde a anotacao fica e qual trecho do fluxo ela engloba.

## Consequencias

- `CheckoutOrderUseCase` e `CancelOrderUseCase` nao importam `org.springframework.transaction`.
- Testes unitarios podem usar uma implementacao imediata de `TransactionPort`, sem subir Spring ou um banco de dados.
- Testes de integracao confirmam que a implementacao JPA aplica rollback e locks dentro da mesma transacao.
- O limite transacional fica explicito no fluxo de aplicacao, em vez de ficar implicito na combinacao de transacoes menores dos adapters.
- Novos casos de uso que precisam coordenar mais de um adapter podem reutilizar esta porta; operacoes de persistencia isoladas continuam usando as transacoes locais de seus adapters.
