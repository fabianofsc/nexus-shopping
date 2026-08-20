# Configuracao de webhooks no Pagar.me v5

Data da pesquisa: 2026-08-10.

Escopo: apenas documentacao oficial publica do Pagar.me. Este documento registra
fatos observados sobre configuracao e entrega de webhooks; nao define contrato
ou arquitetura para o Nexus Shopping.

## Cadastro do endpoint

- O modelo v5 usa uma URL previamente configurada. O guia oficial instrui
  acessar `Conta > Configuracoes > Webhooks`, criar um webhook, informar a URL
  de destino e escolher os eventos. Portanto, a URL nao e enviada em cada
  criacao de pedido ou cobranca.
- E possivel configurar varios endpoints. Cada webhook possui, entre outros,
  `url` (destino) e `event` (evento associado).
- A documentacao de migracao tambem declara que, na v5, webhooks podem ser
  configurados pela Dashboard ou por API. A referencia publica consultada
  descreve consulta e reenvio das entregas, mas nao explicita a rota para criar
  ou alterar a configuracao do endpoint.

Fontes: [como configurar webhook](https://pagarme.helpjuice.com/pt_BR/p2-funcionalidades/configura%C3%A7%C3%B5es-como-configurar-webhooks),
[visao geral de webhooks](https://docs.pagar.me/reference/vis%C3%A3o-geral-sobre-webhooks)
e [migracao de API](https://pagarme.helpjuice.com/pt_BR/sobre-o-pagarme/passo-a-passo-de-migracao-de-api).

## Eventos e entrega

- O Pagar.me envia um HTTP POST para a URL configurada quando ocorre o evento.
  A configuracao permite selecionar quais eventos vao para qual URL.
- A lista atual inclui, entre outros, `charge.created`, `charge.updated`,
  `charge.paid`, `charge.payment_failed`, `charge.pending`,
  `charge.processing` e `charge.refunded`; tambem ha eventos de pedido como
  `order.paid` e `order.payment_failed`.
- A entrega possui `id`, `event`, `status`, `attempts`, `last_attempt`,
  `response_status`, `response_raw` e `data`. Seus estados documentados sao
  `pending`, `sent` e `failed`.

Fontes: [guia de webhooks](https://docs.pagar.me/docs/webhooks),
[eventos de webhook](https://docs.pagar.me/reference/eventos-de-webhook-1),
[visao geral](https://docs.pagar.me/reference/vis%C3%A3o-geral-sobre-webhooks) e
[exemplo de payload](https://docs.pagar.me/reference/exemplo-de-webhook-1).

## Falhas, tentativas e reenvio

- O guia informa que o numero de reenvios em caso de falha de recebimento e
  configuravel. Um tutorial oficial da Dashboard apresenta a opcao "Ate 3
  tentativas"; a pagina de migracao afirma maximo automatico de tres
  tentativas quando a resposta nao for 2xx.
- Entregas podem ser listadas por status e evento em
  `GET /core/v5/hooks`. A consulta permite os estados `pending`, `sent` e
  `failed`.
- Uma entrega individual pode ser reenviada por
  `POST /core/v5/hooks/{hook_id}/retry`. A pagina de migracao informa que a
  consulta e o reenvio ficam disponiveis por 30 dias apos a criacao da entrega.

Fontes: [guia de webhooks](https://docs.pagar.me/docs/webhooks),
[listar webhooks](https://docs.pagar.me/reference/listar-webhooks),
[enviar webhook](https://docs.pagar.me/reference/enviar-webhook),
[tutorial de configuracao](https://pagarme.helpjuice.com/pt_BR/p2-m%C3%B3dulos-e-plataformas/omnichat-%7C-tutorial-de-ativacao)
e [migracao de API](https://pagarme.helpjuice.com/pt_BR/sobre-o-pagarme/passo-a-passo-de-migracao-de-api).

## Autenticidade

- O guia de configuracao diz que autenticacao e opcional, sem detalhar nessa
  pagina o formato nem os cabecalhos usados.
- A pagina oficial de migracao afirma que o webhook v5 usa HMAC-SHA256 e
  cabecalhos adicionais para garantir a origem. A documentacao publica
  consultada nao especifica o nome do cabecalho, a chave nem o calculo da
  assinatura; portanto, esses detalhes nao foram assumidos neste relatorio.
- A documentacao legada v2 especifica `X-Hub-Signature` com HMAC-SHA1 do corpo
  bruto e API key como chave. Essa e uma regra do postback legado, nao uma
  especificacao do webhook v5.

Fontes: [como configurar webhook](https://pagarme.helpjuice.com/pt_BR/p2-funcionalidades/configura%C3%A7%C3%B5es-como-configurar-webhooks),
[migracao de API](https://pagarme.helpjuice.com/pt_BR/sobre-o-pagarme/passo-a-passo-de-migracao-de-api)
e [validacao de postback v2](https://docs.pagar.me/v2/reference/validando-um-postback).
