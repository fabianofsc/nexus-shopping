# Monolith Baseline (manual do aluno)

Este documento descreve o **baseline monolito de e-commerce** (branch
`monolith-first`) do Nexus Shopping: o mapa de contextos, os estados, as regras de
idempotencia, como rodar e a trilha de evolucao para sistemas distribuidos. E a
referencia para replicar o mesmo comportamento em Node/TypeScript, Java ou Python.

O contrato HTTP canonico e o [`docs/api/openapi.yaml`](../../docs/api/openapi.yaml);
o Kotlin e a implementacao de referencia e deve seguir esse contrato.

## Mapa de contextos e fronteiras

Monolito modular hexagonal (`adapter -> application -> domain`), com um banco
compartilhado:

| Contexto | Responsabilidade | Modelo |
| --- | --- | --- |
| Product | Catalogo, descoberta e leitura de produtos | Product, Category, Brand |
| Customer | Dados cadastrais do comprador | Customer, Contact, Address |
| Cart | Intencao temporaria de compra | Cart, CartItem, ProductSummary |
| Order | Compromisso comercial (snapshot historico) | Order, CustomerSnapshot, ShippingAddressSnapshot, OrderItemSnapshot |
| Payment | Processar pagamento (abstrai o PSP) | PaymentAttempt |
| Notification | Comunicar eventos ao cliente | Notification |
| Inventory | Disponibilidade e baixa de estoque | StockMovement, products.inventory_quantity |
| Billing | Emitir documentos comerciais | Invoice futura; neste baseline apenas registra a emissao |
| Shipping | Calcular frete e despachar remessa | Shipment futura; neste baseline apenas registra os efeitos |

Regras de fronteira:

- `Product` no catalogo, `ProductSummary` no carrinho e `OrderItemSnapshot` no pedido
  **nao sao o mesmo modelo global**.
- `Customer` e dono dos dados cadastrais; `Order` guarda **snapshot historico** imutavel.
- `Order` nao consulta `Customer` na criacao do pedido: o checkout envia os snapshots.
- `Payment` nao importa `Order`; usa `referenceId` opaco (`checkout:<orderId>`).
- `Inventory` nao importa os demais contextos; opera `products.inventory_quantity`.
- `Billing` e `Shipping` nao importam os outros contextos; recebem snapshots por ACLs de Checkout.

## Billing e Shipping no caminho aprovado

Depois de Payment aprovado e Order confirmado, Checkout chama Billing para
registrar a emissao da Invoice e depois chama Shipping para registrar o calculo
do frete e o despacho. A confirmacao por Notification continua sendo a etapa
seguinte do processo.

Os adapters de Billing e Shipping apenas registram esses efeitos no baseline.
Nao ha Invoice ou Shipment persistidos, migration, endpoint, integracao fiscal,
transportadora, rastreio ou custo de frete no pedido. Como esses contextos ainda
nao tem persistencia ou idempotencia, um replay aprovado pode repetir seus logs.

## Estados do pedido e pagamento

```
Order:  WAITING_PAYMENT -> PAYMENT_PROCESSING -> CONFIRMED
                          \-> PAYMENT_FAILED (recuperavel) -> CANCELLED
```

- Cancelamento so e permitido a partir de `WAITING_PAYMENT`.
- `PaymentAttempt`: `REQUESTED -> APPROVED | REJECTED`.
- `Notification`: `PENDING -> SENDING -> SENT | FAILED`.

## Idempotencia e replay

- Checkout exige o cabecalho `Idempotency-Key`. Reenviar a mesma chave com o mesmo
  payload **reproduz** (replay) o pedido existente (HTTP 200) sem novo dispatch de
  pagamento, sem nova baixa de estoque e sem nova notificacao.
- Mesma chave com payload diferente -> `409 Conflict`.
- O fingerprint da autorizacao de pagamento (HMAC do token + chave) protege o replay.
- O estoque e baixado uma unica vez na criacao do pedido; e liberado em `PAYMENT_FAILED`
  ou no cancelamento (nao ha double-release, pois rejected nao e cancelavel).

## Como rodar e validar

Requisitos: Java 21, Docker, Gradle Wrapper.

```bash
docker compose up -d postgres redis
./gradlew bootRun
```

Health:

```bash
curl http://localhost:8080/actuator/health
```

Demonstracao ponta a ponta:

```bash
./scripts/e2e-monolith-demo.sh
```

Testes (H2 + seeds reduzidos):

```bash
env GRADLE_USER_HOME=.../.gradle-local ./gradlew build
```

## Endpoints principais

```text
GET    /products?categoryId=|name=&page=&size=
GET    /products/{id}
POST   /products
PATCH  /products/{id}             (preco)
PATCH  /products/{id}/details
POST   /products/{id}/archive
GET    /brands
POST   /brands
GET    /categories
POST   /categories
PATCH  /categories/{id}/status
POST   /customers
GET    /customers/{id}
GET    /customers/{customerId}/address
PUT    /customers/{customerId}/address
GET    /customers/{customerId}/cart
POST   /customers/{customerId}/cart/items
PUT    /customers/{customerId}/cart/items/{productId}
DELETE /customers/{customerId}/cart/items/{productId}
DELETE /customers/{customerId}/cart/items
POST   /customers/{customerId}/cart/checkout   (+ Idempotency-Key)
GET    /customers/{customerId}/orders
GET    /customers/{customerId}/orders/{orderId}
POST   /customers/{customerId}/orders/{orderId}/cancel
POST   /notifications
GET    /notifications?customerId=&page=&size=
GET    /notifications/{id}
```

Detalhes e schemas em [`docs/api/openapi.yaml`](../../docs/api/openapi.yaml).

> **Manutencao do contrato:** o `openapi.yaml` e a fonte de verdade do contrato.
> Qualquer evolucao da API **deve** atualizar esse arquivo no mesmo lote.

## Trilha de evolucao (distribuicao fisica)

Ordem recomendada para os alunos implementarem, escolhendo a stack que dominam:

1. **Extrair Payment** como servico (o DummyPay ja existe; Payment vira o servico que o
   consome). O Nexus chama por HTTP/ACL; retry e reconciliation entram aqui.
2. **Event-driven**: introduzir um broker; `Notification` vira o primeiro consumidor
   assincrono (outbox para garantir entrega).
3. **API Gateway** + propagacao de identidade (Auth).
4. **Observabilidade distribuida**: tracing e logs centralizados.
5. **Saga/compensacao** para o checkout entre servicos.
6. **Banco por servico** (ownership de dados; remover o banco compartilhado).

A mensagem didatica central e:

```text
Decomposicao de dominio primeiro.
Distribuicao fisica depois.
```

O baseline ja mantem os Bounded Contexts explicitos no codigo e no contrato, o que
torna cada extracao um passo incremental e testavel.
