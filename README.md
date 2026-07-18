# Nexus Shopping Backend

> **Nexus** (do latim *nectere*, "atar, ligar"): um ponto de conexao central. O nome reflete a arquitetura hexagonal do projeto -- o dominio como nexus entre adapters de entrada e saida -- e o dominio de negocio: um catalogo que conecta produtos, marcas e categorias.

Backend REST API educacional construido com Kotlin, Java 21, Spring Boot 4, Actuator, Flyway, PostgreSQL, Redis e Spring Data JPA.

O projeto evolui de forma incremental, cobrindo diferentes topicos em aulas progressivas: performance de banco de dados, arquitetura hexagonal, tratamento de erros padronizado, logging estruturado, load balancing e, agora, decomposicao de dominios para discutir microservices. Cada topico importante fica documentado em branches, tags, ADRs e guias reproduziveis.

## Trilha didatica

1. **Performance de leitura**: tabela grande, queries sem indice, indices e paginacao.
2. **Arquitetura de aplicacao**: Ports and Adapters, use cases, DTOs e adapters.
3. **Operacao e observabilidade**: health check, logging estruturado e correlation-id.
4. **Escalabilidade horizontal**: multiplas instancias atras de NGINX local e Load Balancer na AWS.
5. **Sistemas distribuidos**: novos Bounded Contexts de e-commerce para evoluir de monolito modular para microservices.

## Topicos e branches

| Branch | Execucao | Topico |
| --- | --- | --- |
| `missing-index-performance-baseline` | Docker Hub `:baseline` | Performance sem indices secundarios |
| `add-product-query-indexes` | Docker Hub `:indexes` | Impacto de indices de leitura |
| `add-products-pagination` | Docker Hub `:pagination` | Paginacao e custo de retorno de linhas |
| `hexagonal-architecture` | tag `v2.0-hexagonal` | Arquitetura hexagonal (Ports and Adapters) |
| `scalability-and-load-balancer` | Docker Compose local | NGINX distribuindo trafego entre 3 instancias |
| `main` | branch estavel | Versao estavel mais recente |

Veja tambem [REFERENCE_POINTS.md](REFERENCE_POINTS.md) para as tags imutaveis de estudo.

## Evolucao para E-commerce

O codigo atual e um monolito modular com quatro Bounded Contexts implementados: `Product`, `Customer`, `Cart` e `Notification`. `Order` e `Payment` permanecem planejados para as proximas etapas, antes de qualquer extracao para microservices.

![Mapa de Bounded Contexts do Nexus Shopping](docs/assets/bounded-contexts/nexus-shopping-bounded-context-map-preview.png)

Contextos decididos:

```mermaid
flowchart LR
  Product["Product / Catalogo"]
  Customer["Customer / Cliente"]
  Cart["Cart / Carrinho"]
  Order["Order / Pedido"]
  Payment["Payment / Pagamento"]
  Notification["Notification / Notificacao"]

  Product -->|"dados de exibicao"| Cart
  Customer -->|"customerId"| Cart
  Cart -->|"checkout"| Order
  Customer -->|"snapshot escolhido"| Order
  Order -->|"payment request"| Payment
  Payment -->|"payment result"| Order
  Order -->|"eventos"| Notification
```

Estado atual:

- Implementados: `Product`, `Customer`, `Cart` e `Notification`.
- Planejados: `Order` e `Payment`.
- Fora de escopo nesta etapa: `Inventory` e `Auth/Identity`.

Decisoes principais:

- Decomposicao de dominio primeiro; distribuicao fisica depois.
- `Checkout` e um fluxo, nao um Bounded Context separado nesta fase.
- `Customer` e dono dos dados cadastrais, mas `Order` guarda snapshot historico.
- `Product` no catalogo, `ProductSummary` no carrinho e `OrderItemSnapshot` no pedido nao sao o mesmo modelo global.
- `Payment` deve ser o primeiro candidato a extracao futura, por proteger o core de uma integracao externa.
- Redis e usado como cache distribuido das consultas de produto; nao e um Bounded Context.

ADR completo: [docs/decisions/2026-07-17-prd-commerce-bounded-contexts.md](docs/decisions/2026-07-17-prd-commerce-bounded-contexts.md).

## Arquitetura

O projeto segue arquitetura hexagonal (Ports and Adapters), aplicada de forma incremental. A regra de dependencia e `adapter -> application -> domain`.

```
com/nexus/shopping/
  {product,customer,cart,notification}/
    domain/           -> tipos de negocio puros
    application/
      port/outbound/  -> portas outbound
      usecase/        -> orquestracao e validacao
      command/        -> comandos de entrada dos use cases
      exception/      -> excecoes tipadas do dominio de aplicacao
    adapter/
      inbound/http/   -> controllers e DTOs
      outbound/jpa/   -> entidades JPA e adapters de persistencia
  platform/           -> excecoes e handlers compartilhados
  infra/              -> detalhes tecnicos transversais (HTTP, correlation-id)
```

Restricoes de design:

- Domain e use cases sem imports de Spring, JDBC ou JPA.
- JPA fica isolado nos adapters outbound, que implementam as portas de repositorio e convertem entities para tipos de dominio.
- Consultas JPA de leitura usam `@Query` JPQL explicito para tornar visivel o shape das queries.
- Validacao nos use cases para reuso por qualquer adaptador futuro (CLI, fila, batch).
- DTO HTTP vira command via `toCommand()`.
- Entity JPA vira domain via `toDomain()`.

## Requisitos

Para testes de carga apenas:

- Docker e Docker Compose
- Apache JMeter

Para desenvolvimento local:

- Java 21
- Docker e Docker Compose
- Gradle Wrapper (incluido como `./gradlew`)

O Wrapper baixa a versao do Gradle configurada pelo projeto, portanto nao e necessario instalar Gradle separadamente:

```bash
./gradlew build
```

Instalar JMeter no macOS:

```bash
brew install jmeter
```

## Banco de dados e cache

PostgreSQL e Redis via Docker Compose:

```bash
docker compose up -d postgres redis
```

Configuracoes padrao:

- URL: `jdbc:postgresql://localhost:5432/nexus_shopping`
- Database: `nexus_shopping`
- User: `nexus`
- Password: `nexus`
- Redis: `localhost:6379`

O Flyway executa automaticamente ao iniciar a aplicacao e cria:

- Catalogo: `brands`, `categories`, `products`
- Clientes: `customers`, `customer_contacts`, `customer_addresses`
- Notificacoes: `notifications`
- Carrinhos: `carts`, `cart_items`
- Seed de produtos configuravel via `PRODUCT_SEED_COUNT`
- Seeds reduzidos de clientes para desenvolvimento e testes
- Indexes de leitura de produtos e de relacionamentos dos demais contextos

O default da aplicacao e `1000`, para boot rapido em desenvolvimento e demos locais. Para cenarios de performance, sobrescreva explicitamente:

```bash
PRODUCT_SEED_COUNT=10000000 docker compose up -d --build
```

Para resetar o volume local apos mudancas de migrations:

```bash
docker compose down -v
docker compose up -d postgres redis
```

## Executar localmente

Aplicacao local com uma instancia:

```bash
docker compose up -d postgres redis
./gradlew bootRun
```

O seed padrao e de `1000` produtos. Para usar outro volume:

```bash
PRODUCT_SEED_COUNT=10000 ./gradlew bootRun
```

Health:

```bash
curl http://localhost:8080/actuator/health
```

Busca por categoria:

```bash
curl 'http://localhost:8080/products?categoryId=1&page=0&size=50'
```

Busca por nome:

```bash
curl 'http://localhost:8080/products?name=Product%202999999&page=0&size=50'
```

Criar produto:

```bash
curl -X POST http://localhost:8080/products \
  -H 'Content-Type: application/json' \
  -d '{"brandId":1,"categoryId":1,"sku":"SKU-001","name":"New Product","slug":"new-product","priceAmount":49.90}'
```

Principais recursos HTTP:

- Produtos: busca, detalhe, criacao e atualizacao de preco em `/products`.
- Clientes: criacao e detalhe em `/customers`.
- Carrinho ativo: consulta e mutacao de itens em `/customers/{customerId}/cart`.
- Notificacoes: envio, detalhe e listagem paginada em `/notifications`.

O contrato detalhado das consultas de catalogo esta em [docs/agents/api-endpoints.md](docs/agents/api-endpoints.md).

Stack local com NGINX e 3 instancias:

```bash
PRODUCT_SEED_COUNT=1000 docker compose up -d --build
./scripts/test-lb.sh 30
```

## Imagem Docker

O projeto usa o task nativo do Spring Boot para geracao de imagem OCI via Cloud Native Buildpacks (cenarios de performance no Docker Hub). Ha tambem um `Dockerfile` multi-stage usado pelo setup de load balancing (`docker compose build`).

Build local com buildpacks:

```bash
./gradlew bootBuildImage --imageName nexus-shopping:local
```

Push dos cenarios para o Docker Hub:

```bash
make push-baseline
make push-indexes
make push-pagination
```

## Balanceamento de carga (NGINX)

Setup com 3 instancias da aplicacao atras de um NGINX (round-robin por padrao),
para demonstrar balanceamento de carga sem Kubernetes.

Topologia:

```mermaid
flowchart TB
  Client["Cliente / localhost:8080"] --> Nginx["NGINX"]
  Nginx --> App1["app1"]
  Nginx --> App2["app2"]
  Nginx --> App3["app3"]
  App1 --> Postgres["PostgreSQL"]
  App2 --> Postgres
  App3 --> Postgres
  App1 --> Redis["Redis"]
  App2 --> Redis
  App3 --> Redis
```

Guia completo (subir, testar, trocar algoritmo) e roteiro de teste/verificacao:

```
docs/scalability-and-load-balancer/load-balancing-nginx.md      # setup, topologia e algoritmos
docs/scalability-and-load-balancer/load-balancing-test-plan.md  # roteiro de teste e verificacao
```

## Testes

```bash
./gradlew build
```

Os testes automatizados validam:

- Spring Boot inicia com o health endpoint do Actuator.
- Flyway executa automaticamente.
- Os contratos HTTP e de persistencia de Product, Customer, Cart e Notification.
- Migrations portaveis entre PostgreSQL e H2.
- Indexes de leitura presentes sem constraints UNIQUE indesejadas.
- Cache de produtos com Redis e fallback em memoria nos cenarios que o desabilitam.
- Invariantes de concorrencia na criacao e atualizacao de carrinhos.

## Lint

Lint Kotlin configurado com ktlint via Gradle. Na fase inicial, e opt-in e nao faz parte do `build`.

```bash
./gradlew ktlintCheck    # verifica estilo
./gradlew ktlintFormat   # autoformata
```

## Testes de Carga

Os testes de carga comparam os tres cenarios de performance sob alta concorrencia com JMeter.

Guia completo de instalacao, execucao e interpretacao de resultados:

```
docs/jmeter-test-guide.md
```

Resultados documentados:

```
docs/load-test-results-20260626.md           baseline sem indices
docs/load-test-index-results-20260626.md     com indices
docs/load-test-pagination-results-20260627.md  com paginacao
```

Relatorios HTML do JMeter: `docs/jmeter-reports/`.

## Documentacao

- `docs/decisions/` - registros de decisao arquitetural (ADRs)
- `docs/superpowers/specs/` - especificacoes de features
- `docs/jmeter-test-guide.md` - guia completo de testes de carga
- `docs/scalability-and-load-balancer/` - setup e roteiro de load balancing local
- `docs/assets/bounded-contexts/` - mapa visual dos Bounded Contexts planejados
- `docs/agents/api-endpoints.md` - contratos e comportamento dos endpoints
