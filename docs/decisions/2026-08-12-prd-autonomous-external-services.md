# ADR: Registrar DummyPay e Notification Service como servicos externos autonomos

- Status: Accepted
- Data: 2026-08-12
- Complementa: `2026-07-17-prd-commerce-bounded-contexts.md`

## Contexto

O Nexus ja possui os contextos Payment e Notification no monolito modular. Em
paralelo, dois servicos Go foram implementados em repositorios independentes:
DummyPay, um PSP deterministico de cartao, e Notification Service, uma entrega
generica de e-mail e SMS simulados.

Os servicos estao prontos para uso local, mas o Nexus ainda nao os consome. Sem
registrar essa situacao, a documentacao poderia confundir servico implementado
com integracao concluida e incentivar acoplamento direto do checkout a um PSP.

## Decisao

DummyPay e Notification Service passam a ser reconhecidos como servicos
externos autonomos do ecossistema, sem integracao fisica com o Nexus nesta
etapa.

- DummyPay e um PSP, nao o Payment Service. O proximo componente a ser criado e
  o Payment Service, que encapsula adapters de PSP e consome DummyPay por HTTP.
- O Nexus nao chama DummyPay diretamente. Depois que o Payment Service estiver
  validado, um adapter/ACL substituira o provider local do contexto Payment.
- Notification Service permanece agnostico ao negocio. O futuro consumidor do
  Nexus o chamara por HTTP por meio de adapter/ACL e enviara apenas mensagem
  renderizada, destinatario e referencias opacas.
- Cada servico e dono de seu banco, usuario, migrations, credenciais, contratos
  e ciclo de entrega. Sao proibidos banco compartilhado, foreign keys entre
  bancos, imports de tipos de dominio e SDK compartilhado entre repositorios.
- O contrato detalhado de cada servico pertence ao seu proprio repositorio. O
  Nexus mantem somente a descricao de suas fronteiras em
  `docs/agents/external-services.md`.

## Consequencias

### Positivas

- A evolucao pode exercitar ports, adapters e ACLs com dependencias externas
  reais, sem colocar dados de cartao ou providers reais no Nexus.
- DummyPay permite reproduzir aprovacao, recusa e processamento assincrono,
  inclusive idempotencia e webhooks, em ambiente local.
- Notification Service ja oferece ciclo assincrono, deduplicacao e canais fake
  sem conhecer conceitos de e-commerce.
- A separacao permite futuramente trocar ou combinar PSPs sem propagar detalhes
  de provider ao checkout.

### Negativas

- Por enquanto existem duas implementacoes locais de Payment e Notification:
  os contextos do monolito e os servicos externos. Elas nao devem ser tratadas
  como uma unica fonte de runtime ate a refatoracao planejada.
- A proxima integracao introduzira timeout, indisponibilidade, webhook fora de
  ordem e reconciliacao, complexidades inexistentes no provider local.
- Mais containers e bancos serao necessarios no ambiente de desenvolvimento
  quando a integracao for ativada.

## Sequencia

1. Implementar Payment Service com adapter HTTP para DummyPay.
2. Verificar o contrato assincrono do PSP, incluindo idempotencia, HMAC e
   reconciliacao.
3. Refatorar o Nexus para usar o Payment Service por port/ACL.
4. Integrar Notification Service por port/ACL em etapa propria.

## Referencias

- `docs/agents/external-services.md`
- Repositorio local DummyPay: `../dummy-pay`
- Repositorio local Notification Service: `../notification-service`
