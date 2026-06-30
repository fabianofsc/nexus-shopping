# Nexus Shopping Backend

Backend REST API educacional construido com Kotlin, Java 21, Spring Boot 4, Actuator, Flyway, PostgreSQL e Spring Data JPA.

O projeto evolui de forma incremental, cobrindo diferentes topicos de engenharia de software em aulas progressivas: performance de banco de dados, arquitetura hexagonal, tratamento de erros padronizado, logging estruturado, e outros. Cada topico e introduzido em uma branch separada e publicado como imagem Docker Hub para uso independente do codigo-fonte.

## Topicos e Branches

| Branch | Imagem Docker Hub | Topico |
| --- | --- | --- |
| `missing-index-performance-baseline` | `:baseline` | Performance sem indices secundarios |
| `add-product-query-indexes` | `:indexes` | Impacto de indices de leitura |
| `add-products-pagination` | `:pagination` | Paginacao e custo de retorno de linhas |
| `hexagonal-architecture` | `:latest` | Arquitetura hexagonal (Ports and Adapters) |
| `main` | `:latest` | Versao estavel mais recente |

## Architecture

O projeto segue arquitetura hexagonal (Ports and Adapters), aplicada de forma incremental.

```
product/
  domain/          -> tipos de negocio puros (Product, ProductPage)
  application/
    port/outbound/ -> ProductRepositoryPort (interface)
    usecase/       -> ProductSearchUseCase, ProductCreateUseCase, excecoes tipadas
  adapter/
    inbound/http/  -> ProductController, DTOs de requisicao
    outbound/jpa/ -> ProductJpaRepositoryAdapter, ProductEntity, SpringDataProductRepository
```

Restricoes de design:
- Domain e use cases sem imports de Spring, JDBC ou JPA.
- JPA fica isolado no adapter outbound, que implementa `ProductRepositoryPort`, mapeia `ProductEntity` para `Product` e usa `@Query` JPQL nas consultas de leitura para manter explicito o shape das queries de performance.
- `ProductValidationException` lancada pelos use cases; o controller captura apenas este tipo.
- Validacao nos use cases para reuso por qualquer adaptador futuro (CLI, fila, batch).

## Requirements

Para testes de carga apenas:

- Docker e Docker Compose
- Apache JMeter

Para desenvolvimento local:

- Java 21
- Docker e Docker Compose
- Gradle Wrapper (incluido como `./gradlew`)

Instalar JMeter no macOS:

```bash
brew install jmeter
```

## Database

PostgreSQL via Docker Compose:

```bash
docker compose up -d postgres
```

Configuracoes padrao:

- URL: `jdbc:postgresql://localhost:5432/nexus_shopping`
- Database: `nexus_shopping`
- User: `nexus`
- Password: `nexus`

O Flyway executa automaticamente ao iniciar a aplicacao e cria:

- Tabelas: `brands`, `categories`, `products`
- Seed: 10.000.000 produtos, 1.000 marcas, 500 categorias (configuravel via `PRODUCT_SEED_COUNT`)
- Indexes: `idx_products_category_id`, `idx_products_name`

Para resetar o volume local apos mudancas de migrations:

```bash
docker compose down -v
docker compose up -d postgres
```

## Run

```bash
docker compose up -d postgres
./gradlew bootRun
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

## Docker Image

O projeto usa o task nativo do Spring Boot para geracao de imagem OCI via Cloud Native Buildpacks. Nao ha Dockerfile.

Build local:

```bash
./gradlew bootBuildImage --imageName nexus-shopping:local
APP_IMAGE=nexus-shopping:local docker compose up -d postgres app
```

Push dos cenarios para o Docker Hub:

```bash
make push-baseline
make push-indexes
make push-pagination
```

## Test

```bash
./gradlew build
```

Os testes automatizados validam:

- Spring Boot inicia com o health endpoint do Actuator.
- Flyway executa automaticamente.
- O endpoint de produtos funciona com seed de teste reduzido.
- Migrations portaveis entre PostgreSQL e H2.
- Indexes de leitura presentes sem constraints UNIQUE indesejadas.

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

## Documentation

- `docs/decisions/` - registros de decisao arquitetural (ADRs)
- `docs/superpowers/specs/` - especificacoes de features
- `docs/jmeter-test-guide.md` - guia completo de testes de carga
