# ADR: Transferir notificacoes ao Notification Service com journal operacional no Checkout

- Status: Aceita
- Data: 2026-08-16
- Complementa: `2026-08-12-prd-autonomous-external-services.md`
- Decisao relacionada: `2026-07-24-prd-order-transaction-boundary.md`

## Contexto

O Nexus Shopping ainda contem um bounded context `notification` local. Ele possui
tipos de dominio, templates, casos de uso, controller `/notifications`, tabela
`notifications`, adapter JPA e adapter de e-mail. O checkout chama esse contexto
por meio de `checkout/application/port/outbound/NotificationGateway`.

O repositorio irmao `../notification-service` ja oferece o servico autonomo que
deve assumir o ciclo de entrega. Seu contrato relevante e:

- `POST /v1/notifications` usa Basic Auth e `Idempotency-Key` obrigatoria;
- recebe uma mensagem ja renderizada, destinatario e referencias opacas;
- responde `202 Accepted` com a notificacao persistida em estado `PENDING`;
- a entrega ocorre depois, em worker proprio;
- repetir a mesma chave com o mesmo payload e seguro; payload diferente devolve
  `409 Conflict`.

O `202` confirma somente o aceite para entrega, nunca o envio de e-mail ou SMS.
O Nexus nao precisa consultar historico de notificacoes e o endpoint local
`/notifications` nao possui consumidores. Portanto, nao ha requisito para
preservar IDs locais, listagem por cliente ou compatibilidade desse endpoint.

Existe, contudo, um requisito operacional: indisponibilidade ou timeout do
Notification Service nao pode falhar o checkout depois de o pedido ter sido
confirmado. A ocorrencia precisa permanecer identificavel e poder ser reenviada
ou descartada manualmente por um backoffice do Nexus.

## Alternativas consideradas

### 1. Chamar o Notification Service sem estado local

O adapter HTTP faria um `POST` diretamente quando o pagamento fosse aprovado.

Vantagem: menor quantidade de codigo e nenhuma nova tabela.

Desvantagem: se a resposta se perder ou o servico estiver indisponivel, nao ha
registro duravel para identificar o problema, mostrar ao operador ou tentar de
novo. A opcao nao atende ao requisito de retry manual.

### 2. Manter o bounded context `notification` local como facade

O contexto existente manteria controller, entidade e persistencia e passaria a
chamar o servico externo.

Vantagem: aproveita a tabela e os fluxos locais.

Desvantagem: cria duas fontes de verdade para o ciclo de entrega e um modulo
raso que apenas reproduz modelos e estados ja pertencentes ao servico externo.
Tambem preserva APIs e consultas que nao possuem consumidores nem valor de
negocio atual.

### 3. Remover o contexto local e manter um journal de submissao no Checkout

O Checkout conserva apenas um registro tecnico da sua intencao de submeter uma
mensagem. Um gateway HTTP envia essa intencao ao Notification Service; o servico
externo continua sendo o unico dono de notificacao, entrega, tentativas e status
de entrega.

Vantagem: atende a recuperacao manual sem reintroduzir o dominio de notificacao
no Nexus. A responsabilidade de integracao fica localizada no unico ponto que
conhece Order, Payment e o contrato externo.

Desvantagem: acrescenta uma tabela e casos de uso tecnicos no modulo de
integracao.

## Decisao

Adotar a alternativa 3.

O bounded context `notification` sera removido do Nexus. A seam
`NotificationGateway` permanece em `checkout/application`: ela
expressa a intencao de confirmar a notificacao do pedido sem expor HTTP, JSON,
Basic Auth, `202`, IDs externos ou estados de entrega ao workflow.

O adapter que satisfaz essa porta sera um gateway HTTP para o Notification
Service. Ele constituira a ACL do Checkout e concentrara mapeamento de DTOs,
autenticacao, timeout, classificacao de falhas e logs seguros. O modulo do
Checkout tambem ganhara o journal `NotificationSubmission`, que representa a
submissao local, e nao uma notificacao ou sua entrega.

### Ownership e contratos

O Nexus e dono de:

- decidir que um pedido aprovado requer a mensagem de confirmacao;
- renderizar assunto e corpo a partir dos dados do pedido;
- cunhar referencias opacas e a chave idempotente;
- registrar e operar a submissao ao servico externo.

O Notification Service e dono de:

- persistir a notificacao remota;
- deduplicar pelo `Idempotency-Key`;
- entregar, registrar tentativas e definir o estado de entrega;
- fornecer o `notification_id` remoto.

Para a confirmacao de pedido, o payload e invariavel para uma mesma chave:

```text
Idempotency-Key: order-confirmed:{orderId}:{attemptReference}
channel: EMAIL
recipient.email: {recipientEmail}
subject: Pedido {orderId} confirmado
body: Seu pedido {orderId} no valor de {amount} foi confirmado.
reference_id: order:{orderId}
callback_id: order:{orderId}
callback_name: order_confirmed
```

O mesmo registro nunca pode ser reenviado com payload diferente. Isso preserva a
idempotencia do contrato remoto e evita o `409 Conflict` por reutilizacao de
chave incompativel.

### Journal de submissao

`NotificationSubmission` pertence a `checkout`. Seus dados minimos
sao:

- identificador local opaco;
- `orderId`, `attemptReference` e `notificationKey` deterministica;
- destinatario, assunto, corpo e referencias necessarias para reproduzir o
  payload exatamente;
- estado operacional, numero de tentativas, ultimo erro sanitizado e
  timestamps;
- `notificationId` remoto, quando o aceite ocorrer;
- token e validade de lease enquanto uma submissao esta em voo.

Os estados locais sao `PENDING`, `IN_FLIGHT`, `ACCEPTED`, `FAILED` e
`DISCARDED`.

- `PENDING` significa que a submissao foi criada e ainda nao foi tentada;
- `IN_FLIGHT` protege a tentativa em andamento e pode ser retomado quando a
  lease expirar;
- `ACCEPTED` significa que o Notification Service respondeu `202`; nao significa
  entrega concluida;
- `FAILED` conserva o erro para investigacao e retry manual;
- `DISCARDED` e uma decisao operacional terminal de nao reenviar.

Somente `PENDING`, `FAILED` e `IN_FLIGHT` com lease expirada podem ser retomados.
`ACCEPTED` e `DISCARDED` nunca sao reenviados nem descartados novamente.

O registro e reservado por atualizacao condicional antes do HTTP. A chamada de
rede ocorre fora de transacao de banco. A conclusao tambem e condicional ao token
da lease, evitando que tentativas concorrentes sobrescrevam o resultado uma da
outra. A chave idempotente remota continua sendo a protecao final para timeout
ambiguo ou chamada duplicada entre instancias.

### Fronteira transacional e fluxo

Quando um pagamento torna um pedido aprovado, a aplicacao do resultado em Order e
a criacao idempotente da `NotificationSubmission` devem ocorrer na mesma
`TransactionPort` local. Assim, nao existe pedido confirmado sem uma intencao
duravel de notificacao.

Depois do commit, o `NotificationGateway` tenta uma unica chamada HTTP. Falhas de
rede, timeout ou resposta remota nao sao propagadas ao controller de checkout:
elas atualizam a submissao para `FAILED`, geram log estruturado seguro e o
checkout devolve a resposta normal do pedido confirmado.

Depois desse commit, o processo preserva a sequencia existente de efeitos:
Billing emite Invoice, Shipping calcula/despacha e somente entao o
`NotificationGateway` tenta o aceite remoto. Billing e Shipping nao pertencem ao
dominio de notificacao e seus erros mantem a semantica vigente: interrompem os
efeitos seguintes, inclusive o dispatch. Como a submissao ja foi reservada, ela
permanece `PENDING`, diagnosticavel e elegivel ao backoffice; nao ha dispatch ou
retry automatico nessa situacao.

O mesmo fluxo deve ser usado tanto para aprovacao sincrona do pagamento quanto
para a aprovacao descoberta pelo reconciliador. A chamada HTTP nunca pode ficar
dentro da transacao de Cart, Order, Payment ou journal.

Nao havera retry automatico nesta decisao. O retry e uma acao humana explicita;
cada acionamento usa o payload e a chave armazenados, sem recalcular templates ou
referencias.

### Backoffice

O Nexus expora um backoffice tecnico, separado da API de catalogo e checkout:

```text
GET  /backoffice/notification-submissions?status=FAILED&page=0&size=50
POST /backoffice/notification-submissions/{id}/retry
POST /backoffice/notification-submissions/{id}/discard
```

O `GET` usa o contrato de `Slice` ja adotado pelo projeto. Ele exibe informacao
operacional suficiente para correlacao, como ID local, pedido, chave, estado,
tentativas, ultimo erro e timestamps. Destinatario, assunto e corpo nao aparecem
nos logs e nao devem integrar a resposta de listagem.

`retry` reclama uma submissao elegivel, executa a tentativa fora de transacao e
retorna seu estado atualizado. `discard` requer uma justificativa curta, marca a
submissao terminalmente como `DISCARDED` e nao realiza HTTP. Os dois comandos sao
idempotentes no sentido operacional: uma submissao ja `ACCEPTED` ou `DISCARDED`
nao pode voltar ao fluxo de entrega.

O projeto nao possui Spring Security nem convencao de autorizacao por endpoint.
Para manter o escopo, estes endpoints seguirao o mesmo modelo de acesso da
aplicacao atual e deverao ser restringidos pelo ingress ou rede do ambiente. A
introducao de autenticacao/autorizacao na aplicacao e uma decisao transversal
separada, nao um requisito escondido desta integracao.

### Configuracao e falhas HTTP

O gateway usa `RestClient` com HTTP/1.1 e timeouts finitos, seguindo o adapter do
Payment Service. A configuracao vem exclusivamente do ambiente, sob
`nexus.notification-service`:

```yaml
nexus:
  notification-service:
    base-url: ${NEXUS_NOTIFICATION_SERVICE_BASE_URL:http://notification-service:8080}
    username: ${NEXUS_NOTIFICATION_SERVICE_USERNAME:notification}
    password: ${NEXUS_NOTIFICATION_SERVICE_PASSWORD:notification}
```

As credenciais sao usadas somente no header Basic Auth e nunca sao logadas. A
implementacao tambem deve tornar timeout de conexao e leitura configuraveis,
mantendo cinco segundos como default inicial coerente com o adapter de pagamento.

Resposta `202` marca `ACCEPTED` e armazena o ID remoto. Falhas de conexao, timeout,
`429` e `5xx` marcam `FAILED` como potencialmente recuperaveis. `400`, `401`,
`403`, `409` e `422` tambem permanecem visiveis em `FAILED`, mas sao classificados
como nao recuperaveis ate uma acao consciente do operador. Nenhuma classificacao
altera o resultado do checkout. Logs e metricas carregam operacao, resultado,
status HTTP e latencia, sem senha, corpo, assunto ou destinatario.

### Remocao do contexto antigo

Serao removidos o controller `/notifications`, DTOs, dominio, casos de uso,
ports, adapters JPA e e-mail, testes e documentacao ligados ao bounded context
local. README, mapa de contextos e documentacao de endpoints deixarao de listar
Notification como contexto do monolito.

As migrations historicas `V6__create_notification_context.sql` e
`V10__apply_payment_results_and_deduplicate_notifications.sql` nao serao apagadas
nem alteradas. Uma migration nova pode remover a tabela `notifications` e seus
objetos depois que a integracao estiver validada. Como nao existe requisito de
historico nem consumidor da API antiga, nao havera migracao de dados para o
Notification Service: reenviar registros antigos criaria entregas novas e seria
semanticamente incorreto.

## Consequencias

### Positivas

- Ha uma unica fonte de verdade para entrega de notificacoes novas.
- Indisponibilidade do servico externo nao transforma um pedido aprovado em erro
  para o cliente.
- Cada falha tem um registro duravel, observavel e recuperavel manualmente.
- A interface do Checkout permanece pequena e os detalhes externos ficam
  localizados na ACL.
- O Notification Service continua agnostico aos dominios Order e Customer.

### Negativas

- O Nexus passa a armazenar uma copia operacional do payload para permitir retry
  fiel; ela exige o mesmo cuidado de privacidade da tabela local anterior.
- O journal acrescenta migration, repositorio, casos de uso e endpoints tecnicos.
- Sem retry automatico, uma submissao falha depende da intervencao do backoffice.
- O backoffice precisa ser protegido pela topologia de rede ate existir uma
  decisao de autenticacao transversal.

## Criterio de conclusao

A decisao esta realizada quando um pagamento aprovado cria exatamente uma
submissao duravel, Billing e Shipping precedem o dispatch apos o commit, o
checkout permanece bem-sucedido diante de falha do Notification Service, o
backoffice permite listar/retry/discard sem duplicar o payload remoto, e nenhum
codigo de producao do Nexus depende do antigo pacote `notification`.

## Referencias

- `../notification-service/README.md`
- `docs/agents/external-services.md`
- `docs/superpowers/specs/2026-07-26-checkout-integration-boundaries-design.md`
