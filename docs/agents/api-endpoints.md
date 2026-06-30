## Application Behavior

- Health: `GET /actuator/health`
- Busca por categoria: `GET /products?categoryId=1&page=0&size=50`
- Busca por nome: `GET /products?name=Product%209999999&page=0&size=50`
- O endpoint aceita `categoryId` OU `name`, nunca ambos.
- Paginacao: `page` default `0`, `size` default `50`, range valido `1..500`.
- Resposta e um slice sem COUNT:

```json
{ "content": [], "page": 0, "size": 50, "count": 50, "hasNext": true }
```

- Estrategia: ler `size + 1` linhas para calcular `hasNext`. Nao adicionar `COUNT(*)`.
- Create product: `POST /products` com `Content-Type: application/json`
  - Obrigatorios: `brandId`, `categoryId`, `sku`, `name`, `slug`, `priceAmount`
  - Opcionais com default: `description` (null), `status` ("ACTIVE"), `currency` ("BRL"), `inventoryQuantity` (0)
  - Retorna `201 Created`. Validacao retorna `400 Bad Request` com Problem Details (RFC 7807).
