# Roteiro de Teste de Carga com JMeter

Este roteiro guia a execucao comparativa dos tres cenarios de performance do projeto e ensina a interpretar os resultados.

## O que este projeto demonstra

O banco de dados e populado com 10 milhoes de produtos, 1.000 marcas e 500 categorias. Dois endpoints sao testados repetidamente sob carga concorrente:

```
GET /products?categoryId=1&page=0&size=50
GET /products?name=Product%209999999&page=0&size=50
```

Cada cenario representa uma evolucao do codigo, disponivel como imagem publica no Docker Hub:

| Cenario | Imagem Docker Hub | O que muda |
| --- | --- | --- |
| Baseline | `fabianofsc/nexus-shopping:baseline` | Sem indices secundarios |
| Indices | `fabianofsc/nexus-shopping:indexes` | Adiciona indices em `category_id` e `name` |
| Paginacao | `fabianofsc/nexus-shopping:pagination` | Limita a 50 registros por resposta |

## Pre-requisitos

- Docker e Docker Compose
- Apache JMeter

```bash
brew install jmeter   # macOS
```

Nao e necessario ter Java, Gradle ou clonar branches diferentes. As imagens estao prontas no Docker Hub.

## Como o banco funciona entre os cenarios

O banco e criado apenas uma vez, no cenario baseline. Os cenarios seguintes aproveitam o mesmo volume do Postgres e o Flyway aplica apenas as migrations novas:

- **Baseline**: cria tabelas e semeia 10 milhoes de produtos. Pode demorar varios minutos.
- **Indices**: aplica somente `CREATE INDEX idx_products_category_id` e `CREATE INDEX idx_products_name`. Rapido.
- **Paginacao**: sem migrations novas. Apenas o codigo da aplicacao muda. Instantaneo.

Nunca derrube o volume entre os cenarios baseline → indexes → pagination. O volume so e recriado ao iniciar o baseline.

## Executar os tres cenarios em sequencia

### Cenario 1: Baseline

Reseta o banco, sobe a stack e executa os dois planos JMeter:

```bash
make load-hub-baseline
```

Aguarde a conclusao. O seed de 10 milhoes de produtos ocorre nesta etapa e leva alguns minutos. O `wait-health` aguarda automaticamente a aplicacao ficar pronta.

### Cenario 2: Indices

Troca apenas a imagem da aplicacao, sem tocar no banco. O Flyway aplica os dois indices:

```bash
make load-hub-indexes
```

### Cenario 3: Paginacao

Troca a imagem novamente. Nenhuma migration nova e aplicada:

```bash
make load-hub-pagination
```

## Metricas a observar no relatorio JMeter

Cada execucao gera um relatorio HTML em `build/jmeter-report/`. As metricas mais importantes para comparar os cenarios:

| Metrica | O que revela |
| --- | --- |
| Vazao (req/s) | Quantas requisicoes o sistema atende por segundo |
| Tempo medio | Latencia media sob 50 usuarios concorrentes |
| P95 | 95% das requisicoes respondem em ate este tempo |
| P99 | 99% das requisicoes respondem em ate este tempo |
| Erros (%) | Deve ser zero em todos os cenarios |

## O que esperar em cada cenario

### Baseline: sem indices

A busca por categoria percorre a tabela inteira para cada requisicao. Cada categoria tem cerca de 20.000 produtos, entao o endpoint combina varredura total com payload grande.

A busca por nome retorna poucos resultados mas tambem percorre a tabela inteira.

| Metrica | Categoria | Nome |
| --- | ---: | ---: |
| Vazao | ~12 req/s | ~11 req/s |
| Tempo medio | ~3.800 ms | ~4.200 ms |
| P95 | ~4.800 ms | ~5.400 ms |

**Como interpretar**: os dois endpoints ficam limitados a 11-12 req/s com P95 acima de 5 segundos. A busca por nome e o caso mais revelador: retorna quase nada, mas ainda e lenta porque o banco nao tem como localizar as linhas sem varrer tudo.

### Indices: localizacao sem varredura

A busca por nome melhora de forma dramatica, porque combina um indice eficiente com um resultado pequeno.

A busca por categoria melhora cerca de 5x, mas ainda retorna 20.000 produtos por requisicao. O gargalo passa a ser materializacao e serializacao do payload.

| Metrica | Categoria (baseline) | Categoria (indices) | Nome (baseline) | Nome (indices) |
| --- | ---: | ---: | ---: | ---: |
| Vazao | ~12 req/s | ~61 req/s | ~11 req/s | ~26.000 req/s |
| Tempo medio | ~3.800 ms | ~750 ms | ~4.200 ms | ~2 ms |
| P95 | ~4.800 ms | ~917 ms | ~5.400 ms | ~5 ms |

**Como interpretar**: indice nao e otimizacao opcional em tabelas grandes. Para a busca por nome, o ganho e de mais de 2.000x em vazao. A busca por categoria melhorou 5x, mas ainda tem gargalo diferente: o volume de linhas retornadas.

### Paginacao: limitar o retorno de linhas

A busca por categoria tem ganho expressivo. Com `size=50`, a aplicacao materializa e serializa apenas 50 produtos em vez de 20.000.

A busca por nome tem ganho pequeno, porque ela ja retornava poucos resultados.

| Metrica | Categoria (indices) | Categoria (paginada) | Nome (indices) | Nome (paginado) |
| --- | ---: | ---: | ---: | ---: |
| Vazao | ~61 req/s | ~2.960 req/s | ~26.000 req/s | ~28.000 req/s |
| Tempo medio | ~750 ms | ~16 ms | ~2 ms | ~2 ms |
| P95 | ~917 ms | ~26 ms | ~5 ms | ~4 ms |

**Como interpretar**: indices e paginacao resolvem problemas diferentes. Indices reduzem o custo de **localizar** linhas. Paginacao reduz o custo de **retornar** linhas. Para a categoria, os dois juntos foram necessarios para atingir P95 de 26 ms e quase 3.000 req/s.

## Resumo da progressao

| Cenario | Vazao (categoria) | P95 (categoria) | Vazao (nome) | P95 (nome) |
| --- | ---: | ---: | ---: | ---: |
| Sem indice | ~12 req/s | ~4.800 ms | ~11 req/s | ~5.400 ms |
| Com indice | ~61 req/s | ~917 ms | ~26.000 req/s | ~5 ms |
| Com paginacao | ~2.960 req/s | ~26 ms | ~28.000 req/s | ~4 ms |

## Comandos uteis

Subir apenas a stack sem rodar JMeter (banco preservado):

```bash
make hub-baseline    # troca para baseline, banco preservado
make hub-indexes     # troca para indexes, banco preservado
make hub-pagination  # troca para pagination, banco preservado
```

Resetar o banco explicitamente antes de subir:

```bash
make hub-reset-baseline    # derruba volume e sobe baseline
make hub-reset-indexes     # derruba volume e sobe indexes
make hub-reset-pagination  # derruba volume e sobe pagination
```

Rodar JMeter contra a stack ja no ar:

```bash
make jmeter-all
make jmeter-category
make jmeter-name
```

Verificar saude da aplicacao:

```bash
make health
```

## Onde ficam os relatorios

```text
build/jmeter-results/   # arquivos .jtl com dados brutos
build/jmeter-report/    # relatorios HTML
```

Esses arquivos sao artefatos locais e nao devem ser commitados.

## Documentacao dos resultados reais

- `docs/load-test-results-20260626.md` - baseline sem indices
- `docs/load-test-index-results-20260626.md` - com indices
- `docs/load-test-pagination-results-20260627.md` - com paginacao
