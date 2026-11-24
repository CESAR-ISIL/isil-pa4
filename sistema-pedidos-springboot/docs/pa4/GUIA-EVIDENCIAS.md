# PA4 — Guía para generar las evidencias

Siga estos pasos en su máquina y pegue las capturas en el informe, en la sección indicada.
Use las consultas de `db/pa4-consultas-evidencia.sql` (reemplace `:id` por el ID real).

> Consejo: en cada captura deje visible la URL, el usuario conectado o la consola, y la hora.

## 0. Preparación

```bash
# MySQL activo con la base pedidosdb
mysql -u root -p < "Sesión 5/sistema-pedidos-springboot/db/pa4-revision-schema.sql"   # opcional (Hibernate también lo crea)

# Terminal 1: inventario-service en WildFly (8082)
# Terminal 2
cd "Sesión 12/pedidos-reportes-service" && mvn spring-boot:run
# Terminal 3
cd "Sesión 5/sistema-pedidos-springboot" && mvn spring-boot:run
```

**Captura E0:** `SHOW CREATE TABLE pedido_revision;` y la columna `revisado` de `pedido` (muestra FK y UNIQUE).

## Caso 1 — Revisión correcta

1. Ingrese con `operador` a `http://localhost:8081/sistema-pedidos/pedidos` y registre un pedido (flujo existente con inventario-service). Anote el ID.
2. **Antes (E1a):** consultas SQL del pedido → `revisado = 0` y `0` filas en `pedido_revision`.
3. Abra **Supervisión y revisión**, escriba una observación (p. ej. "Validado con el cliente") y pulse **Revisar**.
4. **E1b:** la JSP muestra el mensaje verde `[REVISADO]` y el pedido con estado REVISADO, observación y fecha. (DevTools → Network: el POST responde **302** y el GET siguiente **200**; eso demuestra el PRG.)
5. **Después (E1c):** SQL → `revisado = 1` y exactamente **1** fila en `pedido_revision`.

## Caso 2 — Revisión duplicada

1. Con el mismo pedido, intente revisarlo de nuevo. Como la JSP ya no muestra el botón, use DevTools (F12) sobre otro formulario y cambie el `action` a `/sistema-pedidos/supervision/pedidos/<ID>/revisar`, o envíe el formulario de otra pestaña que se abrió antes de revisar.
2. **E2a:** mensaje rojo `[YA_REVISADO_409]` "El pedido #ID ya fue revisado…".
3. **E2b:** SQL → sigue habiendo **1** fila y la observación original no cambió.

## Caso 3 — Validaciones e ID inexistente

Con `operador`, sobre un pedido **pendiente**:

| Prueba | Cómo | Resultado esperado (captura) |
|---|---|---|
| Observación vacía | Deje el campo en blanco y pulse Revisar | `[DATOS_INVALIDOS]` "La observación de la revisión es obligatoria." |
| pedidoId inválido | DevTools: `action` → `.../supervision/pedidos/0/revisar` y luego `.../abc/revisar` | `[DATOS_INVALIDOS]` |
| ID no registrado | DevTools: `action` → `.../supervision/pedidos/999999/revisar` | `[NO_ENCONTRADO]` "No existe el pedido con ID 999999." |

**E3d:** ejecute la consulta de consistencia global (resultado esperado: 0 filas) y el conteo de `pedido_revision` (no aumentó).
Complemento: `SupervisionPedidoControllerTest` y `PedidoRevisionServiceImplTest` prueban los mismos casos automáticamente.

## Caso 4 — Rollback real

```bash
cd "Sesión 5/sistema-pedidos-springboot"
mvn -Dtest=PedidoRevisionServiceIntegrationTest test
```

**E4:** captura del resultado (`Tests run: 5, Failures: 0`). Las pruebas
`rollbackOrderUpdateWhenTraceInsertFailsInsideDeclarativeTransaction` y
`rollbackIsVisibleWithProgrammaticTransactionTemplate` provocan un fallo **en el INSERT** de
`pedido_revision` después del UPDATE (restricción CHECK temporal en H2) y verifican `revisado = false`
y 0 revisiones. Si desea, abra `target/surefire-reports/...PedidoRevisionServiceIntegrationTest.txt`.

## Caso 5 — Servicio disponible / no disponible

Con 8083 **activo**:

```bash
curl -i "http://localhost:8083/pedidos-reportes-service/api/v1/pedidos/revisiones?estado=REVISADO"
curl -i "http://localhost:8083/pedidos-reportes-service/api/v1/pedidos/revisiones?estado=PENDIENTE"
curl -i "http://localhost:8083/pedidos-reportes-service/api/v1/pedidos/revisiones?estado=ANULADO"   # 400
curl -i "http://localhost:8083/pedidos-reportes-service/api/v1/pedidos/estadisticas/revisiones"
```

**E5a:** respuestas con `HTTP/1.1 200` (y `400` para ANULADO) y JSON. **E5b:** la JSP con el resumen (KPIs) y "origen: pedidos-reportes-service".

Detenga `pedidos-reportes-service` (Ctrl + C en la terminal 2):

**E5c:** `curl -i ...` falla con *connection refused* (puerto cerrado).
**E5d:** recargue la JSP → aviso amarillo "El servicio de reportes (puerto 8083) no está disponible…", origen "respaldo local (MySQL)", sin trazas técnicas.
**E5e:** revise otro pedido pendiente con 8083 detenido → `[REVISADO]` (la transacción es local) y la consulta de consistencia global devuelve **0 filas** (sin corrupción).

## Pruebas automatizadas

```bash
cd "Sesión 5/sistema-pedidos-springboot" && mvn clean test
cd "Sesión 12/pedidos-reportes-service" && mvn clean test
```

**E6a / E6b:** captura del bloque final `Tests run: X, Failures: 0, Errors: 0` y `BUILD SUCCESS` de cada proyecto.
**E6c:** (opcional) vista de pruebas del IDE con las clases nuevas en verde.

## Repositorio

```bash
git push -u origin feature/pa4-<apellido>
git log --oneline -1        # hash del commit final para el informe
```

Dé acceso como **editor/colaborador** a `canolem@isil.pe` e `ing.inf.cnolem@gmail.com` antes del vencimiento.
