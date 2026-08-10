# Payment Ecosystem Delivery Roadmap

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Entregar um PSP de cartao em Go, integra-lo a um Payment Service e so entao refatorar o Nexus Shopping para consumir o novo servico.

**Architecture:** O PSP e um servico Go autonomo com PostgreSQL proprio e webhooks. O Payment Service e o dono do dominio e se conecta ao PSP por adapter HTTP. Nexus permanece consumidor de Payment e nao conhece contratos internos do PSP.

**Tech Stack:** Go e PostgreSQL para o PSP; stack do Payment Service definida em sua propria spec; Kotlin, Spring Boot e PostgreSQL no Nexus existente.

## Global Constraints

- Executar cada fase em repositorio ou worktree isolados.
- Nao compartilhar banco, tipos de dominio ou bibliotecas de negocio entre PSP, Payment Service e Nexus.
- Somente cartao e `auth_and_capture` pertencem a V1 do PSP.
- Nunca trafegar, persistir ou registrar PAN, CVV ou dados reais de cartao.
- Preservar idempotencia de negocio no Payment Service mesmo quando o PSP tem idempotencia propria.
- Nao usar failover automatico entre provedores para timeout ambiguo.

---

### Task 1: Implementar e aceitar o PSP de cartao em Go

**Responsavel:** sessao e repositorio autonomos do PSP.

**Consumes:** `docs/superpowers/specs/2026-08-10-go-card-psp-design.md` como contrato aprovado.

**Produces:** um servico Go com PostgreSQL proprio, Basic Auth, pagamentos de
cartao por token de cenario, idempotencia, assinaturas administrativas e
webhooks HMAC.

- [ ] Criar no repositorio do PSP uma spec tecnica e plano detalhado que mantenham
  o contrato desta spec, sem importar ou depender de Nexus/Payment Service.
- [ ] Implementar testes unitarios para token de cenario, estados, idempotencia
  e assinatura HMAC antes dos handlers HTTP.
- [ ] Implementar persistencia de transacoes, chaves de idempotencia,
  assinaturas e entregas de webhook no PostgreSQL proprio.
- [ ] Implementar `POST /v1/payments`, `POST /v1/webhook-subscriptions` e
  `POST /v1/webhook-deliveries/{delivery_id}/retry` protegidos por Basic Auth.
- [ ] Verificar localmente os cenarios aprovado, recusado, processamento para
  aprovado, processamento para recusado, replay, concorrencia e reenvio.
- [ ] Publicar o contrato HTTP versionado e executar revisao antes de iniciar
  o Payment Service.

**Gate:** O PSP e iniciado e testado sem repositorio, banco, pacote ou URL do
Nexus Shopping. A aprovacao humana do contrato e da verificacao encerra esta
fase.

### Task 2: Implementar o Payment Service como consumidor do PSP

**Responsavel:** trabalho independente, iniciado somente apos o gate da Task 1.

**Consumes:** contrato HTTP versionado do PSP e a semantica atual de
`PaymentAttempt`, `referenceId` opaco e idempotencia do Nexus.

**Produces:** Payment Service com banco proprio, API para o Checkout e adapter
HTTP para o PSP, incluindo receptor e reconciliador de webhooks.

- [ ] Criar uma spec do Payment Service que separe seu dominio dos DTOs e
  estados proprietarios do PSP.
- [ ] Criar plano e testes para tentativa, fingerprint de autorizacao,
  idempotencia, fencing, timeout e reconciliacao de webhook.
- [ ] Implementar cliente HTTP idempotente do PSP e mapear somente estados
  normalizados `REQUESTED`, `APPROVED` e `REJECTED` ao dominio Payment.
- [ ] Implementar receptor de webhooks que valide HMAC, persista `event_id`,
  deduplique reentregas e aplique apenas transicoes validas.
- [ ] Demonstrar que resposta perdida, retry e webhook fora de ordem nao
  duplicam tentativa nem alteram um resultado terminal.
- [ ] Publicar o contrato HTTP do Payment Service para seus consumidores e
  obter revisao humana antes de alterar Nexus.

**Gate:** Payment Service opera contra o PSP em Go e mantem sua propria
idempotencia e persistencia. Nenhuma parte do Nexus foi modificada nesta fase.

### Task 3: Refatorar Nexus para consumir o Payment Service

**Responsavel:** worktree isolada deste repositorio, iniciada somente apos o
gate da Task 2.

**Consumes:** contrato HTTP versionado do Payment Service e contratos HTTP
existentes de checkout.

**Produces:** adapter HTTP de Integration/Checkout para Payment Service,
preservando a fronteira entre Cart, Order e Notification.

- [ ] Criar spec de migracao que enumere cada contrato HTTP existente afetado,
  a estrategia de configuracao e a politica de timeout/retry.
- [ ] Substituir somente o adapter local de Payment por um adapter HTTP; o
  workflow `integration/checkout` continua dependente de sua propria porta.
- [ ] Manter `Order` e `Notification` sem imports, consultas ou FKs para o
  banco do Payment Service ou do PSP.
- [ ] Cobrir checkout aprovado, recusado, `REQUESTED`, replay, resposta perdida
  e reconciliacao posterior por teste de integracao controlado.
- [ ] Remover o provider local somente quando os testes de contrato com Payment
  Service e os testes ponta a ponta estiverem aprovados.
- [ ] Abrir PR para revisao humana; nao fazer merge sem confirmacao explicita.

**Gate:** Nexus usa exclusivamente o contrato HTTP do Payment Service e nao
conhece PSP, tokens de cenario, Basic Auth ou assinaturas de webhook.

## Evolucoes posteriores

- `auth_only`, captura posterior/parcial, cancelamento e estorno.
- Tokenizacao, 3DS, parcelamento, antifraude, Pix, boleto e chargeback.
- Varias contas tecnicas, rotacao de chaves e dashboard do PSP.
- Novo adapter para Pagar.me, Mercado Pago ou Stripe e selecao manual de
  provedor por configuracao.
- Mensageria para resultado de pagamento e notificacoes, depois de estabilizar
  os contratos HTTP.
