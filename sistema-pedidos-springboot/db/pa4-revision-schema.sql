-- =====================================================================
-- PA4 - Módulo de revisión y trazabilidad de pedidos (MySQL 8.x)
-- Base de datos: pedidosdb
--
-- Opción A (recomendada para laboratorio): ejecutar este script ANTES de
--            iniciar sistema-pedidos-springboot.
-- Opción B: dejar que Hibernate (spring.jpa.hibernate.ddl-auto=update) cree
--            la columna y la tabla a partir de Pedido/PedidoRevision. Los nombres
--            de restricciones son los mismos en ambas opciones.
-- Ejecutar una sola vez. Si la columna ya existe, omitir el ALTER TABLE.
-- =====================================================================
USE pedidosdb;

-- 1) Atributo revisado en pedido (boolean, valor inicial false)
ALTER TABLE pedido
    ADD COLUMN revisado BOOLEAN NOT NULL DEFAULT FALSE;

-- 2) Tabla de trazabilidad
CREATE TABLE IF NOT EXISTS pedido_revision (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    pedido_id       BIGINT       NOT NULL,
    observacion     VARCHAR(500) NOT NULL,
    fecha_revision  DATETIME(6)  NOT NULL,
    CONSTRAINT pk_pedido_revision PRIMARY KEY (id),
    -- Integridad referencial: toda revisión pertenece a un pedido existente
    CONSTRAINT fk_pedido_revision_pedido FOREIGN KEY (pedido_id) REFERENCES pedido (id),
    -- Impide más de una revisión confirmada para el mismo pedido
    CONSTRAINT uk_pedido_revision_pedido UNIQUE (pedido_id)
) ENGINE = InnoDB;

-- 3) Permisos (ajustar el host según su instalación, p. ej. 'localhost' o '%')
--    sistema-pedidos escribe; pedidos-reportes-service solo lee.
-- GRANT SELECT, INSERT, UPDATE ON pedidosdb.pedido_revision TO 'spedidos_app'@'localhost';
-- GRANT SELECT ON pedidosdb.pedido_revision TO 'spedidos_reportes'@'localhost';
-- FLUSH PRIVILEGES;

-- Verificación
SHOW CREATE TABLE pedido_revision;
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'pedidosdb' AND TABLE_NAME = 'pedido' AND COLUMN_NAME = 'revisado';
