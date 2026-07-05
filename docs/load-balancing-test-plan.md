# Roteiro de Teste - Load Balancing com NGINX

Procedimento para subir a infra, executar os testes e verificar que o NGINX esta
de fato distribuindo as requisicoes entre as 3 instancias da aplicacao.

Para a descricao da topologia, do `nginx.conf` e dos algoritmos, veja
`docs/load-balancing-nginx.md`. Este documento foca no **processo de teste**.

## 1. Objetivo e escopo

Validar que:

1. A stack sobe sem conflito de portas (banco, 3 apps, nginx).
2. Apenas o NGINX e acessivel pelo host; as instancias nao sao expostas diretamente.
3. O balanceamento round-robin distribui as requisicoes de forma ~uniforme (~33% cada).
4. Trocar o algoritmo (ex.: weighted) muda a distribuicao de forma previsivel.
5. A aplicacao inicia corretamente contra PostgreSQL (`ddl-auto=validate`).

## 2. Pre-requisitos

- Docker e Docker Compose em execucao.
- Portas livres no host: `8080` (nginx) e `5432` (postgres).
- `curl` instalado. `jq` e opcional (o script tem fallback).

Checagem rapida de portas antes de comecar:

```bash
lsof -nP -iTCP:8080 -sTCP:LISTEN   # deve retornar vazio
lsof -nP -iTCP:5432 -sTCP:LISTEN   # deve retornar vazio
```

## 3. Visao geral do processo

```
subir infra  ->  smoke test  ->  teste de isolamento  ->  teste de distribuicao
     |                                                              |
  (build+up)                                              (round-robin e weighted)
                                                                    |
                                                               teardown
```

## 4. Subir a infra

O seed padrao e de 10.000.000 produtos; para testar o balanceamento isso e
desnecessario e lento. Use um seed reduzido:

```bash
PRODUCT_SEED_COUNT=1000 docker compose up -d --build
```

Aguarde o health responder `200` atraves do NGINX:

```bash
until [ "$(curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/actuator/health)" = "200" ]; do
  echo "aguardando health..."; sleep 5
done
echo "health OK"
```

Confirme que os 5 containers estao no ar (3 apps `Up`, nginx `Up`, postgres `Up (healthy)`):

```bash
docker compose ps
```

> Se alguma app ficar em `Restarting`, veja os logs: `docker compose logs app1`.
> Ver a secao 8 (Troubleshooting).

## 5. Executar os testes

### 5.1 Smoke test

O endpoint deve responder com o hostname da instancia e um timestamp:

```bash
curl -s http://localhost:8080/instance-info
# {"hostname":"app1","timestamp":"2026-07-05T18:53:05.229Z"}
```

O NGINX deve devolver o header `X-Upstream` com o endereco da instancia que atendeu:

```bash
curl -s -D - -o /dev/null http://localhost:8080/instance-info | grep -i x-upstream
# X-Upstream: 192.168.x.y:8080
```

### 5.2 Teste de isolamento (apps nao expostas ao host)

Nenhuma instancia deve responder diretamente pelo host - apenas via NGINX na 8080.
As portas 8081-8083 nao existem no host e devem falhar:

```bash
for p in 8081 8082 8083; do
  curl -s -o /dev/null -w "porta $p -> %{http_code}\n" --max-time 3 http://localhost:$p/instance-info || echo "porta $p -> sem resposta (ok)"
done
```

Esperado: `000` / "sem resposta" para todas.

### 5.3 Teste de distribuicao (round-robin)

Alternancia imediata em requisicoes sequenciais:

```bash
for n in $(seq 1 9); do
  curl -s http://localhost:8080/instance-info | sed -n 's/.*"hostname":"\([^"]*\)".*/\1/p'
done
# esperado: app1 app2 app3 app1 app2 app3 app1 app2 app3
```

Resumo agregado da distribuicao:

```bash
./scripts/test-lb.sh 30
```

Saida esperada (~33% por instancia):

```
Distribuicao por instancia (total: 30):
-------------------------------------------------
  app3                             10   33.33%
  app2                             10   33.33%
  app1                             10   33.33%
-------------------------------------------------
  TOTAL                            30  100.00%
```

> A alternancia perfeita depende de `worker_processes 1` no `nginx.conf` (definido
> para este demo). Com `worker_processes auto`, cada worker mantem seu proprio
> contador e a distribuicao so tende a ~uniforme no agregado de muitas requisicoes.

### 5.4 Teste de troca de algoritmo (weighted)

Comprova que o balanceamento reage a mudanca de algoritmo. Em `nginx/nginx.conf`,
comente os 3 `server` do round-robin e descomente o bloco weighted `3:1:1`:

```nginx
        # server app1:8080;
        # server app2:8080;
        # server app3:8080;

        server app1:8080 weight=3;
        server app2:8080 weight=1;
        server app3:8080 weight=1;
```

Recarregue e teste:

```bash
docker compose exec nginx nginx -s reload   # ver ressalva de macOS abaixo
./scripts/test-lb.sh 50
```

Saida esperada (~60/20/20, refletindo os pesos):

```
Distribuicao por instancia (total: 50):
-------------------------------------------------
  app1                             30   60.00%
  app3                             10   20.00%
  app2                             10   20.00%
-------------------------------------------------
  TOTAL                            50  100.00%
```

Depois reverta para round-robin (descomente os 3 `server` sem peso, comente o weighted)
e recarregue novamente.

## 6. Criterios de aceite

| # | Criterio | Como verificar | Esperado |
| --- | --- | --- | --- |
| 1 | Stack sobe sem conflito de portas | `docker compose ps` | 5 containers no ar |
| 2 | App inicia contra PostgreSQL | health via nginx | `200` |
| 3 | Apps nao expostas ao host | curl em 8081-8083 | sem resposta (`000`) |
| 4 | Round-robin alterna | 9 curls sequenciais | app1/app2/app3 em rodizio |
| 5 | Distribuicao ~uniforme | `./scripts/test-lb.sh 30` | ~33% por instancia |
| 6 | Troca de algoritmo funciona | weighted 3:1:1 + reload | ~60/20/20 |
| 7 | Falha clara sem nginx | `./scripts/test-lb.sh 30 http://localhost:9999/x` | exit != 0 + mensagem |

## 7. Teardown

```bash
docker compose down            # mantem o volume do Postgres
docker compose down -v         # remove tambem o volume (reseta o banco)
```

## 8. Troubleshooting

- **Apps em `Restarting` / health nunca fica `200`**: veja `docker compose logs app1`.
  Se aparecer `Schema validation: wrong column type ... currency`, o volume do Postgres
  esta com o schema antigo (`CHAR(3)`). Rode `docker compose down -v` e suba de novo
  (a migration atual cria `VARCHAR(3)`).

- **`nginx -s reload` falha com `open() ... No such file or directory` (macOS)**: o
  `nginx.conf` e um bind mount de arquivo unico e alguns editores salvam via
  "atomic save", trocando o inode e deixando o mount stale. Restabeleca com:

  ```bash
  docker compose restart nginx
  ```

- **Flyway checksum mismatch apos alterar migration**: uma migration ja aplicada foi
  editada. Resete o volume: `docker compose down -v && docker compose up -d --build`.

- **Teste muito lento no primeiro `up`**: o seed padrao e 10M de produtos. Use
  `PRODUCT_SEED_COUNT=1000` conforme a secao 4.
