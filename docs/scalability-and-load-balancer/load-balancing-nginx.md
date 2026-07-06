# Load Balancing com NGINX

> **Proposito:** guia de referencia deste setup. Responde "o que existe aqui e
> como esta montado?". E o documento de leitura para entender ou alterar a
> configuracao.
>
> **Conteudo:** topologia, componentes (quais arquivos, com que papel), como
> subir, o endpoint `/instance-info`, como testar a distribuicao, os algoritmos
> de balanceamento (quando usar cada um) e como troca-los.
>
> **Documento companheiro:** `load-balancing-test-plan.md` -- roteiro passo a
> passo para *validar* que o balanceamento funciona.

Setup didatico de balanceamento de carga com NGINX roteando entre 3 instancias
da aplicacao, sem Kubernetes. Objetivo: observar na pratica como um reverse proxy
distribui requisicoes e como diferentes algoritmos mudam essa distribuicao.

## Topologia

```
                  host:8080
                     |
                 +---------+
                 |  nginx  |   (unico servico exposto ao host)
                 +---------+
                  /   |   \
                 /    |    \        rede interna "backend"
          +------+ +------+ +------+
          | app1 | | app2 | | app3 |   (expose 8080, sem porta no host)
          +------+ +------+ +------+
                  \   |   /
                 +----------+
                 | postgres |
                 +----------+
```

- Apenas o NGINX publica porta no host (`8080:80`).
- `app1`, `app2` e `app3` saem do mesmo build/imagem (`Dockerfile`) e so sao
  acessiveis pela rede interna `backend` (`expose: 8080`, sem `ports`).
- O Postgres permanece com a mesma configuracao do restante do projeto
  (imagem, credenciais, volume, healthcheck).

## Componentes

| Arquivo | Papel |
| --- | --- |
| `docker-compose.yml` | Define `app1/app2/app3`, `nginx`, `postgres` e a rede `backend`. |
| `Dockerfile` | Build multi-stage (JDK 21 -> JRE 21) usado pelas 3 instancias. |
| `nginx/nginx.conf` | Upstream `backend` + algoritmos de balanceamento (round-robin ativo). |
| `src/.../infra/http/InstanceInfoController.kt` | Endpoint `GET /instance-info`. |
| `scripts/test-lb.sh` | Dispara N requisicoes e resume a distribuicao por instancia. |

## Subir o setup

```bash
docker compose up -d --build
```

Isso constroi a imagem a partir do `Dockerfile` e sobe banco, as 3 instancias e o NGINX.
So a porta `8080` do host fica exposta (pelo NGINX).

Health:

```bash
curl http://localhost:8080/actuator/health
```

## Endpoint /instance-info

Retorna o hostname do container que atendeu a requisicao e um timestamp, para
identificar visualmente a alternancia entre instancias:

```bash
curl -s http://localhost:8080/instance-info
# {"hostname":"app1","timestamp":"2026-07-05T12:00:00.000Z"}
```

Repetindo o curl, o `hostname` deve alternar entre `app1`, `app2` e `app3`.
O NGINX tambem devolve o header `X-Upstream` com o endereco da instancia:

```bash
curl -s -D - -o /dev/null http://localhost:8080/instance-info | grep -i x-upstream
```

## Testar a distribuicao

O script `scripts/test-lb.sh` dispara varias requisicoes silenciosamente e imprime
um resumo agregado (contagem e percentual por instancia, ordenado do maior para o menor):

```bash
./scripts/test-lb.sh                 # 30 requisicoes (default) para /instance-info
./scripts/test-lb.sh 100             # 100 requisicoes
./scripts/test-lb.sh 100 http://localhost:8080/instance-info
```

Parametros (ambos opcionais):

1. numero de requisicoes (default `30`)
2. URL alvo (default `http://localhost:8080/instance-info`)

Saida de exemplo (round-robin):

```
Distribuicao por instancia (total: 30):
-------------------------------------------------
  app1                             10   33.33%
  app2                             10   33.33%
  app3                             10   33.33%
-------------------------------------------------
  TOTAL                            30  100.00%
```

O script valida que o NGINX responde antes de iniciar o loop; se nao houver resposta,
falha com mensagem clara e exit code diferente de zero. Usa apenas `bash`, `curl` e
utilitarios POSIX (`sort`, `uniq`, `awk`); aproveita `jq` se instalado, com fallback
para `grep`/`sed`.

## Trocar o algoritmo de balanceamento

Todos os algoritmos vivem no bloco `upstream backend` de `nginx/nginx.conf`. Trocar
significa apenas comentar/descomentar linhas -- nao e preciso reescrever o bloco:

| Algoritmo | Quando usar |
| --- | --- |
| Round-robin (default, ativo) | Rodizio uniforme; instancias equivalentes. |
| Weighted round-robin (`weight=N`) | Instancias com capacidade diferente. |
| `least_conn` | Duracao das requisicoes varia muito. |
| `ip_hash` | Sticky sessions por IP, sem estado compartilhado. |
| `hash $request_uri consistent` | Afinidade de cache: mesma URI -> mesma instancia. |
| `random two least_conn` | Upstreams grandes; sorteia 2 e escolhe a de menos conexoes. |

Depois de editar o arquivo, recarregue o NGINX **sem downtime**:

```bash
docker compose exec nginx nginx -s reload
```

> Nota (macOS/Docker Desktop/OrbStack): o `nginx.conf` e montado como arquivo unico
> (bind mount de arquivo). Alguns editores salvam via "atomic save" (escrevem um
> arquivo temporario e renomeiam), o que troca o inode e pode deixar o mount stale --
> o `nginx -s reload` entao falha com `open() ... failed (No such file or directory)`
> e a config antiga continua ativa. Se isso acontecer, restabeleca o mount com:
>
> ```bash
> docker compose restart nginx
> ```
>
> O `restart` tem um blip de ~1s; o `reload` e zero-downtime quando o mount esta ok.

Rode o `test-lb.sh` novamente para comparar a nova distribuicao (ex.: com weighted
`3:1:1`, a distribuicao fica ~60/20/20, refletindo os pesos configurados).

## Derrubar

```bash
docker compose down            # mantem o volume do Postgres
docker compose down -v         # remove tambem o volume (reseta o banco)
```
