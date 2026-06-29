# ADR: Adotar ktlint como primeira biblioteca de lint Kotlin

**Status:** Aceita
**Data:** 2026-06-29
**PRD relacionado:** decisao registrada diretamente em `docs/decisions`

---

## Contexto

O projeto usa Kotlin 2.2.21, Java 21, Gradle Wrapper 9.6.0, Spring Boot 4.1.0 e Gradle Kotlin DSL. A arquitetura segue Ports and Adapters, com foco didatico em qualidade incremental, testes e comparacao de desempenho.

Ainda nao ha uma biblioteca de lint ou formatacao Kotlin configurada no projeto. A verificacao principal continua sendo:

```bash
rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew build
```

A discussao teve estes criterios:

- A adocao deve ser incremental.
- Deve existir um comando de autoformat para uso local.
- O check obrigatorio nao deve aplicar autoformat automaticamente.
- O lint nao deve entrar no `build` na primeira fase.
- Ferramentas alpha devem ser evitadas.
- Uma segunda camada de analise estatica pode ser considerada no futuro.

---

## Opcoes avaliadas

### ktlint com `org.jlleitschuh.gradle.ktlint`

`ktlint` e um linter Kotlin com formatter embutido, suporte a `.editorconfig`, reporters e integracoes com Gradle. O plugin `org.jlleitschuh.gradle.ktlint` cria tarefas convenientes para check e formatacao e so ativa em projetos com plugin Kotlin aplicado.

Esta opcao tem baixo atrito para a stack atual e atende diretamente ao objetivo de padronizar Kotlin e Gradle Kotlin DSL.

### Spotless

Spotless e um orquestrador de formatacao para varias linguagens e pode executar `ktlint` ou `ktfmt` para Kotlin.

Foi considerado util para um futuro em que o projeto queira formatar tambem Markdown, SQL, YAML ou outros arquivos. Para o momento atual, foi rejeitado por ser mais amplo do que a necessidade imediata.

### detekt

`detekt` oferece analise estatica Kotlin mais profunda, incluindo complexidade, code smells e regras de manutencao.

A versao estavel `1.23.8` nao e a melhor escolha para Kotlin 2.2.x, pois foi compilada contra Kotlin 2.0.21. As versoes `2.0.0-alpha.x` estao mais alinhadas a Kotlin 2.2+ e Gradle 9, mas ainda sao alpha. Como a tolerancia a ferramentas alpha foi definida como baixa, `detekt` fica adiado.

### ktfmt

`ktfmt` e um formatador deterministico e pouco configuravel. Ele reduz debates de estilo, mas pode alterar mais agressivamente a aparencia do codigo e nao e uma biblioteca de lint completa.

Foi rejeitado nesta fase para evitar uma mudanca visual mais forte do que o necessario.

### Qodana e DiKTat

Qodana e uma plataforma mais ampla de analise estatica e CI. DiKTat e um conjunto de regras Kotlin mais rigido e opinativo. Ambos foram considerados alem do escopo da primeira fase.

---

## Decisao

Adotar **ktlint via plugin Gradle `org.jlleitschuh.gradle.ktlint`** como primeira biblioteca de lint Kotlin do projeto.

A primeira implementacao deve:

- adicionar o plugin ao `build.gradle.kts`;
- usar versao explicita do plugin;
- adicionar uma `.editorconfig` minima no root;
- disponibilizar as tarefas padrao de check e format;
- documentar comandos com `rtk`;
- manter o lint fora do `build` e do `check` nesta fase inicial.

Comandos esperados:

```bash
rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew ktlintCheck
rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew ktlintFormat
```

O `ktlintFormat` existe para uso local e revisao mecanica. O `ktlintCheck` valida estilo sem alterar arquivos.

---

## Plano de adocao

### Fase 1: configuracao opt-in

Adicionar `ktlint`, `.editorconfig` e comandos documentados. Rodar `ktlintCheck` de forma isolada para medir o impacto.

Se houver violacoes existentes, revisar o relatorio antes de aplicar `ktlintFormat`. O objetivo e evitar misturar decisao de ferramenta com grandes diffs mecanicos nao revisados.

### Fase 2: enforcement no ciclo principal

Depois que o codigo estiver formatado e a equipe estiver confortavel com a ferramenta, acoplar `ktlintCheck` ao ciclo principal de qualidade, preferencialmente via `check`.

### Fase 3: reavaliar analise estatica

Reavaliar `detekt` quando houver uma versao estavel alinhada a Kotlin 2.2+ e Gradle 9. A segunda camada deve focar em complexidade e manutencao, nao em formatacao.

---

## Consequencias

Pontos positivos:

- O projeto ganha padronizacao Kotlin com baixo risco.
- A formatacao fica automatizavel, mas nao automatica no CI.
- A decisao preserva o comando principal `build` sem novos bloqueios imediatos.
- A solucao segue a stack atual de Kotlin, Gradle e Spring sem introduzir plataforma externa.
- A porta para `detekt` continua aberta sem assumir dependencia alpha.

Trade-offs:

- Na primeira fase, o lint nao protege o `build`; desenvolvedores precisam rodar `ktlintCheck` explicitamente.
- `ktlint` cobre estilo e formatacao, mas nao substitui uma ferramenta de analise estatica profunda.
- A introducao de `.editorconfig` pode revelar diferencas de estilo entre IDE e Gradle.

Alternativas rejeitadas:

- Spotless foi rejeitado nesta fase por ser mais amplo que a necessidade atual.
- detekt foi rejeitado agora por incompatibilidade pratica da linha estavel com Kotlin 2.2.x e pela decisao de evitar alpha.
- ktfmt foi rejeitado por ser mais formatter do que linter e por poder gerar mudanca visual maior.
- Qodana e DiKTat foram rejeitados por serem pesados demais para a primeira camada de lint.

