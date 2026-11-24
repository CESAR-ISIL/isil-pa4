-- =====================================================================
-- PA4 - Consultas para capturar evidencia ANTES / DESPUÉS de cada caso
-- Reemplace :id por el ID del pedido usado en la prueba.
-- =====================================================================
USE pedidosdb;

-- Estado del pedido
SELECT id, cliente, producto_nombre, total, revisado FROM pedido WHERE id = :id;

-- Revisiones del pedido (debe ser 0 antes y exactamente 1 después del caso 1)
SELECT id, pedido_id, observacion, fecha_revision FROM pedido_revision WHERE pedido_id = :id;
SELECT COUNT(*) AS revisiones_del_pedido FROM pedido_revision WHERE pedido_id = :id;

-- Consistencia global: no debe haber pedidos revisados sin traza ni trazas de pedidos no revisados
SELECT p.id, p.revisado, COUNT(r.id) AS revisiones
FROM pedido p LEFT JOIN pedido_revision r ON r.pedido_id = p.id
GROUP BY p.id, p.revisado
HAVING (p.revisado = TRUE AND COUNT(r.id) <> 1) OR (p.revisado = FALSE AND COUNT(r.id) <> 0);
-- (resultado esperado: 0 filas)

-- Totales (comparar con GET /api/v1/pedidos/estadisticas/revisiones)
SELECT COUNT(*) AS total_pedidos,
       SUM(CASE WHEN revisado = TRUE THEN 1 ELSE 0 END) AS total_revisados
FROM pedido;
