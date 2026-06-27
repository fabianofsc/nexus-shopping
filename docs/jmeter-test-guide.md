# Guia de Testes com JMeter

## Smoke Test ou Load Test?

Este guia deve ser chamado de guia de teste de carga com JMeter, nao de smoke test.

Smoke test e uma checagem rapida para confirmar que a aplicacao subiu e responde. Neste projeto, o smoke test e:

```bash
make health
```

ou diretamente:

```bash
curl http://localhost:8080/actuator/health
```

Os testes com JMeter deste projeto exercitam concorrencia, latencia, vazao e tamanho de resposta. Portanto, eles sao testes de carga comparativos.

## Cenarios

O projeto possui tres branches didaticas:

| Cenario | Branch | Objetivo |
| --- | --- | --- |
| Baseline | `missing-index-performance-baseline` | Medir o comportamento sem indices secundarios. |
| Indexes | `add-product-query-indexes` | Medir o impacto dos indices de leitura. |
| Pagination | `add-products-pagination` | Medir o impacto da paginacao depois dos indices. |

A versao mais nova do projeto e a versao paginada. A `main` deve representar essa ultima versao.

## Imagem Docker

A imagem da ultima versao deve ser gerada a partir da branch `main`, que representa a versao paginada consolidada, e publicada localmente como:

```text
nexus-shopping:latest
```

Use:

```bash
make image-latest
```

Esse alvo troca para a branch `main` e executa o task nativo do Spring Boot:

```bash
./gradlew bootBuildImage --imageName nexus-shopping:latest
```

Nao existe Dockerfile neste projeto. A imagem e criada pela opcao nativa do Spring Boot com Cloud Native Buildpacks.

## Executar Cada Cenario

Cada alvo abaixo faz o fluxo completo:

1. troca para a branch correta;
2. gera a imagem Docker da aplicacao;
3. recria o banco com `docker compose down -v`;
4. sobe `postgres` e `app`;
5. espera `/actuator/health`;
6. executa os dois testes JMeter;
7. gera `.jtl` e relatorio HTML.

Baseline sem indices:

```bash
make load-baseline
```

Indices:

```bash
make load-indexes
```

Paginacao:

```bash
make load-pagination
```

Ultima versao, usando a imagem `nexus-shopping:latest`:

```bash
make load-latest
```

## Executar Apenas a Stack

Para subir um cenario sem rodar JMeter:

```bash
make stack-baseline
make stack-indexes
make stack-pagination
```

Para resetar o banco antes de subir:

```bash
make stack-reset-baseline
make stack-reset-indexes
make stack-reset-pagination
```

Para subir a ultima versao com a imagem `nexus-shopping:latest`:

```bash
make stack-latest
```

## Executar JMeter Contra a Aplicacao Atual

Depois de subir a aplicacao, rode:

```bash
make jmeter-category
make jmeter-name
make jmeter-all
```

Variaveis uteis:

```bash
make jmeter-all THREADS=50 RAMP_UP=20 DURATION=120
make jmeter-category CATEGORY_ID=1
make jmeter-name NAME='Product 9999999'
make jmeter-all SCENARIO=manual RUN_ID=experimento-01
```

Na branch de paginacao, tambem e possivel controlar:

```bash
make jmeter-all PAGE=0 SIZE=50
```

Nas branches baseline e indexes, `PAGE` e `SIZE` sao enviados para o JMeter, mas os planos daquelas branches nao usam esses parametros.

## Saida dos Relatorios

Os resultados sao gravados em:

```text
build/jmeter-results/
build/jmeter-report/
```

Exemplo:

```text
build/jmeter-results/products-by-category-baseline-20260627-173237.jtl
build/jmeter-report/products-by-category-baseline-20260627-173237/index.html
```

Os arquivos dentro de `build/` sao artefatos locais e nao devem ser commitados.

## Comparacao Esperada

Ordem recomendada:

```bash
make load-baseline
make load-indexes
make load-pagination
```

Leitura esperada:

- Baseline mostra o custo de consultar tabela grande sem indices.
- Indexes mostram o ganho ao localizar linhas por indice.
- Pagination mostra o ganho ao limitar o volume de linhas retornadas e serializadas.

O cenario de categoria e o melhor para observar o ganho da paginacao, porque sem pagina ela retorna muitos produtos por requisicao.
