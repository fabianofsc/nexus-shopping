## Docker Hub

- Imagens publicadas automaticamente pelo GitHub Actions em cada branch.
- Para iniciar um cenario sem build local:

```bash
make start-baseline     # reseta DB, pull da imagem, aguarda health
make start-indexes      # troca imagem, mantem DB, aguarda health
make start-pagination   # troca imagem, sem novas migrations, aguarda health
```

- Reset de DB: `make hub-reset-baseline` / `hub-reset-indexes` / `hub-reset-pagination`
- Push manual: `make push-baseline` / `push-indexes` / `push-pagination`

## JMeter

- JMeter e tooling externo. Sempre usar o wrapper: `scripts/jmeter.sh`
- Plans: `load-tests/jmeter/products-by-category.jmx`, `load-tests/jmeter/products-by-name.jmx`
- Perfil padrao: 50 threads, ramp-up 20s, duracao 120s.

```bash
make jmeter-all SCENARIO=baseline
make jmeter-category SCENARIO=indexes
make jmeter-name SCENARIO=pagination
```

- Se JMeter reportar `java.net.SocketException: Operation not permitted`, reexecutar com permissao elevada.
