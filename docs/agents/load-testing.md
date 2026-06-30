## Docker Hub

- Imagens publicadas automaticamente pelo GitHub Actions em cada branch.
- Para iniciar um cenario sem build local:

```bash
rtk make start-baseline     # reseta DB, pull da imagem, aguarda health
rtk make start-indexes      # troca imagem, mantem DB, aguarda health
rtk make start-pagination   # troca imagem, sem novas migrations, aguarda health
```

- Reset de DB: `rtk make hub-reset-baseline` / `hub-reset-indexes` / `hub-reset-pagination`
- Push manual: `rtk make push-baseline` / `push-indexes` / `push-pagination`

## JMeter

- JMeter e tooling externo. Sempre usar o wrapper: `scripts/jmeter.sh`
- Plans: `load-tests/jmeter/products-by-category.jmx`, `load-tests/jmeter/products-by-name.jmx`
- Perfil padrao: 50 threads, ramp-up 20s, duracao 120s.

```bash
rtk make jmeter-all SCENARIO=baseline
rtk make jmeter-category SCENARIO=indexes
rtk make jmeter-name SCENARIO=pagination
```

- Se JMeter reportar `java.net.SocketException: Operation not permitted`, reexecutar com permissao elevada.
