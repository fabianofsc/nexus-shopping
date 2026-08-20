# Sandboxes de gateways para o contexto Payment

Data da pesquisa: 2026-08-10. Fontes: apenas documentacao oficial dos provedores.

> **Status de implementacao (2026-08-20):** esta pesquisa antecede a extracao
> de Payment. O Nexus Shopping agora usa `nexus-payment-service` via HTTP e
> reconciliacao assincrona; as recomendacoes sobre um provider local sao
> referencia para a evolucao interna desse servico, nao para o runtime atual do
> Nexus.

## Resposta curta

Sim. Ha gateways com ambientes de teste que nao movimentam dinheiro real e
permitem exercitar autorizacao, recusa, pagamentos assincronos e webhooks. Para
o Nexus Shopping, a melhor combinacao e manter o provider local simulado como
test double deterministico e, na extracao de Payment, adicionar um adapter
real configuravel para sandbox.

O Pagar.me e a referencia brasileira mais direta para o primeiro adapter:
tem chaves de sandbox, simuladores explicitos de aprovado, recusado e
processamento assincrono, idempotencia e webhooks. Mercado Pago e uma boa
segunda referencia brasileira, especialmente para contas e cartoes de teste.
Stripe e Adyen tem documentacao de teste particularmente rica, mas sao menos
alinhados ao contexto brasileiro do projeto.

Nenhum sandbox substitui o simulador local: ele nao e apropriado para a suite
automatizada por envolver rede, credenciais, limites e disponibilidade de
terceiro.

## Comparacao

| Gateway | Ambiente e credenciais de teste | Cenarios e cartoes de teste | Webhooks | Idempotencia documentada | Leitura para o projeto |
|---|---|---|---|---|---|
| Mercado Pago | Credenciais de teste ficam disponiveis ao criar a aplicacao; ha contas de teste de vendedor, comprador e integrador. | Cartoes e codigos de titular permitem simular aprovado (`APRO`), recusado (`OTHE`) e pendente (`CONT`), entre outros. | URL separada para teste, eventos de pagamento/order, assinatura secreta; confirmar com 200/201 em ate 22 s ou havera reenvio. | `X-Idempotency-Key` e obrigatorio nas APIs atuais de pagamento/order; use UUID unico. | Boa referencia brasileira para lifecycle de order, credenciais, contas e notificacoes. |
| Pagar.me | Mesmo endpoint v5; a chave `sk_test_*` ou `pk_test_*` seleciona o sandbox. | Simulador de cartao oferece aprovado, recusado, `processing` seguido de aprovado e `processing` seguido de falha. Ha simulador PSP para cenarios adicionais. | Eventos de order e charge, inclusive pago, falho, pendente, processing, estornado e chargeback; possui reenvio e consulta de falhas. | Header `Idempotency-key`; 24 h em producao e 5 min no sandbox; concorrencia pode retornar 409. | Melhor primeiro adapter real de sandbox e melhor referencia para testar estado assincrono. |
| PagBank (PagSeguro) | Sandbox separado em `https://sandbox.api.pagseguro.com/`; novas contas iniciam nele. | Cartoes de teste para autorizacao e recusa; simulador cobre boleto e Pix, incluindo conclusao com atraso. | O produto oferece webhooks para mudancas de status e permite reenviar notificacoes no ambiente de teste. | A documentacao de cartoes mostra `x-idempotency-key` nas chamadas de charge; confirmar o contrato do endpoint escolhido antes de implementar. | Boa opcao brasileira para contrastar autorizacao/captura e meios locais. Requer homologacao para producao. |
| Stripe | Sandboxes/test mode isolados e chaves de teste. | Cartoes e `PaymentMethod` de teste simulam sucesso, recusa, fraude, disputa, reembolso e 3DS. | CLI encaminha para localhost e dispara eventos; entregas podem ser reenviadas e nao ha garantia de ordem. | `Idempotency-Key` em todos os POSTs v1; resposta inicial e preservada por pelo menos 24 h. | Excelente referencia de contrato e testes de webhook, mas nao deve orientar regras brasileiras. |
| Adyen | Test Customer Area, credenciais e endpoints de teste separados. | Cartoes de teste e cartoes gerados; nada e debitado em conta real. | Webhooks de teste, ferramenta de teste no portal, HMAC, reentregas e orientacao explicita para lidar com duplicados. | Header `idempotency-key` em POST; chave valida por ao menos 7 dias; timeout pode ser repetido com a mesma chave. | Referencia forte de resiliencia e webhook, com maior complexidade operacional. |

## Evidencias e fontes oficiais

### Mercado Pago

- [Credenciais](https://www.mercadopago.com.br/developers/pt/docs/your-integrations/credentials): credenciais de teste ficam disponiveis logo apos a criacao da aplicacao.
- [Contas de teste](https://www.mercadopago.com.br/developers/pt/docs/your-integrations/test/accounts): contas de vendedor, comprador e integrador para exercitar os fluxos.
- [Cartoes de teste](https://www.mercadopago.com.br/developers/pt/docs/your-integrations/test/cards): cartoes e codigos de status para cenarios controlados.
- [Webhooks](https://www.mercadopago.com.br/developers/pt/docs/your-integrations/notifications/webhooks): URL de teste, assinatura e reentrega de notificacoes.
- [Criar order no Checkout Transparente](https://www.mercadopago.com.br/developers/pt/reference/online-payments/checkout-api/create-order/post): `X-Idempotency-Key` obrigatorio no contrato atual.

### Pagar.me

- [Autenticacao](https://docs.pagar.me/reference/autentica%C3%A7%C3%A3o-2): chaves `sk_test_*`/`pk_test_*` e selecao de sandbox pelo tipo de chave.
- [Simulador de cartao de credito](https://docs.pagar.me/docs/simulador-de-cart%C3%A3o-de-cr%C3%A9dito): cenarios de aprovado, recusado e processamento assincrono.
- [Simulador PSP](https://docs.pagar.me/docs/simulador-psp): simulacao adicional de antifraude, split e recebiveis.
- [Idempotencia](https://docs.pagar.me/docs/o-que-%C3%A9): duracao, concorrencia, retry e comportamento de erros.
- [Visao geral de webhooks](https://docs.pagar.me/reference/vis%C3%A3o-geral-sobre-webhooks) e [eventos](https://docs.pagar.me/reference/eventos-de-webhook-1): eventos, tentativas e consulta de falhas.

### PagBank (PagSeguro)

- [Ambientes disponiveis](https://developer.pagbank.com.br/docs/ambientes-disponiveis): URLs de sandbox e producao.
- [Testar integracao](https://developer.pagbank.com.br/docs/testar-integracao): nenhuma transacao de sandbox tem valor monetario; ha cartoes e simulador.
- [Cartoes de teste](https://developer.pagbank.com.br/docs/cartoes-de-teste): respostas de autorizacao e recusa e exemplo de `x-idempotency-key`.
- [Simulador](https://developer.pagbank.com.br/docs/simulador): cenarios de Pix/boleto, inclusive conclusao atrasada.
- [Checkout e link de pagamento](https://developer.pagbank.com.br/docs/checkout): notificacoes de mudanca de status e necessidade de homologacao antes de producao.

### Stripe

- [Testing](https://docs.stripe.com/testing): sandboxes, valores de teste, cartoes e cenarios sem movimentacao de fundos.
- [Idempotent requests](https://docs.stripe.com/api/idempotent_requests?lang=curl): semantica de `Idempotency-Key` e janela minima de 24 h.
- [Webhooks](https://docs.stripe.com/webhooks?lang=node): teste local, retries e ausencia de garantia de ordenacao.
- [Trigger webhook events with the Stripe CLI](https://docs.stripe.com/stripe-cli/triggers?locale=en-GB): disparo controlado de eventos no sandbox.

### Adyen

- [Testing your online payments integration](https://docs.adyen.com/development-resources/testing): credenciais de teste e teste completo, inclusive webhook.
- [Test card numbers](https://docs.adyen.com/development-resources/test-cards-and-credentials/test-card-numbers): cartoes exclusivos da plataforma de teste e valores cifrados de teste.
- [API idempotency](https://docs.adyen.com/development-resources/api-idempotency): `idempotency-key`, retry, conflitos e validade minima de sete dias.
- [Configure and manage webhooks](https://docs.adyen.com/development-resources/webhooks/configure-and-manage/): teste de configuracao e teste ponta a ponta no ambiente de teste.
- [Handle webhook events](https://docs.adyen.com/development-resources/webhooks/handle-webhook-events/): validacao HMAC e obrigacao de tratar duplicatas.

## Recomendacao para a extracao de Payment

1. Preserve o atual provider simulado como `FakePaymentGateway` local. Ele deve
   ser deterministico, sem rede, e oferecer cenarios programaveis de aprovado,
   recusado, pendente, timeout, resposta perdida e webhook duplicado/fora de
   ordem. Ele continua sendo usado nos testes unitarios, de integracao e CI.
2. Defina um port outbound de gateway que aceite uma chave de idempotencia,
   `referenceId`, valor em centavos, moeda e token/fingerprint, e devolva um
   identificador externo e estado normalizado. O dominio nunca deve conhecer
   DTOs ou estados proprietarios do gateway.
3. Implemente primeiro `PagarmePaymentGatewayAdapter`, habilitado somente por
   profile/configuracao de sandbox. Use-o em testes manuais ou E2E opt-in, com
   credenciais fora do repositorio. Exercite ao menos aprovado, recusado,
   `processing`, timeout/retry com a mesma chave e webhook.
4. Crie um endpoint de webhook no Payment que valide autenticidade conforme o
   provedor escolhido, persista o evento antes de responder, deduplique pelo
   identificador externo e reconcilie pelo identificador/referencia da
   tentativa. Responder rapido e processar assincronamente e importante:
   provedores repetem entregas e podem envia-las fora de ordem.
5. Nao use sandbox para carga e nao substitua as garantias locais de
   idempotencia, lease/fencing ou reconciliacao. A idempotencia do provedor tem
   janela e semantica proprias; a do Payment precisa continuar sendo a fonte de
   verdade do negocio.

## Ressalvas

- A disponibilidade e o comportamento de sandbox sao externos; testes contra
  eles devem ser opt-in e nunca bloquear a build.
- Testar um gateway nao autoriza armazenar PAN, CVV ou tokens brutos. O
  servico deve receber apenas tokenizacao/representacao segura prevista pelo
  provedor e manter os cuidados de PCI.
- Antes de contratar ou promover para producao, conferir requisitos comerciais,
  habilitacao de meios brasileiros, limites e processo de homologacao. PagBank
  declara homologacao obrigatoria para a maioria das integracoes.
- A documentacao atual do Pagar.me consultada descreve eventos e reenvios, mas
  nao tornou evidente uma regra v5 de assinatura de webhook; validar esse
  requisito diretamente no contrato vigente antes de codificar o adapter.
