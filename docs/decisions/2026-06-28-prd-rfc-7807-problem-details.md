# ADR: Usar RFC 7807 Problem Details para erros da API

**Status:** Aceita
**Data:** 2026-06-28
**PRD relacionado:** `docs/superpowers/specs/2026-06-28-prd-error-handling-problem-details-design.md`

---

## Contexto

A API precisa de um formato padronizado de resposta para falhas de validacao e erros inesperados de servidor. O mapeamento atual no controller usa blocos `try/catch` manuais e o formato padrao de erro do Spring.

O objetivo e mover a traducao de erros HTTP para um `ControllerAdvice`, manter a validacao nos use cases da aplicacao e impedir que respostas 5xx vazem detalhes internos.

Dois formatos de resposta foram considerados:

1. Um DTO customizado do projeto com campos como `status`, `error`, `message`, `path` e `timestamp`.
2. RFC 7807 Problem Details, suportado pelo Spring 6+.

---

## Decisao

Usar **RFC 7807 Problem Details** como formato de resposta de erro da API.

Exemplo:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "sku must not be blank.",
  "instance": "/products"
}
```

O atributo `type` e o identificador do tipo de problema. Neste projeto, a primeira implementacao deve usar:

```json
{
  "type": "about:blank"
}
```

`about:blank` significa que o erro usa o significado padrao do status HTTP. Ainda nao ha uma categoria de problema especifica do projeto.

No exemplo acima, a categoria e definida por:

- `status: 400`
- `title: "Bad Request"`

A explicacao especifica da requisicao fica em:

- `detail: "sku must not be blank."`

O caminho da requisicao fica em:

- `instance: "/products"`

Se o projeto precisar de categorias de erro mais ricas no futuro, `type` pode evoluir para URLs estaveis, por exemplo:

```json
{
  "type": "https://nexus-shopping.dev/problems/product-validation"
}
```

Isso fica propositalmente adiado ate a API ter uma necessidade real de multiplas categorias de problema documentadas.

---

## Consequencias

Pontos positivos:

- A API usa um formato de erro padrao da industria.
- A implementacao fica alinhada ao suporte de `ProblemDetail` no Spring 6+/Spring Boot 4.
- As respostas de erro se tornam previsiveis para clientes.
- O projeto evita criar um DTO customizado antes de haver uma necessidade clara.
- Categorias futuras podem ser adicionadas pelo campo `type` sem mudar o modelo geral da resposta.

Trade-offs:

- Os nomes dos campos sao os termos da RFC (`detail`, `instance`) em vez de nomes customizados (`message`, `path`).
- `about:blank` pode parecer estranho para leitores que nao conhecem RFC 7807, por isso esta ADR documenta seu significado.
- A primeira implementacao intencionalmente nao fornece URLs customizadas de problema.

Alternativa rejeitada:

- Um DTO customizado foi rejeitado porque duplicaria um formato padrao ja suportado pelo Spring e criaria um contrato especifico do projeto sem beneficio claro.

