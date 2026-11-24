# pedidos-reportes-service

Servicio web Spring Boot (sesión 12) que publica reportes de pedidos en JSON leyendo la base `pedidosdb`
con **Spring JDBC**. Lo consume `sistema-pedidos-springboot` mediante `PedidoReporteClient` (RestClient).

- Puerto: **8083** · context path: `/pedidos-reportes-service`
- Base: MySQL `pedidosdb` (solo lectura). Credenciales por variables `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.

## Recursos

| Recurso | Origen |
|---|---|
| `GET /api/v1/pedidos/{id}`, `GET /api/v1/pedidos?cliente=`, `GET /api/v1/pedidos/estadisticas/resumen`, `GET /api/v1/pedidos/estadisticas/productos`, `GET /api/v1/orders[/resumen]` | Proyecto base (sesión 12) |
| **`GET /api/v1/pedidos/revisiones?estado=PENDIENTE\|REVISADO`** | **PA4** |
| **`GET /api/v1/pedidos/estadisticas/revisiones`** | **PA4** |

### PA4 — contrato

`GET /api/v1/pedidos/revisiones?estado=REVISADO` → `200 application/json`

```json
[{ "id": 12, "cliente": "Ana Torres", "productoCodigo": "LAP-001", "productoNombre": "Laptop",
   "cantidad": 2, "total": 5000.00, "fecha": "2026-11-23T10:02:11", "revisado": true,
   "estadoRevision": "REVISADO", "observacion": "Validado con el cliente",
   "fechaRevision": "2026-11-23T10:15:30" }]
```

- Sin `estado`: todos los pedidos. `estado` no distingue mayúsculas.
- `estado` inválido → `400` `application/problem+json` (`title: "Parámetro inválido"`).
- Error de base de datos → `503` ProblemDetail sin detalles técnicos.
- `observacion` y `fechaRevision` solo aparecen cuando `revisado = true`.

`GET /api/v1/pedidos/estadisticas/revisiones` → `200`

```json
{ "totalPedidos": 4, "totalRevisados": 1, "totalPendientes": 3, "porcentajeRevisado": 25.00 }
```

El cálculo (`pendientes = total − revisados`, porcentaje con 2 decimales HALF_UP, 0.00 si no hay pedidos)
está en `PedidoRevisionReporteServiceImpl`; el Controller solo convierte a DTO.

### Capas (PA4)

```text
PedidoRevisionReporteController -> PedidoRevisionReporteService(Impl) -> PedidoRevisionReporteJdbcRepository -> MySQL
         DTO JSON                    valida estado / calcula %              SQL con parámetros (:revisado)
```

## Ejecutar y probar

```bash
mvn spring-boot:run          # requiere MySQL con pedido.revisado y pedido_revision (ver sistema-pedidos/db)
mvn clean test               # usa H2 (src/test/resources), no requiere MySQL
```

Pruebas PA4: `PedidoRevisionReporteControllerTest`, `PedidoRevisionReporteServiceImplTest`,
`PedidoRevisionReporteIntegrationTest`.

Colección Postman: `postman/PA4-pedidos-reportes.postman_collection.json`.
