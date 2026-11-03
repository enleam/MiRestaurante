/*
    PROYECTO: Mi Restaurante (Grupo 3)
    CURSO: Desarrollo de Aplicaciones Moviles
    BASE DE DATOS: MiRestaurante

    IMPORTANTE:
    - Los usuarios son SOLO para pruebas locales, no para produccion.
    - El backend Node.js valida los permisos y cambios de estado.
    - Las operaciones de abrir/cerrar una mesa se haran en transacciones
      desde el backend, para que no queden datos a medias.
    - Las fechas de registro se guardan en UTC. La fecha de reserva
      representa la hora local del restaurante (Peru).
*/

-- Opciones recomendadas por SQL Server para el indice que protege mesas abiertas.
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
GO

/* 1. CREAR LA BASE DE DATOS */
IF DB_ID(N'MiRestaurante') IS NULL
BEGIN
    EXEC(N'CREATE DATABASE MiRestaurante');
END;
GO

USE MiRestaurante;
GO

SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;

    -- Evitar ejecutar este script encima de la version anterior.
    IF EXISTS (
        SELECT 1
        FROM sys.tables
        WHERE name IN (
            N'Rol', N'Usuario', N'Mesa', N'AtencionMesa',
            N'Cliente', N'Reserva', N'Categoria', N'Producto',
            N'Pedido', N'DetallePedido', N'Venta', N'ComprobanteSimple'
        )
    )
    BEGIN
        THROW 50001, N'Ya existen tablas del restaurante. Realiza una copia de seguridad y usa una base de datos vacia.', 1;
    END;

    /* 2. ESQUEMAS: AGRUPAN LAS TABLAS SEGUN SU FUNCION */
    IF SCHEMA_ID(N'seguridad') IS NULL EXEC(N'CREATE SCHEMA seguridad');
    IF SCHEMA_ID(N'salon') IS NULL EXEC(N'CREATE SCHEMA salon');
    IF SCHEMA_ID(N'clientes') IS NULL EXEC(N'CREATE SCHEMA clientes');
    IF SCHEMA_ID(N'catalogo') IS NULL EXEC(N'CREATE SCHEMA catalogo');
    IF SCHEMA_ID(N'pedidos') IS NULL EXEC(N'CREATE SCHEMA pedidos');
    IF SCHEMA_ID(N'ventas') IS NULL EXEC(N'CREATE SCHEMA ventas');
    IF SCHEMA_ID(N'reportes') IS NULL EXEC(N'CREATE SCHEMA reportes');

    /* 3. TABLAS */

    -- ROLES Y USUARIOS
    CREATE TABLE seguridad.Rol (
        rol_id INT IDENTITY(1,1) PRIMARY KEY,
        nombre NVARCHAR(20) NOT NULL UNIQUE,
        CONSTRAINT CK_Rol_Nombre CHECK (nombre IN (N'MOZO', N'COCINA'))
    );

    CREATE TABLE seguridad.Usuario (
        usuario_id INT IDENTITY(1,1) PRIMARY KEY,
        rol_id INT NOT NULL,
        nombres NVARCHAR(100) NOT NULL,
        correo NVARCHAR(150) NOT NULL UNIQUE,
        password_hash VARCHAR(100) NOT NULL,
        estado NVARCHAR(10) NOT NULL DEFAULT N'ACTIVO',
        CONSTRAINT FK_Usuario_Rol FOREIGN KEY (rol_id)
            REFERENCES seguridad.Rol(rol_id),
        CONSTRAINT CK_Usuario_Estado CHECK (estado IN (N'ACTIVO', N'INACTIVO'))
    );

    -- MESAS DEL RESTAURANTE
    CREATE TABLE salon.Mesa (
        mesa_id INT IDENTITY(1,1) PRIMARY KEY,
        numero INT NOT NULL UNIQUE,
        capacidad INT NOT NULL,
        estado NVARCHAR(10) NOT NULL DEFAULT N'LIBRE',
        activa BIT NOT NULL DEFAULT 1,
        CONSTRAINT CK_Mesa_Numero CHECK (numero > 0),
        CONSTRAINT CK_Mesa_Capacidad CHECK (capacidad > 0),
        CONSTRAINT CK_Mesa_Estado CHECK (estado IN (N'LIBRE', N'OCUPADA'))
    );

    -- CLIENTES Y RESERVAS (LAS RESERVAS SOLO SE CONSULTAN DESDE ANDROID)
    CREATE TABLE clientes.Cliente (
        cliente_id INT IDENTITY(1,1) PRIMARY KEY,
        nombre NVARCHAR(100) NOT NULL,
        telefono VARCHAR(20) NOT NULL
    );

    CREATE TABLE clientes.Reserva (
        reserva_id INT IDENTITY(1,1) PRIMARY KEY,
        cliente_id INT NOT NULL,
        mesa_id INT NOT NULL,
        fecha_hora DATETIME2(0) NOT NULL,
        cantidad_personas INT NOT NULL,
        estado NVARCHAR(15) NOT NULL DEFAULT N'REGISTRADA',
        observacion NVARCHAR(200) NULL,
        CONSTRAINT FK_Reserva_Cliente FOREIGN KEY (cliente_id)
            REFERENCES clientes.Cliente(cliente_id),
        CONSTRAINT FK_Reserva_Mesa FOREIGN KEY (mesa_id)
            REFERENCES salon.Mesa(mesa_id),
        CONSTRAINT UQ_Reserva_MesaFecha UNIQUE (mesa_id, fecha_hora),
        CONSTRAINT CK_Reserva_Personas CHECK (cantidad_personas > 0),
        CONSTRAINT CK_Reserva_Estado CHECK (estado IN (N'REGISTRADA', N'CONFIRMADA', N'CANCELADA'))
    );

    -- CARTA: CATEGORIAS Y PRODUCTOS
    CREATE TABLE catalogo.Categoria (
        categoria_id INT IDENTITY(1,1) PRIMARY KEY,
        nombre NVARCHAR(80) NOT NULL UNIQUE,
        activa BIT NOT NULL DEFAULT 1
    );

    CREATE TABLE catalogo.Producto (
        producto_id INT IDENTITY(1,1) PRIMARY KEY,
        categoria_id INT NOT NULL,
        nombre NVARCHAR(120) NOT NULL,
        descripcion NVARCHAR(200) NULL,
        precio DECIMAL(10,2) NOT NULL,
        disponible BIT NOT NULL DEFAULT 1,
        CONSTRAINT FK_Producto_Categoria FOREIGN KEY (categoria_id)
            REFERENCES catalogo.Categoria(categoria_id),
        CONSTRAINT CK_Producto_Precio CHECK (precio > 0)
    );

    -- ATENCION DE UNA MESA: UNA ATENCION PUEDE TENER VARIOS PEDIDOS
    CREATE TABLE salon.AtencionMesa (
        atencion_id INT IDENTITY(1,1) PRIMARY KEY,
        mesa_id INT NOT NULL,
        mozo_id INT NOT NULL,
        fecha_apertura DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
        fecha_cierre DATETIME2(0) NULL,
        estado NVARCHAR(10) NOT NULL DEFAULT N'ABIERTA',
        CONSTRAINT FK_Atencion_Mesa FOREIGN KEY (mesa_id)
            REFERENCES salon.Mesa(mesa_id),
        CONSTRAINT FK_Atencion_Mozo FOREIGN KEY (mozo_id)
            REFERENCES seguridad.Usuario(usuario_id),
        CONSTRAINT CK_Atencion_Estado CHECK (estado IN (N'ABIERTA', N'CERRADA')),
        CONSTRAINT CK_Atencion_Fechas CHECK (
            (estado = N'ABIERTA' AND fecha_cierre IS NULL)
            OR (estado = N'CERRADA' AND fecha_cierre IS NOT NULL)
        )
    );

    -- Evita dos atenciones ABIERTAS para la misma mesa.
    -- Se permiten muchas atenciones CERRADAS como historial.
    CREATE UNIQUE INDEX UX_Atencion_MesaAbierta
        ON salon.AtencionMesa(mesa_id)
        WHERE estado = N'ABIERTA';

    -- PEDIDOS
    CREATE TABLE pedidos.Pedido (
        pedido_id INT IDENTITY(1,1) PRIMARY KEY,
        atencion_id INT NOT NULL,
        registrado_por_id INT NOT NULL,
        fecha_registro DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
        estado NVARCHAR(20) NOT NULL DEFAULT N'REGISTRADO',
        CONSTRAINT FK_Pedido_Atencion FOREIGN KEY (atencion_id)
            REFERENCES salon.AtencionMesa(atencion_id),
        CONSTRAINT FK_Pedido_Usuario FOREIGN KEY (registrado_por_id)
            REFERENCES seguridad.Usuario(usuario_id),
        CONSTRAINT CK_Pedido_Estado CHECK (estado IN (
            N'REGISTRADO', N'ENVIADO', N'EN_PREPARACION',
            N'LISTO', N'ENTREGADO', N'CANCELADO'
        ))
    );

    CREATE TABLE pedidos.DetallePedido (
        detalle_id INT IDENTITY(1,1) PRIMARY KEY,
        pedido_id INT NOT NULL,
        producto_id INT NOT NULL,
        cantidad INT NOT NULL,
        precio_unitario DECIMAL(10,2) NOT NULL,
        observacion NVARCHAR(200) NULL,
        CONSTRAINT FK_Detalle_Pedido FOREIGN KEY (pedido_id)
            REFERENCES pedidos.Pedido(pedido_id),
        CONSTRAINT FK_Detalle_Producto FOREIGN KEY (producto_id)
            REFERENCES catalogo.Producto(producto_id),
        CONSTRAINT UQ_Detalle_PedidoProducto UNIQUE (pedido_id, producto_id),
        CONSTRAINT CK_Detalle_Cantidad CHECK (cantidad > 0),
        CONSTRAINT CK_Detalle_Precio CHECK (precio_unitario >= 0)
    );

    -- VENTA: UNA SOLA VENTA POR ATENCION; CLIENTE OPCIONAL
    CREATE TABLE ventas.Venta (
        venta_id INT IDENTITY(1,1) PRIMARY KEY,
        atencion_id INT NOT NULL UNIQUE,
        cliente_id INT NULL,
        fecha_venta DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
        total DECIMAL(10,2) NOT NULL,
        metodo_pago NVARCHAR(10) NOT NULL,
        CONSTRAINT FK_Venta_Atencion FOREIGN KEY (atencion_id)
            REFERENCES salon.AtencionMesa(atencion_id),
        CONSTRAINT FK_Venta_Cliente FOREIGN KEY (cliente_id)
            REFERENCES clientes.Cliente(cliente_id),
        CONSTRAINT CK_Venta_Total CHECK (total > 0),
        CONSTRAINT CK_Venta_Metodo CHECK (metodo_pago IN (
            N'EFECTIVO', N'TARJETA', N'YAPE', N'PLIN'
        ))
    );

    -- COMPROBANTE INTERNO, NO ES BOLETA NI FACTURA SUNAT.
    -- Su numero se muestra en la app como MR-000001 usando comprobante_id.
    -- El total se obtiene de Venta; no hace falta repetirlo aqui.
    CREATE TABLE ventas.ComprobanteSimple (
        comprobante_id INT IDENTITY(1,1) PRIMARY KEY,
        venta_id INT NOT NULL UNIQUE,
        fecha_emision DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_Comprobante_Venta FOREIGN KEY (venta_id)
            REFERENCES ventas.Venta(venta_id)
    );

    -- El esquema reportes se conserva, pero no tiene vistas:
    -- el panel administrativo web quedo fuera del alcance.

    /* 4. DATOS DE PRUEBA */

    INSERT INTO seguridad.Rol (nombre) VALUES
        (N'MOZO'), (N'COCINA');

    -- Las cuatro contrasenas de PRUEBA son: Restaurante2026!
    -- password_hash contiene un hash bcrypt diferente para cada usuario.
    -- En un sistema real se crean usuarios por un proceso seguro, no con claves conocidas.
    INSERT INTO seguridad.Usuario (rol_id, nombres, correo, password_hash) VALUES
        ((SELECT rol_id FROM seguridad.Rol WHERE nombre = N'MOZO'),
         N'Mozo de prueba 1', N'mozo1@mirestaurante.test', '$2b$10$wNL/vGG8wx9WDrg8gYloZuetNDXoKTEtRPb2WNwtM9CfWkUyWd8wy'),
        ((SELECT rol_id FROM seguridad.Rol WHERE nombre = N'MOZO'),
         N'Mozo de prueba 2', N'mozo2@mirestaurante.test', '$2b$10$CzLQ4vaa7IXphb0LR02Gm.tbsX/GB4ZOIEPHNJ4A97iBtfjJ6YgQe'),
        ((SELECT rol_id FROM seguridad.Rol WHERE nombre = N'COCINA'),
         N'Cocina de prueba 1', N'cocina1@mirestaurante.test', '$2b$10$/o0SQqh8cAlMXOMJFsKfh.IRsBjo4ZkKY3JqmDtDNteMaTWss2QFu'),
        ((SELECT rol_id FROM seguridad.Rol WHERE nombre = N'COCINA'),
         N'Cocina de prueba 2', N'cocina2@mirestaurante.test', '$2b$10$kKI.npuXKCLQNc6lhjoTn.Kf/lqkNmcWA1nqjfkBqGPoBsoEtoPbe');

    INSERT INTO salon.Mesa (numero, capacidad) VALUES
        (1, 2), (2, 4), (3, 4), (4, 6),
        (5, 2), (6, 4), (7, 6), (8, 4);

    INSERT INTO clientes.Cliente (nombre, telefono) VALUES
        (N'Ana Pérez (prueba)', '900000001'),
        (N'Carlos Rojas (prueba)', '900000002'),
        (N'María López (prueba)', '900000003'),
        (N'Luis Gómez (prueba)', '900000004');

    -- Reservas precargadas: horario local de Peru.
    -- Solo se consultaran, no cambian automaticamente el estado de la mesa.
    INSERT INTO clientes.Reserva
        (cliente_id, mesa_id, fecha_hora, cantidad_personas, estado, observacion)
    VALUES
        (1, 1, '2026-11-10T19:00:00', 2, N'CONFIRMADA', N'Mesa cerca de la ventana'),
        (2, 2, '2026-11-11T13:00:00', 3, N'CONFIRMADA', NULL),
        (3, 4, '2026-11-13T20:00:00', 5, N'REGISTRADA', NULL),
        (4, 5, '2026-11-14T14:00:00', 2, N'CONFIRMADA', NULL),
        (1, 7, '2026-11-15T20:00:00', 4, N'REGISTRADA', NULL),
        (2, 6, '2026-12-03T12:00:00', 4, N'CONFIRMADA', NULL);

    INSERT INTO catalogo.Categoria (nombre) VALUES
        (N'Entradas'),
        (N'Platos de fondo'),
        (N'Bebidas'),
        (N'Postres');

    INSERT INTO catalogo.Producto (categoria_id, nombre, descripcion, precio) VALUES
        (1, N'Causa limeña', N'Causa de pollo', 14.00),
        (1, N'Papa a la huancaina', N'Papas con salsa huancaina', 12.00),
        (1, N'Sopa criolla', N'Sopa con carne y fideos', 15.00),
        (2, N'Lomo saltado', N'Carne con arroz y papas fritas', 30.00),
        (2, N'Arroz con pollo', N'Con salsa criolla', 24.00),
        (2, N'Ají de gallina', N'Con arroz y papa', 26.00),
        (2, N'Cuarto de pollo a la brasa', N'Con papas y ensalada', 23.00),
        (2, N'Tallarines verdes', N'Con bistec', 25.00),
        (3, N'Limonada', N'Vaso personal', 9.00),
        (3, N'Chicha morada', N'Vaso personal', 8.00),
        (3, N'Agua mineral', N'Botella personal', 5.00),
        (3, N'Gaseosa', N'Botella personal', 6.00),
        (4, N'Mazamorra morada', N'Porción personal', 10.00),
        (4, N'Arroz con leche', N'Porción personal', 10.00),
        (4, N'Pie de limón', N'Porción personal', 12.00);

    -- Un ejemplo coherente de mesa OCUPADA con su atencion ABIERTA.
    -- El resto de mesas empieza LIBRE para practicar desde la app.
    INSERT INTO salon.AtencionMesa (mesa_id, mozo_id)
    SELECT m.mesa_id, u.usuario_id
    FROM salon.Mesa AS m
    CROSS JOIN seguridad.Usuario AS u
    WHERE m.numero = 8 AND u.correo = N'mozo1@mirestaurante.test';

    UPDATE salon.Mesa SET estado = N'OCUPADA' WHERE numero = 8;

    COMMIT TRANSACTION;

    PRINT N'MiRestaurante creada correctamente con datos de prueba.';

    /* 5. CONSULTAS DE VERIFICACION */
    SELECT TABLE_SCHEMA AS esquema, TABLE_NAME AS tabla
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_TYPE = 'BASE TABLE'
      AND TABLE_SCHEMA IN (N'seguridad', N'salon', N'clientes', N'catalogo', N'pedidos', N'ventas')
    ORDER BY TABLE_SCHEMA, TABLE_NAME;

    SELECT u.usuario_id, u.nombres, u.correo, r.nombre AS rol, u.estado
    FROM seguridad.Usuario AS u
    INNER JOIN seguridad.Rol AS r ON r.rol_id = u.rol_id
    ORDER BY u.usuario_id;

    SELECT mesa_id, numero, capacidad, estado FROM salon.Mesa ORDER BY numero;
    SELECT producto_id, nombre, precio FROM catalogo.Producto ORDER BY producto_id;
    SELECT reserva_id, mesa_id, fecha_hora, estado FROM clientes.Reserva ORDER BY fecha_hora;

END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO