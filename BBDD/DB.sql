/*
    12 tablas: Rol, Usuario, Mesa, Cliente, Reserva, Categoria, Producto,
    AtencionMesa, Pedido, DetallePedido, Venta y ComprobanteSimple.

    SEGURIDAD:
    - El backend genera password_hash mediante bcrypt; nunca guardar contraseñas en texto plano.
    - Android y React solo consultan esta base a través de la API REST.
    - No contiene usuarios ficticios ni contraseñas precargadas.

    FECHAS:
    - Fechas operativas / auditoría: UTC (SYSUTCDATETIME()).
    - Reserva.fecha_hora: hora local del restaurante (Perú). La API debe interpretarla
      explícitamente en la zona horaria America/Lima y validar los conflictos de horarios.

    ESQUEMAS SQL SERVER:
    - seguridad: Rol, Usuario.
    - salon: Mesa, AtencionMesa.
    - clientes: Cliente, Reserva.
    - catalogo: Categoria, Producto.
    - pedidos: Pedido, DetallePedido.
    - ventas: Venta, ComprobanteSimple.
    - reportes: vistas de ventas y productos más vendidos.
*/
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
GO

IF DB_ID(N'MiRestaurante') IS NULL
BEGIN
    EXEC (N'CREATE DATABASE [MiRestaurante]');
END;
GO

USE [MiRestaurante];
GO

/* =============================================================
   0. ESQUEMAS SQL SERVER Y MIGRACIÓN SEGURA DESDE dbo (V1.0)
   ============================================================= */
IF SCHEMA_ID(N'seguridad') IS NULL EXEC(N'CREATE SCHEMA seguridad AUTHORIZATION dbo');
IF SCHEMA_ID(N'salon') IS NULL EXEC(N'CREATE SCHEMA salon AUTHORIZATION dbo');
IF SCHEMA_ID(N'clientes') IS NULL EXEC(N'CREATE SCHEMA clientes AUTHORIZATION dbo');
IF SCHEMA_ID(N'catalogo') IS NULL EXEC(N'CREATE SCHEMA catalogo AUTHORIZATION dbo');
IF SCHEMA_ID(N'pedidos') IS NULL EXEC(N'CREATE SCHEMA pedidos AUTHORIZATION dbo');
IF SCHEMA_ID(N'ventas') IS NULL EXEC(N'CREATE SCHEMA ventas AUTHORIZATION dbo');
IF SCHEMA_ID(N'reportes') IS NULL EXEC(N'CREATE SCHEMA reportes AUTHORIZATION dbo');
GO

-- Si ya existe la versión inicial bajo dbo, se migra en su lugar.
-- Evita duplicar tablas o perder información de pedidos y ventas.
DECLARE @TotalAntiguas INT, @TotalNuevas INT;
SELECT @TotalAntiguas = COUNT(*)
FROM sys.tables
WHERE schema_id = SCHEMA_ID(N'dbo')
  AND name IN (N'Rol', N'Usuario', N'Mesa', N'AtencionMesa', N'Cliente', N'Reserva',
               N'Categoria', N'Producto', N'Pedido', N'DetallePedido', N'Venta', N'ComprobanteSimple');
SELECT @TotalNuevas = COUNT(*)
FROM sys.tables
WHERE schema_id IN (SCHEMA_ID(N'seguridad'), SCHEMA_ID(N'salon'),
                    SCHEMA_ID(N'clientes'), SCHEMA_ID(N'catalogo'),
                    SCHEMA_ID(N'pedidos'), SCHEMA_ID(N'ventas'))
  AND name IN (N'Rol', N'Usuario', N'Mesa', N'AtencionMesa', N'Cliente', N'Reserva',
               N'Categoria', N'Producto', N'Pedido', N'DetallePedido', N'Venta', N'ComprobanteSimple');

IF @TotalAntiguas > 0 AND (@TotalAntiguas <> 12 OR @TotalNuevas > 0)
BEGIN
    THROW 51201, N'Se detecto una estructura mixta o incompleta en dbo. Revisar la base antes de migrar para evitar duplicados.', 1;
END;

IF @TotalAntiguas = 12
BEGIN
    SET XACT_ABORT ON;
    BEGIN TRY
        BEGIN TRANSACTION;
        -- Las dependencias antiguas de programabilidad se vuelven a crear más abajo.
        DROP TRIGGER IF EXISTS dbo.TR_Pedido_Transiciones;
        DROP TRIGGER IF EXISTS dbo.TR_DetallePedido_SoloAntesDePreparar;
        DROP TRIGGER IF EXISTS dbo.TR_Mesa_NoLiberarConAtencionAbierta;
        DROP TRIGGER IF EXISTS dbo.TR_Venta_ValidarTotalYEntrega;
        DROP TRIGGER IF EXISTS dbo.TR_Comprobante_Total;
        DROP TRIGGER IF EXISTS dbo.TR_AtencionMesa_Cierre;

        DROP PROCEDURE IF EXISTS dbo.sp_CrearReserva;
        DROP PROCEDURE IF EXISTS dbo.sp_AbrirAtencionMesa;
        DROP PROCEDURE IF EXISTS dbo.sp_CrearPedido;
        DROP PROCEDURE IF EXISTS dbo.sp_AgregarDetallePedido;
        DROP PROCEDURE IF EXISTS dbo.sp_CambiarEstadoPedido;
        DROP PROCEDURE IF EXISTS dbo.sp_CerrarAtencionMesa;

        DROP VIEW IF EXISTS dbo.vw_VentasDiarias;
        DROP VIEW IF EXISTS dbo.vw_ProductosMasVendidos;

        -- ALTER SCHEMA TRANSFER conserva tablas, identidades, datos e índices.
        -- Las FK ya existentes mantienen los vínculos por object_id.
        ALTER SCHEMA seguridad TRANSFER dbo.Rol;
        ALTER SCHEMA seguridad TRANSFER dbo.Usuario;
        ALTER SCHEMA salon TRANSFER dbo.Mesa;
        ALTER SCHEMA salon TRANSFER dbo.AtencionMesa;
        ALTER SCHEMA clientes TRANSFER dbo.Cliente;
        ALTER SCHEMA clientes TRANSFER dbo.Reserva;
        ALTER SCHEMA catalogo TRANSFER dbo.Categoria;
        ALTER SCHEMA catalogo TRANSFER dbo.Producto;
        ALTER SCHEMA pedidos TRANSFER dbo.Pedido;
        ALTER SCHEMA pedidos TRANSFER dbo.DetallePedido;
        ALTER SCHEMA ventas TRANSFER dbo.Venta;
        ALTER SCHEMA ventas TRANSFER dbo.ComprobanteSimple;
        COMMIT;
        PRINT N'Migración desde dbo finalizada sin borrar registros.';
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK;
        THROW;
    END CATCH;
END;
GO

/* =============================================================
   1. SEGURIDAD
   ============================================================= */
IF OBJECT_ID(N'seguridad.Rol', N'U') IS NULL
BEGIN
    CREATE TABLE seguridad.Rol (
        rol_id          INT IDENTITY(1,1) NOT NULL,
        nombre          NVARCHAR(30) NOT NULL,
        CONSTRAINT PK_Rol PRIMARY KEY (rol_id),
        CONSTRAINT UQ_Rol_Nombre UNIQUE (nombre),
        CONSTRAINT CK_Rol_Nombre CHECK (nombre IN (N'ADMINISTRADOR', N'MOZO', N'COCINA'))
    );
END;
GO

IF OBJECT_ID(N'seguridad.Usuario', N'U') IS NULL
BEGIN
    CREATE TABLE seguridad.Usuario (
        usuario_id          INT IDENTITY(1,1) NOT NULL,
        rol_id              INT NOT NULL,
        nombres             NVARCHAR(120) NOT NULL,
        correo              NVARCHAR(254) COLLATE Latin1_General_100_CI_AI NOT NULL,
        password_hash       NVARCHAR(255) NOT NULL,
        estado              NVARCHAR(10) NOT NULL CONSTRAINT DF_Usuario_Estado DEFAULT (N'ACTIVO'),
        fecha_creacion      DATETIME2(0) NOT NULL CONSTRAINT DF_Usuario_Creado DEFAULT (SYSUTCDATETIME()),
        fecha_actualizacion DATETIME2(0) NULL,
        CONSTRAINT PK_Usuario PRIMARY KEY (usuario_id),
        CONSTRAINT UQ_Usuario_Correo UNIQUE (correo),
        CONSTRAINT FK_Usuario_Rol FOREIGN KEY (rol_id) REFERENCES seguridad.Rol(rol_id),
        CONSTRAINT CK_Usuario_Estado CHECK (estado IN (N'ACTIVO', N'INACTIVO')),
        CONSTRAINT CK_Usuario_Nombres CHECK (LEN(LTRIM(RTRIM(nombres))) > 0),
        CONSTRAINT CK_Usuario_Correo CHECK (LEN(LTRIM(RTRIM(correo))) > 3 AND correo LIKE N'%_@_%._%'),
        CONSTRAINT CK_Usuario_Password CHECK (LEN(LTRIM(RTRIM(password_hash))) >= 40)
    );
END;
GO

/* =============================================================
   2. MESAS Y CLIENTES
   ============================================================= */
IF OBJECT_ID(N'salon.Mesa', N'U') IS NULL
BEGIN
    CREATE TABLE salon.Mesa (
        mesa_id          INT IDENTITY(1,1) NOT NULL,
        numero           INT NOT NULL,
        capacidad        INT NOT NULL,
        estado           NVARCHAR(12) NOT NULL CONSTRAINT DF_Mesa_Estado DEFAULT (N'LIBRE'),
        activo           BIT NOT NULL CONSTRAINT DF_Mesa_Activo DEFAULT (1),
        CONSTRAINT PK_Mesa PRIMARY KEY (mesa_id),
        CONSTRAINT UQ_Mesa_Numero UNIQUE (numero),
        CONSTRAINT CK_Mesa_Numero CHECK (numero > 0),
        CONSTRAINT CK_Mesa_Capacidad CHECK (capacidad > 0),
        CONSTRAINT CK_Mesa_Estado CHECK (estado IN (N'LIBRE', N'OCUPADA', N'RESERVADA'))
    );
END;
GO

IF OBJECT_ID(N'clientes.Cliente', N'U') IS NULL
BEGIN
    CREATE TABLE clientes.Cliente (
        cliente_id     INT IDENTITY(1,1) NOT NULL,
        nombre         NVARCHAR(150) NOT NULL,
        telefono       NVARCHAR(25) NOT NULL,
        fecha_creacion DATETIME2(0) NOT NULL CONSTRAINT DF_Cliente_Creado DEFAULT (SYSUTCDATETIME()),
        CONSTRAINT PK_Cliente PRIMARY KEY (cliente_id),
        CONSTRAINT CK_Cliente_Nombre CHECK (LEN(LTRIM(RTRIM(nombre))) > 0),
        CONSTRAINT CK_Cliente_Telefono CHECK (LEN(LTRIM(RTRIM(telefono))) BETWEEN 6 AND 25)
    );
END;
GO

IF OBJECT_ID(N'clientes.Reserva', N'U') IS NULL
BEGIN
    CREATE TABLE clientes.Reserva (
        reserva_id        INT IDENTITY(1,1) NOT NULL,
        mesa_id           INT NOT NULL,
        cliente_id        INT NOT NULL,
        fecha_hora        DATETIME2(0) NOT NULL,
        cantidad_personas INT NOT NULL,
        estado            NVARCHAR(15) NOT NULL CONSTRAINT DF_Reserva_Estado DEFAULT (N'REGISTRADA'),
        observacion       NVARCHAR(500) NULL,
        fecha_registro    DATETIME2(0) NOT NULL CONSTRAINT DF_Reserva_Creado DEFAULT (SYSUTCDATETIME()),
        CONSTRAINT PK_Reserva PRIMARY KEY (reserva_id),
        CONSTRAINT FK_Reserva_Mesa FOREIGN KEY (mesa_id) REFERENCES salon.Mesa(mesa_id),
        CONSTRAINT FK_Reserva_Cliente FOREIGN KEY (cliente_id) REFERENCES clientes.Cliente(cliente_id),
        CONSTRAINT UQ_Reserva_IdMesa UNIQUE (reserva_id, mesa_id),
        CONSTRAINT CK_Reserva_Personas CHECK (cantidad_personas > 0),
        CONSTRAINT CK_Reserva_Estado CHECK (estado IN
            (N'REGISTRADA', N'CONFIRMADA', N'ATENDIDA', N'CANCELADA', N'NO_ASISTIO'))
    );
END;
GO

/* =============================================================
   3. CARTA DEL RESTAURANTE
   ============================================================= */
IF OBJECT_ID(N'catalogo.Categoria', N'U') IS NULL
BEGIN
    CREATE TABLE catalogo.Categoria (
        categoria_id INT IDENTITY(1,1) NOT NULL,
        nombre       NVARCHAR(100) NOT NULL,
        activa       BIT NOT NULL CONSTRAINT DF_Categoria_Activa DEFAULT (1),
        CONSTRAINT PK_Categoria PRIMARY KEY (categoria_id),
        CONSTRAINT UQ_Categoria_Nombre UNIQUE (nombre),
        CONSTRAINT CK_Categoria_Nombre CHECK (LEN(LTRIM(RTRIM(nombre))) > 0)
    );
END;
GO

IF OBJECT_ID(N'catalogo.Producto', N'U') IS NULL
BEGIN
    CREATE TABLE catalogo.Producto (
        producto_id       INT IDENTITY(1,1) NOT NULL,
        categoria_id      INT NOT NULL,
        nombre            NVARCHAR(150) NOT NULL,
        descripcion       NVARCHAR(500) NULL,
        precio            DECIMAL(12,2) NOT NULL,
        disponible        BIT NOT NULL CONSTRAINT DF_Producto_Disponible DEFAULT (1),
        activo            BIT NOT NULL CONSTRAINT DF_Producto_Activo DEFAULT (1),
        CONSTRAINT PK_Producto PRIMARY KEY (producto_id),
        CONSTRAINT FK_Producto_Categoria FOREIGN KEY (categoria_id)
            REFERENCES catalogo.Categoria(categoria_id),
        CONSTRAINT UQ_Producto_CategoriaNombre UNIQUE (categoria_id, nombre),
        CONSTRAINT CK_Producto_Nombre CHECK (LEN(LTRIM(RTRIM(nombre))) > 0),
        CONSTRAINT CK_Producto_Precio CHECK (precio >= 0)
    );
END;
GO

/* =============================================================
   4. ATENCIÓN Y PEDIDOS
   ============================================================= */
IF OBJECT_ID(N'salon.AtencionMesa', N'U') IS NULL
BEGIN
    CREATE TABLE salon.AtencionMesa (
        atencion_id     INT IDENTITY(1,1) NOT NULL,
        mesa_id         INT NOT NULL,
        reserva_id      INT NULL,
        mozo_id         INT NOT NULL,
        fecha_apertura  DATETIME2(0) NOT NULL CONSTRAINT DF_Atencion_Apertura DEFAULT (SYSUTCDATETIME()),
        fecha_cierre    DATETIME2(0) NULL,
        estado          NVARCHAR(12) NOT NULL CONSTRAINT DF_Atencion_Estado DEFAULT (N'ABIERTA'),
        CONSTRAINT PK_AtencionMesa PRIMARY KEY (atencion_id),
        CONSTRAINT FK_Atencion_Mesa FOREIGN KEY (mesa_id) REFERENCES salon.Mesa(mesa_id),
        CONSTRAINT FK_Atencion_ReservaMesa FOREIGN KEY (reserva_id, mesa_id)
            REFERENCES clientes.Reserva(reserva_id, mesa_id),
        CONSTRAINT FK_Atencion_Mozo FOREIGN KEY (mozo_id) REFERENCES seguridad.Usuario(usuario_id),
        CONSTRAINT CK_Atencion_Estado CHECK (estado IN (N'ABIERTA', N'CERRADA', N'CANCELADA')),
        CONSTRAINT CK_Atencion_Fechas CHECK (
            (estado = N'ABIERTA' AND fecha_cierre IS NULL)
            OR (estado IN (N'CERRADA', N'CANCELADA') AND fecha_cierre IS NOT NULL AND fecha_cierre >= fecha_apertura)
        )
    );
END;
GO

IF OBJECT_ID(N'pedidos.Pedido', N'U') IS NULL
BEGIN
    CREATE TABLE pedidos.Pedido (
        pedido_id         INT IDENTITY(1,1) NOT NULL,
        atencion_id       INT NOT NULL,
        registrado_por_id INT NOT NULL,
        fecha_registro    DATETIME2(0) NOT NULL CONSTRAINT DF_Pedido_Registro DEFAULT (SYSUTCDATETIME()),
        fecha_envio       DATETIME2(0) NULL,
        fecha_entrega     DATETIME2(0) NULL,
        estado            NVARCHAR(20) NOT NULL CONSTRAINT DF_Pedido_Estado DEFAULT (N'REGISTRADO'),
        CONSTRAINT PK_Pedido PRIMARY KEY (pedido_id),
        CONSTRAINT FK_Pedido_Atencion FOREIGN KEY (atencion_id) REFERENCES salon.AtencionMesa(atencion_id),
        CONSTRAINT FK_Pedido_Usuario FOREIGN KEY (registrado_por_id) REFERENCES seguridad.Usuario(usuario_id),
        CONSTRAINT CK_Pedido_Estado CHECK (estado IN (
            N'REGISTRADO', N'ENVIADO', N'EN_PREPARACION', N'LISTO',
            N'ENTREGADO', N'CERRADO', N'CANCELADO'))
    );
END;
GO

IF OBJECT_ID(N'pedidos.DetallePedido', N'U') IS NULL
BEGIN
    CREATE TABLE pedidos.DetallePedido (
        detalle_id       INT IDENTITY(1,1) NOT NULL,
        pedido_id        INT NOT NULL,
        producto_id      INT NOT NULL,
        cantidad         INT NOT NULL,
        precio_unitario  DECIMAL(12,2) NOT NULL,
        observacion      NVARCHAR(500) NULL,
        subtotal         AS (CONVERT(DECIMAL(18,2), cantidad * precio_unitario)) PERSISTED,
        CONSTRAINT PK_DetallePedido PRIMARY KEY (detalle_id),
        CONSTRAINT FK_Detalle_Pedido FOREIGN KEY (pedido_id) REFERENCES pedidos.Pedido(pedido_id),
        CONSTRAINT FK_Detalle_Producto FOREIGN KEY (producto_id) REFERENCES catalogo.Producto(producto_id),
        CONSTRAINT CK_Detalle_Cantidad CHECK (cantidad > 0),
        CONSTRAINT CK_Detalle_Precio CHECK (precio_unitario >= 0)
    );
END;
GO

/* =============================================================
   5. VENTAS Y COMPROBANTES SIMPLES (NO TRIBUTARIOS)
   ============================================================= */
IF OBJECT_ID(N'ventas.Venta', N'U') IS NULL
BEGIN
    CREATE TABLE ventas.Venta (
        venta_id     INT IDENTITY(1,1) NOT NULL,
        atencion_id  INT NOT NULL,
        cliente_id   INT NULL,  -- venta anónima permitida
        fecha_venta  DATETIME2(0) NOT NULL CONSTRAINT DF_Venta_Fecha DEFAULT (SYSUTCDATETIME()),
        total        DECIMAL(18,2) NOT NULL,
        metodo_pago  NVARCHAR(10) NOT NULL,
        CONSTRAINT PK_Venta PRIMARY KEY (venta_id),
        CONSTRAINT UQ_Venta_Atencion UNIQUE (atencion_id), -- máximo una venta por atención
        CONSTRAINT FK_Venta_Atencion FOREIGN KEY (atencion_id) REFERENCES salon.AtencionMesa(atencion_id),
        CONSTRAINT FK_Venta_Cliente FOREIGN KEY (cliente_id) REFERENCES clientes.Cliente(cliente_id),
        CONSTRAINT CK_Venta_Total CHECK (total >= 0),
        CONSTRAINT CK_Venta_Metodo CHECK (metodo_pago IN
            (N'EFECTIVO', N'TARJETA', N'YAPE', N'PLIN'))
    );
END;
GO

IF OBJECT_ID(N'ventas.ComprobanteSimple', N'U') IS NULL
BEGIN
    CREATE TABLE ventas.ComprobanteSimple (
        comprobante_id INT IDENTITY(1,1) NOT NULL,
        venta_id       INT NOT NULL,
        -- Numeración interna automática y determinística. NO es factura/boleta SUNAT.
        numero         AS (N'MR-' + RIGHT(N'0000000000' + CONVERT(NVARCHAR(10), comprobante_id), 10)) PERSISTED,
        fecha_emision  DATETIME2(0) NOT NULL CONSTRAINT DF_Comprobante_Fecha DEFAULT (SYSUTCDATETIME()),
        total          DECIMAL(18,2) NOT NULL,
        CONSTRAINT PK_Comprobante PRIMARY KEY (comprobante_id),
        CONSTRAINT UQ_Comprobante_Venta UNIQUE (venta_id),
        CONSTRAINT FK_Comprobante_Venta FOREIGN KEY (venta_id) REFERENCES ventas.Venta(venta_id),
        CONSTRAINT CK_Comprobante_Total CHECK (total >= 0)
    );
END;
GO

/* =============================================================
   6. ÍNDICES (CONSULTAS MÓVILES, RESERVAS Y REPORTES)
   ============================================================= */
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'salon.AtencionMesa') AND name = N'UX_AtencionMesa_MesaAbierta')
    CREATE UNIQUE INDEX UX_AtencionMesa_MesaAbierta ON salon.AtencionMesa(mesa_id) WHERE estado = N'ABIERTA';
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'salon.AtencionMesa') AND name = N'UX_AtencionMesa_Reserva')
    CREATE UNIQUE INDEX UX_AtencionMesa_Reserva ON salon.AtencionMesa(reserva_id) WHERE reserva_id IS NOT NULL;
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'clientes.Reserva') AND name = N'IX_Reserva_MesaFechaEstado')
    CREATE INDEX IX_Reserva_MesaFechaEstado ON clientes.Reserva(mesa_id, fecha_hora, estado);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'clientes.Reserva') AND name = N'UX_Reserva_MesaHoraActiva')
    CREATE UNIQUE INDEX UX_Reserva_MesaHoraActiva ON clientes.Reserva(mesa_id, fecha_hora)
    WHERE estado IN (N'REGISTRADA', N'CONFIRMADA');
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'pedidos.Pedido') AND name = N'IX_Pedido_AtencionEstado')
    CREATE INDEX IX_Pedido_AtencionEstado ON pedidos.Pedido(atencion_id, estado);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'pedidos.DetallePedido') AND name = N'IX_Detalle_Pedido')
    CREATE INDEX IX_Detalle_Pedido ON pedidos.DetallePedido(pedido_id) INCLUDE (producto_id, cantidad, subtotal);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'catalogo.Producto') AND name = N'IX_Producto_CategoriaDisponibilidad')
    CREATE INDEX IX_Producto_CategoriaDisponibilidad ON catalogo.Producto(categoria_id, activo, disponible);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'ventas.Venta') AND name = N'IX_Venta_Fecha')
    CREATE INDEX IX_Venta_Fecha ON ventas.Venta(fecha_venta) INCLUDE (total, metodo_pago);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'ventas.ComprobanteSimple') AND name = N'UX_Comprobante_Numero')
    CREATE UNIQUE INDEX UX_Comprobante_Numero ON ventas.ComprobanteSimple(numero);
GO

/* =============================================================
   7. PROTECCIONES DE INTEGRIDAD
   Las transiciones se ejecutan desde la API/procedimientos. Estos triggers
   impiden modificaciones inconsistentes aunque alguien use SQL directo.
   ============================================================= */
CREATE OR ALTER TRIGGER pedidos.TR_Pedido_Transiciones
ON pedidos.Pedido
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF NOT UPDATE(estado) RETURN;

    IF EXISTS (
        SELECT 1
        FROM inserted i
        INNER JOIN deleted d ON d.pedido_id = i.pedido_id
        WHERE i.estado <> d.estado AND NOT (
            (d.estado = N'REGISTRADO' AND i.estado IN (N'ENVIADO', N'CANCELADO')) OR
            (d.estado = N'ENVIADO' AND i.estado IN (N'EN_PREPARACION', N'CANCELADO')) OR
            (d.estado = N'EN_PREPARACION' AND i.estado = N'LISTO') OR
            (d.estado = N'LISTO' AND i.estado = N'ENTREGADO') OR
            (d.estado = N'ENTREGADO' AND i.estado = N'CERRADO')
        )
    )
        THROW 51001, N'Transicion de estado del pedido no permitida.', 1;

    IF EXISTS (
        SELECT 1 FROM inserted i INNER JOIN deleted d ON d.pedido_id = i.pedido_id
        INNER JOIN salon.AtencionMesa a ON a.atencion_id = i.atencion_id
        WHERE i.estado <> d.estado AND a.estado <> N'ABIERTA'
    )
        THROW 51009, N'No se puede cambiar el estado de pedidos de una atencion finalizada.', 1;

    IF EXISTS (
        SELECT 1 FROM inserted i INNER JOIN deleted d ON d.pedido_id = i.pedido_id
        WHERE i.estado = N'ENVIADO' AND d.estado <> N'ENVIADO'
          AND NOT EXISTS (SELECT 1 FROM pedidos.DetallePedido dp WHERE dp.pedido_id = i.pedido_id)
    )
        THROW 51010, N'No se puede enviar un pedido sin productos.', 1;

    -- Solo se puede cerrar un pedido después de registrar la venta de su atención.
    IF EXISTS (
        SELECT 1 FROM inserted i INNER JOIN deleted d ON d.pedido_id = i.pedido_id
        WHERE i.estado = N'CERRADO' AND d.estado <> N'CERRADO'
          AND NOT EXISTS (SELECT 1 FROM ventas.Venta v WHERE v.atencion_id = i.atencion_id)
    )
        THROW 51002, N'No se puede cerrar un pedido sin venta registrada.', 1;
END;
GO

CREATE OR ALTER TRIGGER pedidos.TR_DetallePedido_SoloAntesDePreparar
ON pedidos.DetallePedido
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (
        SELECT 1
        FROM (
            SELECT pedido_id FROM inserted
            UNION
            SELECT pedido_id FROM deleted
        ) a
        INNER JOIN pedidos.Pedido p ON p.pedido_id = a.pedido_id
        INNER JOIN salon.AtencionMesa am ON am.atencion_id = p.atencion_id
        WHERE p.estado NOT IN (N'REGISTRADO', N'ENVIADO') OR am.estado <> N'ABIERTA'
    )
        THROW 51003, N'El detalle solo puede cambiarse antes de iniciar preparacion y con la atencion abierta.', 1;
END;
GO

CREATE OR ALTER TRIGGER salon.TR_Mesa_NoLiberarConAtencionAbierta
ON salon.Mesa
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF NOT UPDATE(estado) RETURN;
    IF EXISTS (
        SELECT 1 FROM inserted i
        WHERE i.estado <> N'OCUPADA'
          AND EXISTS (SELECT 1 FROM salon.AtencionMesa a
                      WHERE a.mesa_id = i.mesa_id AND a.estado = N'ABIERTA')
    )
        THROW 51004, N'No se puede liberar o reservar una mesa que tiene una atencion abierta.', 1;
END;
GO

CREATE OR ALTER TRIGGER ventas.TR_Venta_ValidarTotalYEntrega
ON ventas.Venta
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (
        SELECT 1
        FROM inserted v
        JOIN salon.AtencionMesa a ON a.atencion_id = v.atencion_id
        WHERE a.estado <> N'ABIERTA'
           OR NOT EXISTS (SELECT 1 FROM pedidos.Pedido p
                          WHERE p.atencion_id = a.atencion_id AND p.estado = N'ENTREGADO')
           OR EXISTS (SELECT 1 FROM pedidos.Pedido p
                      WHERE p.atencion_id = a.atencion_id
                        AND p.estado NOT IN (N'ENTREGADO', N'CANCELADO'))
           OR v.total <> ISNULL((
                    SELECT SUM(d.subtotal)
                    FROM pedidos.Pedido p
                    INNER JOIN pedidos.DetallePedido d ON d.pedido_id = p.pedido_id
                    WHERE p.atencion_id = v.atencion_id AND p.estado = N'ENTREGADO'
                ), CONVERT(DECIMAL(18,2), 0))
    )
        THROW 51005, N'La venta requiere pedidos entregados y un total que coincida con el detalle.', 1;
END;
GO

CREATE OR ALTER TRIGGER ventas.TR_Comprobante_Total
ON ventas.ComprobanteSimple
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (
        SELECT 1 FROM inserted c
        INNER JOIN ventas.Venta v ON v.venta_id = c.venta_id
        WHERE c.total <> v.total
    )
        THROW 51006, N'El total del comprobante debe coincidir con el de la venta.', 1;
END;
GO

CREATE OR ALTER TRIGGER salon.TR_AtencionMesa_Cierre
ON salon.AtencionMesa
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (
        SELECT 1 FROM inserted i JOIN deleted d ON d.atencion_id = i.atencion_id
        WHERE d.estado IN (N'CERRADA', N'CANCELADA') AND i.estado <> d.estado
    )
        THROW 51007, N'Una atencion finalizada no puede reabrirse.', 1;

    IF EXISTS (
        SELECT 1
        FROM inserted i JOIN deleted d ON d.atencion_id = i.atencion_id
        WHERE i.estado = N'CERRADA' AND d.estado <> N'CERRADA'
          AND (
                NOT EXISTS (SELECT 1 FROM ventas.Venta v WHERE v.atencion_id = i.atencion_id)
             OR NOT EXISTS (SELECT 1 FROM ventas.Venta v
                            INNER JOIN ventas.ComprobanteSimple c ON c.venta_id = v.venta_id
                            WHERE v.atencion_id = i.atencion_id)
             OR NOT EXISTS (SELECT 1 FROM pedidos.Pedido p
                            WHERE p.atencion_id = i.atencion_id AND p.estado = N'CERRADO')
             OR EXISTS (SELECT 1 FROM pedidos.Pedido p
                        WHERE p.atencion_id = i.atencion_id
                          AND p.estado NOT IN (N'CERRADO', N'CANCELADO'))
          )
    )
        THROW 51008, N'Para cerrar la atencion se requiere una venta, comprobante y todos los pedidos entregados.', 1;
END;
GO

/* =============================================================
   8. PROCEDIMIENTOS PARA OPERACIONES CRÍTICAS
   La API REST debe llamar a estos procedimientos usando consultas parametrizadas.
   ============================================================= */

-- Reserva: nombre/teléfono se registran en Cliente antes de llamar al procedimiento.
-- Previene reserva simultánea de la misma mesa a la misma hora exacta;
-- el criterio de solapamiento entre horarios lo definirá y validará la API.
CREATE OR ALTER PROCEDURE clientes.sp_CrearReserva
    @MesaId INT,
    @ClienteId INT,
    @FechaHora DATETIME2(0),
    @CantidadPersonas INT,
    @ReservaId INT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;
    BEGIN TRY
        BEGIN TRAN;
        DECLARE @Capacidad INT;
        SELECT @Capacidad = capacidad
        FROM salon.Mesa WITH (UPDLOCK, HOLDLOCK)
        WHERE mesa_id = @MesaId AND activo = 1;

        IF @Capacidad IS NULL
            THROW 51101, N'Mesa inexistente o desactivada.', 1;
        IF @CantidadPersonas IS NULL OR @CantidadPersonas < 1 OR @CantidadPersonas > @Capacidad
            THROW 51102, N'Cantidad de personas fuera de la capacidad de la mesa.', 1;
        IF NOT EXISTS (SELECT 1 FROM clientes.Cliente WHERE cliente_id = @ClienteId)
            THROW 51103, N'Cliente inexistente.', 1;
        IF EXISTS (
            SELECT 1 FROM clientes.Reserva WITH (UPDLOCK, HOLDLOCK)
            WHERE mesa_id = @MesaId AND fecha_hora = @FechaHora
              AND estado IN (N'REGISTRADA', N'CONFIRMADA')
        )
            THROW 51104, N'Ya hay una reserva activa para esa mesa a esa hora.', 1;

        INSERT INTO clientes.Reserva (mesa_id, cliente_id, fecha_hora, cantidad_personas)
        VALUES (@MesaId, @ClienteId, @FechaHora, @CantidadPersonas);
        SET @ReservaId = CONVERT(INT, SCOPE_IDENTITY());
        COMMIT;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK;
        THROW;
    END CATCH;
END;
GO

CREATE OR ALTER PROCEDURE salon.sp_AbrirAtencionMesa
    @MesaId INT,
    @MozoId INT,
    @AtencionId INT OUTPUT,
    @ReservaId INT = NULL
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;
    BEGIN TRY
        BEGIN TRAN;
        DECLARE @EstadoMesa NVARCHAR(12);
        SELECT @EstadoMesa = estado
        FROM salon.Mesa WITH (UPDLOCK, HOLDLOCK)
        WHERE mesa_id = @MesaId AND activo = 1;

        IF @EstadoMesa IS NULL
            THROW 51111, N'Mesa inexistente o desactivada.', 1;
        IF @EstadoMesa = N'OCUPADA'
            THROW 51112, N'La mesa ya se encuentra ocupada.', 1;
        IF @ReservaId IS NULL AND @EstadoMesa <> N'LIBRE'
            THROW 51113, N'La mesa reservada requiere asociar su reserva.', 1;
        IF NOT EXISTS (
            SELECT 1 FROM seguridad.Usuario u JOIN seguridad.Rol r ON r.rol_id = u.rol_id
            WHERE u.usuario_id = @MozoId AND u.estado = N'ACTIVO' AND r.nombre = N'MOZO'
        )
            THROW 51114, N'El usuario asignado debe ser un mozo activo.', 1;
        IF @ReservaId IS NOT NULL AND NOT EXISTS (
            SELECT 1 FROM clientes.Reserva
            WHERE reserva_id = @ReservaId AND mesa_id = @MesaId
              AND estado IN (N'REGISTRADA', N'CONFIRMADA')
        )
            THROW 51115, N'La reserva no corresponde a esa mesa o ya no esta vigente.', 1;

        INSERT INTO salon.AtencionMesa(mesa_id, reserva_id, mozo_id)
        VALUES (@MesaId, @ReservaId, @MozoId);
        SET @AtencionId = CONVERT(INT, SCOPE_IDENTITY());

        IF @ReservaId IS NOT NULL
            UPDATE clientes.Reserva SET estado = N'ATENDIDA' WHERE reserva_id = @ReservaId;

        UPDATE salon.Mesa SET estado = N'OCUPADA' WHERE mesa_id = @MesaId;
        COMMIT;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK;
        THROW;
    END CATCH;
END;
GO

CREATE OR ALTER PROCEDURE pedidos.sp_CrearPedido
    @AtencionId INT,
    @MozoId INT,
    @PedidoId INT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;
    BEGIN TRY
        BEGIN TRAN;
        IF NOT EXISTS (
            SELECT 1 FROM salon.AtencionMesa a WITH (UPDLOCK, HOLDLOCK)
            JOIN seguridad.Usuario u ON u.usuario_id = a.mozo_id
            JOIN seguridad.Rol r ON r.rol_id = u.rol_id
            WHERE a.atencion_id = @AtencionId AND a.estado = N'ABIERTA'
              AND a.mozo_id = @MozoId AND u.estado = N'ACTIVO' AND r.nombre = N'MOZO'
        )
            THROW 51121, N'La atencion no esta abierta o el mozo no esta autorizado.', 1;

        INSERT INTO pedidos.Pedido(atencion_id, registrado_por_id)
        VALUES (@AtencionId, @MozoId);
        SET @PedidoId = CONVERT(INT, SCOPE_IDENTITY());
        COMMIT;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK;
        THROW;
    END CATCH;
END;
GO

-- El precio se COPIA de Producto al detalle; cambios futuros de carta
-- no alteran el importe de pedidos y ventas históricos.
CREATE OR ALTER PROCEDURE pedidos.sp_AgregarDetallePedido
    @PedidoId INT,
    @ProductoId INT,
    @Cantidad INT,
    @DetalleId INT OUTPUT,
    @Observacion NVARCHAR(500) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;
    BEGIN TRY
        BEGIN TRAN;
        DECLARE @Precio DECIMAL(12,2);
        IF @Cantidad IS NULL OR @Cantidad <= 0
            THROW 51131, N'La cantidad del producto debe ser mayor que cero.', 1;
        IF NOT EXISTS (
            SELECT 1 FROM pedidos.Pedido p WITH (UPDLOCK, HOLDLOCK)
            JOIN salon.AtencionMesa a ON a.atencion_id = p.atencion_id
            WHERE p.pedido_id = @PedidoId
              AND p.estado IN (N'REGISTRADO', N'ENVIADO') AND a.estado = N'ABIERTA'
        )
            THROW 51132, N'No se pueden modificar productos después de iniciar preparación.', 1;
        SELECT @Precio = p.precio FROM catalogo.Producto p
        JOIN catalogo.Categoria c ON c.categoria_id = p.categoria_id
        WHERE p.producto_id = @ProductoId AND p.activo = 1 AND p.disponible = 1 AND c.activa = 1;
        IF @Precio IS NULL
            THROW 51133, N'Producto inexistente o no disponible.', 1;

        INSERT INTO pedidos.DetallePedido(pedido_id, producto_id, cantidad, precio_unitario, observacion)
        VALUES (@PedidoId, @ProductoId, @Cantidad, @Precio, @Observacion);
        SET @DetalleId = CONVERT(INT, SCOPE_IDENTITY());
        COMMIT;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK;
        THROW;
    END CATCH;
END;
GO

-- Transiciones válidas: REGISTRADO -> ENVIADO -> EN_PREPARACION -> LISTO
-- -> ENTREGADO. Se permite cancelar desde REGISTRADO o ENVIADO.
-- CERRADO solo se establece a través de sp_CerrarAtencionMesa.
CREATE OR ALTER PROCEDURE pedidos.sp_CambiarEstadoPedido
    @PedidoId INT,
    @NuevoEstado NVARCHAR(20),
    @UsuarioId INT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;
    BEGIN TRY
        BEGIN TRAN;
        DECLARE @Rol NVARCHAR(30), @EstadoAnterior NVARCHAR(20), @AtencionEstado NVARCHAR(12);
        SELECT @Rol = r.nombre
        FROM seguridad.Usuario u JOIN seguridad.Rol r ON r.rol_id = u.rol_id
        WHERE u.usuario_id = @UsuarioId AND u.estado = N'ACTIVO';

        SELECT @EstadoAnterior = p.estado, @AtencionEstado = a.estado
        FROM pedidos.Pedido p WITH (UPDLOCK, HOLDLOCK)
        JOIN salon.AtencionMesa a ON a.atencion_id = p.atencion_id
        WHERE p.pedido_id = @PedidoId;

        IF @EstadoAnterior IS NULL OR @AtencionEstado <> N'ABIERTA'
            THROW 51141, N'El pedido no existe o la atencion no esta abierta.', 1;
        IF NOT (
            (@NuevoEstado IN (N'ENVIADO', N'ENTREGADO', N'CANCELADO') AND @Rol = N'MOZO')
            OR (@NuevoEstado IN (N'EN_PREPARACION', N'LISTO') AND @Rol = N'COCINA')
        )
            THROW 51142, N'El usuario no tiene permiso para efectuar esta transicion.', 1;
        IF @NuevoEstado = N'ENVIADO'
           AND NOT EXISTS (SELECT 1 FROM pedidos.DetallePedido WHERE pedido_id = @PedidoId)
            THROW 51143, N'El pedido debe contener al menos un producto antes del envio.', 1;

        -- El trigger TR_Pedido_Transiciones valida origen -> destino.
        UPDATE pedidos.Pedido
        SET estado = @NuevoEstado,
            fecha_envio = CASE WHEN @NuevoEstado = N'ENVIADO' THEN SYSUTCDATETIME() ELSE fecha_envio END,
            fecha_entrega = CASE WHEN @NuevoEstado = N'ENTREGADO' THEN SYSUTCDATETIME() ELSE fecha_entrega END
        WHERE pedido_id = @PedidoId;
        COMMIT;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK;
        THROW;
    END CATCH;
END;
GO

-- Operación atómica: validación de pedidos -> venta -> comprobante ->
-- cierre de pedidos/atención -> liberación de mesa.
CREATE OR ALTER PROCEDURE ventas.sp_CerrarAtencionMesa
    @AtencionId INT,
    @MetodoPago NVARCHAR(10),
    @VentaId INT OUTPUT,
    @ClienteId INT = NULL
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;
    BEGIN TRY
        BEGIN TRAN;
        DECLARE @MesaId INT, @Estado NVARCHAR(12), @Total DECIMAL(18,2);

        SELECT @MesaId = a.mesa_id, @Estado = a.estado
        FROM salon.AtencionMesa a WITH (UPDLOCK, HOLDLOCK)
        WHERE a.atencion_id = @AtencionId;

        IF @Estado IS NULL OR @Estado <> N'ABIERTA'
            THROW 51151, N'La atencion no existe o ya se cerro.', 1;
        IF @MetodoPago IS NULL OR @MetodoPago NOT IN
            (N'EFECTIVO', N'TARJETA', N'YAPE', N'PLIN')
            THROW 51152, N'Metodo de pago no permitido.', 1;
        IF @ClienteId IS NOT NULL AND NOT EXISTS
            (SELECT 1 FROM clientes.Cliente WHERE cliente_id = @ClienteId)
            THROW 51153, N'El cliente indicado no existe.', 1;
        IF NOT EXISTS (
            SELECT 1 FROM pedidos.Pedido WITH (UPDLOCK, HOLDLOCK)
            WHERE atencion_id = @AtencionId AND estado = N'ENTREGADO'
        )
            THROW 51154, N'Se requiere al menos un pedido entregado para cerrar.', 1;
        IF EXISTS (
            SELECT 1 FROM pedidos.Pedido WITH (UPDLOCK, HOLDLOCK)
            WHERE atencion_id = @AtencionId AND estado NOT IN (N'ENTREGADO', N'CANCELADO')
        )
            THROW 51155, N'Hay pedidos pendientes de entrega; cierre rechazado.', 1;
        IF EXISTS (
            SELECT 1 FROM pedidos.Pedido p
            WHERE p.atencion_id = @AtencionId AND p.estado = N'ENTREGADO'
              AND NOT EXISTS (SELECT 1 FROM pedidos.DetallePedido d WHERE d.pedido_id = p.pedido_id)
        )
            THROW 51156, N'No se puede vender un pedido sin detalle.', 1;

        SELECT @Total = SUM(d.subtotal)
        FROM pedidos.Pedido p
        INNER JOIN pedidos.DetallePedido d ON d.pedido_id = p.pedido_id
        WHERE p.atencion_id = @AtencionId AND p.estado = N'ENTREGADO';
        IF @Total IS NULL
            THROW 51157, N'No fue posible calcular el importe de la venta.', 1;

        INSERT INTO ventas.Venta(atencion_id, cliente_id, total, metodo_pago)
        VALUES (@AtencionId, @ClienteId, @Total, @MetodoPago);
        SET @VentaId = CONVERT(INT, SCOPE_IDENTITY());

        INSERT INTO ventas.ComprobanteSimple(venta_id, total) VALUES (@VentaId, @Total);
        UPDATE pedidos.Pedido SET estado = N'CERRADO'
        WHERE atencion_id = @AtencionId AND estado = N'ENTREGADO';
        UPDATE salon.AtencionMesa
        SET estado = N'CERRADA', fecha_cierre = SYSUTCDATETIME()
        WHERE atencion_id = @AtencionId;
        UPDATE salon.Mesa SET estado = N'LIBRE' WHERE mesa_id = @MesaId;
        COMMIT;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK;
        THROW;
    END CATCH;
END;
GO

/* =============================================================
   9. VISTAS PARA EL PANEL ADMINISTRATIVO WEB
   ============================================================= */
CREATE OR ALTER VIEW reportes.vw_VentasDiarias
AS
    SELECT CAST(v.fecha_venta AS DATE) AS fecha_utc,
           COUNT_BIG(*) AS cantidad_ventas,
           SUM(v.total) AS total_ventas
    FROM ventas.Venta v
    GROUP BY CAST(v.fecha_venta AS DATE);
GO

CREATE OR ALTER VIEW reportes.vw_ProductosMasVendidos
AS
    SELECT d.producto_id,
           pr.nombre AS producto,
           SUM(CONVERT(BIGINT, d.cantidad)) AS unidades_vendidas,
           SUM(d.subtotal) AS importe_vendido
    FROM pedidos.DetallePedido d
    JOIN pedidos.Pedido p ON p.pedido_id = d.pedido_id
    JOIN ventas.Venta v ON v.atencion_id = p.atencion_id
    JOIN catalogo.Producto pr ON pr.producto_id = d.producto_id
    WHERE p.estado = N'CERRADO'
    GROUP BY d.producto_id, pr.nombre;
GO

/* =============================================================
   10. CATÁLOGO DE ROLES Y VERIFICACIÓN
   ============================================================= */
INSERT INTO seguridad.Rol(nombre)
SELECT s.nombre
FROM (VALUES (N'ADMINISTRADOR'), (N'MOZO'), (N'COCINA')) s(nombre)
WHERE NOT EXISTS (SELECT 1 FROM seguridad.Rol r WHERE r.nombre = s.nombre);
GO

PRINT N'Base de datos MiRestaurante: estructura instalada y roles inicializados.';
SELECT TABLE_SCHEMA AS esquema, TABLE_NAME AS tabla
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA IN ('seguridad', 'salon', 'clientes', 'catalogo', 'pedidos', 'ventas')
  AND TABLE_TYPE = 'BASE TABLE'
ORDER BY TABLE_SCHEMA, TABLE_NAME;
SELECT rol_id, nombre FROM seguridad.Rol ORDER BY rol_id;
GO