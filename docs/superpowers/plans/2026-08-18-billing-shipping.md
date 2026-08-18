# Billing e Shipping Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (- [ ]) syntax for tracking.

**Goal:** Adicionar Billing e Shipping e executa-los em sequencia apos Payment aprovado no processo Checkout.

**Architecture:** Billing recebe um snapshot de pedido e delega a emissao de Invoice para uma porta. Shipping recebe outro snapshot e delega calculo seguido de despacho a uma porta de transportadora. Checkout os coordena apenas por gateways e ACLs.

**Tech Stack:** Kotlin, Java 21, Spring Boot, Gradle, Kotlin Test, SLF4J/Logback e ArchUnit.

**Spec:** docs/superpowers/specs/2026-08-18-billing-shipping-design.md

## Global Constraints

- Criar a branch de implementacao a partir de monolith-first, nunca de main.
- Validar Billing e Shipping no monolito modular antes de qualquer tentativa de
  integracao com main.
- Tratar a futura promocao para main como trabalho separado: reconciliar com o
  Payment extraido, sem descartar nem sobrescrever sua evolucao.
- Manter adapter -> application -> domain.
- Nao criar entidades, JPA, migrations, endpoints, OpenAPI ou alteracoes em Notification.
- Nao usar Fake, Mock, Simulated ou Logging em nomes de producao.
- Billing e Shipping nao dependem de Checkout, Order, Payment, Notification ou um do outro.
- Somente checkout.adapter.outbound.acl pode depender de seus input ports.
- Os adapters registram apenas a referencia do pedido.
- Billing e Shipping executam em todo APPROVED, inclusive replay aprovado; duplicacao de log e limite aceito sem persistencia.
- Usar env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew.

---

## Estrutura de arquivos

| Arquivo | Responsabilidade |
| --- | --- |
| billing/application/{command,port,inbound,port/outbound,usecase} | Comando, portas e caso de uso de Invoice. |
| billing/adapter/outbound/issuer/InvoiceIssuerAdapter.kt | Log de emissao da Invoice. |
| shipping/application/{command,port,inbound,port/outbound,usecase} | Comando, portas e caso de uso de Shipping. |
| shipping/adapter/outbound/carrier/CarrierAdapter.kt | Logs de calculo e despacho. |
| checkout/application/model/CheckoutModels.kt | Comandos de Checkout para os gateways. |
| checkout/application/port/outbound/{BillingGateway,ShippingGateway}.kt | Saidas de Checkout. |
| checkout/adapter/outbound/acl/{BillingGatewayAdapter,ShippingGatewayAdapter}.kt | Traducao para os contextos novos. |
| checkout/application/usecase/ExecuteCheckoutUseCase.kt | Sequencia aprovada. |
| checkout/adapter/config/CheckoutConfiguration.kt | Wiring dos gateways. |
| testes de billing, shipping e checkout | Delegacao, logs, ordem e replay. |
| PackageStructureArchitectureTest.kt e guias | Fronteiras e documentacao. |

### Task 1: Criar Billing sem persistencia

**Files:**
- Create: src/main/kotlin/com/nexus/shopping/billing/application/command/IssueInvoiceCommand.kt
- Create: src/main/kotlin/com/nexus/shopping/billing/application/port/inbound/IssueInvoiceInputPort.kt
- Create: src/main/kotlin/com/nexus/shopping/billing/application/port/outbound/InvoiceIssuerPort.kt
- Create: src/main/kotlin/com/nexus/shopping/billing/application/usecase/IssueInvoiceUseCase.kt
- Create: src/main/kotlin/com/nexus/shopping/billing/adapter/outbound/issuer/InvoiceIssuerAdapter.kt
- Test: src/test/kotlin/com/nexus/shopping/billing/IssueInvoiceUseCaseTest.kt
- Test: src/test/kotlin/com/nexus/shopping/billing/adapter/outbound/issuer/InvoiceIssuerAdapterTest.kt

**Interfaces:**
- Produces: IssueInvoiceInputPort.issue(command: IssueInvoiceCommand): Unit.
- Consumes: nenhum tipo de outro contexto.

- [ ] **Step 1: Write the failing test**

~~~kotlin
@Test
fun issuesInvoiceThroughIssuerPort() {
    var received: IssueInvoiceCommand? = null
    val useCase = IssueInvoiceUseCase(object : InvoiceIssuerPort {
        override fun issue(command: IssueInvoiceCommand) { received = command }
    })
    val command = invoiceCommand()

    useCase.issue(command)

    assertEquals(command, received)
}
~~~

- [ ] **Step 2: Run test to verify it fails**

Run: env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests '*IssueInvoiceUseCaseTest'

Expected: FAIL porque Billing ainda nao existe.

- [ ] **Step 3: Write minimal implementation**

~~~kotlin
interface IssueInvoiceInputPort {
    fun issue(command: IssueInvoiceCommand)
}

interface InvoiceIssuerPort {
    fun issue(command: IssueInvoiceCommand)
}

@Service
class IssueInvoiceUseCase(
    private val issuer: InvoiceIssuerPort,
) : IssueInvoiceInputPort {
    override fun issue(command: IssueInvoiceCommand) = issuer.issue(command)
}
~~~

Definir IssueInvoiceCommand com orderId, orderReference, snapshots proprios de
cliente/endereco/itens e totalAmount. Nao importar tipos de Order ou Checkout.

- [ ] **Step 4: Implement and test the adapter**

~~~kotlin
@Component
class InvoiceIssuerAdapter : InvoiceIssuerPort {
    override fun issue(command: IssueInvoiceCommand) {
        logger.info("billing.invoice.issued order_reference={}", command.orderReference)
        // TODO: substituir o log pela integracao com o emissor fiscal.
    }
}
~~~

No teste, anexar ListAppender ao logger, chamar issue(invoiceCommand()) e
afirmar uma unica mensagem billing.invoice.issued com checkout:1, sem CPF/CNPJ
ou endereco.

- [ ] **Step 5: Run tests and commit**

Run: env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests '*IssueInvoiceUseCaseTest' --tests '*InvoiceIssuerAdapterTest'

~~~bash
git add src/main/kotlin/com/nexus/shopping/billing src/test/kotlin/com/nexus/shopping/billing
git commit -m "feat: add billing invoice skeleton"
~~~

### Task 2: Criar Shipping sem persistencia

**Files:**
- Create: src/main/kotlin/com/nexus/shopping/shipping/application/command/ProcessShippingCommand.kt
- Create: src/main/kotlin/com/nexus/shopping/shipping/application/port/inbound/ProcessShippingInputPort.kt
- Create: src/main/kotlin/com/nexus/shopping/shipping/application/port/outbound/CarrierPort.kt
- Create: src/main/kotlin/com/nexus/shopping/shipping/application/usecase/ProcessShippingUseCase.kt
- Create: src/main/kotlin/com/nexus/shopping/shipping/adapter/outbound/carrier/CarrierAdapter.kt
- Test: src/test/kotlin/com/nexus/shopping/shipping/ProcessShippingUseCaseTest.kt
- Test: src/test/kotlin/com/nexus/shopping/shipping/adapter/outbound/carrier/CarrierAdapterTest.kt

**Interfaces:**
- Produces: ProcessShippingInputPort.process(command: ProcessShippingCommand): Unit.
- Consumes: nenhum tipo de outro contexto.

- [ ] **Step 1: Write the failing test**

~~~kotlin
@Test
fun calculatesFreightBeforeDispatchingShipment() {
    val calls = mutableListOf<String>()
    val useCase = ProcessShippingUseCase(object : CarrierPort {
        override fun calculateFreight(command: ProcessShippingCommand) { calls += "calculate" }
        override fun dispatch(command: ProcessShippingCommand) { calls += "dispatch" }
    })

    useCase.process(shippingCommand())

    assertEquals(listOf("calculate", "dispatch"), calls)
}
~~~

- [ ] **Step 2: Run test to verify it fails**

Run: env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests '*ProcessShippingUseCaseTest'

Expected: FAIL porque Shipping ainda nao existe.

- [ ] **Step 3: Write minimal implementation**

~~~kotlin
interface CarrierPort {
    fun calculateFreight(command: ProcessShippingCommand)
    fun dispatch(command: ProcessShippingCommand)
}

@Service
class ProcessShippingUseCase(
    private val carrier: CarrierPort,
) : ProcessShippingInputPort {
    override fun process(command: ProcessShippingCommand) {
        carrier.calculateFreight(command)
        carrier.dispatch(command)
    }
}
~~~

Definir ProcessShippingCommand com pedido, endereco, itens e total sob tipos
proprios de Shipping, sem tipos de Order ou Checkout.

- [ ] **Step 4: Implement and test the adapter**

~~~kotlin
@Component
class CarrierAdapter : CarrierPort {
    override fun calculateFreight(command: ProcessShippingCommand) {
        logger.info("shipping.freight.calculated order_reference={}", command.orderReference)
        // TODO: substituir o log pela integracao com a transportadora.
    }

    override fun dispatch(command: ProcessShippingCommand) {
        logger.info("shipping.shipment.dispatched order_reference={}", command.orderReference)
    }
}
~~~

No teste com ListAppender, afirmar as duas mensagens na ordem indicada e que
nenhuma contem dados de cliente.

- [ ] **Step 5: Run tests and commit**

Run: env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests '*ProcessShippingUseCaseTest' --tests '*CarrierAdapterTest'

~~~bash
git add src/main/kotlin/com/nexus/shopping/shipping src/test/kotlin/com/nexus/shopping/shipping
git commit -m "feat: add shipping dispatch skeleton"
~~~

### Task 3: Orquestrar os contextos por ACL apos pagamento aprovado

**Files:**
- Modify: src/main/kotlin/com/nexus/shopping/checkout/application/model/CheckoutModels.kt
- Create: src/main/kotlin/com/nexus/shopping/checkout/application/port/outbound/BillingGateway.kt
- Create: src/main/kotlin/com/nexus/shopping/checkout/application/port/outbound/ShippingGateway.kt
- Create: src/main/kotlin/com/nexus/shopping/checkout/adapter/outbound/acl/BillingGatewayAdapter.kt
- Create: src/main/kotlin/com/nexus/shopping/checkout/adapter/outbound/acl/ShippingGatewayAdapter.kt
- Modify: src/main/kotlin/com/nexus/shopping/checkout/application/usecase/ExecuteCheckoutUseCase.kt
- Modify: src/main/kotlin/com/nexus/shopping/checkout/adapter/config/CheckoutConfiguration.kt
- Modify: src/test/kotlin/com/nexus/shopping/checkout/ExecuteCheckoutUseCaseTest.kt
- Modify: src/test/kotlin/com/nexus/shopping/checkout/ExecuteCheckoutIntegrationTest.kt
- Create: src/test/kotlin/com/nexus/shopping/checkout/adapter/outbound/acl/BillingShippingGatewayAdaptersTest.kt

**Interfaces:**
- Consumes: input ports de Billing e Shipping exclusivamente nos ACLs.
- Produces: ordem apply -> invoice -> calculate -> dispatch -> notify.

- [ ] **Step 1: Write the failing tests**

Adicionar gateways de registro e um teste aprovado com:

~~~kotlin
assertEquals(
    listOf("payment", "apply", "invoice", "shipping", "notify"),
    events.takeLast(5),
)
~~~

No gateway de Shipping de teste, registrar shipping; o teste unitario de
Shipping prova calculate -> dispatch. Adicionar casos REQUESTED e REJECTED sem
invoice ou shipping. Adicionar APPROVED com replayed = true que ainda chama os
dois gateways, documentando o limite sem persistencia.

- [ ] **Step 2: Run test to verify it fails**

Run: env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests '*ExecuteCheckoutUseCaseTest'

Expected: FAIL porque Checkout nao conhece os gateways.

- [ ] **Step 3: Add Checkout ports, models and ACLs**

~~~kotlin
interface BillingGateway {
    fun issueInvoice(command: CheckoutInvoiceCommand)
}

interface ShippingGateway {
    fun process(command: CheckoutShippingCommand)
}
~~~

Criar os dois comandos em CheckoutModels.kt. Nos ACLs, converter para
IssueInvoiceCommand e ProcessShippingCommand e chamar os input ports. Usar
alias de import Kotlin se os nomes coincidirem, como no
OrderPaymentResultGatewayAdapter.

- [ ] **Step 4: Orchestrate and wire**

No ramo APPROVED de ExecuteCheckoutUseCase, depois de aplicar o resultado,
chamar:

~~~kotlin
billing.issueInvoice(CheckoutInvoiceCommand.from(updatedOrder))
shipping.process(CheckoutShippingCommand.from(updatedOrder))
notifications.ensureOrderConfirmation(/* comando existente, sem alteracao */)
~~~

Manter as chamadas fora da transacao curta de criacao de Order. Adicionar os
gateways ao construtor e ao bean de CheckoutConfiguration. Atualizar os
construtores nos testes de integracao.

- [ ] **Step 5: Run tests and commit**

Run: env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests '*BillingShippingGatewayAdaptersTest' --tests '*ExecuteCheckoutUseCaseTest' --tests '*ExecuteCheckoutIntegrationTest'

~~~bash
git add src/main/kotlin/com/nexus/shopping/checkout src/test/kotlin/com/nexus/shopping/checkout
git commit -m "feat: orchestrate billing and shipping after payment"
~~~

### Task 4: Proteger as fronteiras e documentar

**Files:**
- Modify: src/test/kotlin/com/nexus/shopping/PackageStructureArchitectureTest.kt
- Modify: README.md
- Modify: docs/agents/monolith-baseline.md
- Modify: docs/decisions/2026-07-17-prd-commerce-bounded-contexts.md

**Interfaces:**
- Produces: isolamento arquitetural verificavel e documentacao sem mudanca HTTP.

- [ ] **Step 1: Extend architecture tests**

Incluir billing e shipping nas listas de bounded contexts proibidos de depender
de Checkout. Proibir Billing e Shipping de dependerem reciprocamente e dos
contextos existentes. Permitir seus input ports somente para
checkout.adapter.outbound.acl.

- [ ] **Step 2: Run architecture test**

Run: env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew test --tests '*PackageStructureArchitectureTest'

Expected: PASS; nao ha dependencia direta entre contextos.

- [ ] **Step 3: Update documentation**

Atualizar README e monolith-baseline.md com Billing e Shipping e o caminho
Billing -> Shipping -> Notification. Declarar os efeitos de log, a ausencia de
persistencia e a duplicacao possivel em replay aprovado.

Atualizar o ADR: Billing inicia com Invoice, Receipt fica futuro e dirigido por
Payment aprovado; Shipping somente simula calculo e despacho. Nao alterar
docs/api/openapi.yaml.

- [ ] **Step 4: Verify and commit**

Run: env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew build

Expected: PASS, sem migrations ou endpoints novos.

~~~bash
git add README.md docs/agents/monolith-baseline.md docs/decisions/2026-07-17-prd-commerce-bounded-contexts.md src/test/kotlin/com/nexus/shopping/PackageStructureArchitectureTest.kt
git commit -m "docs: describe billing and shipping contexts"
~~~

## Revisao do plano

- Task 1 cobre Billing; Task 2 cobre Shipping; Task 3 cobre ACLs e
  orquestracao; Task 4 cobre fronteiras e documentacao.
- Nenhuma tarefa cria entidade, persistencia, migration, HTTP, OpenAPI, Receipt
  ou mudanca em Notification.
- O plano limita input ports novos aos ACLs e registra explicitamente a
  duplicacao possivel em replay aprovado.
