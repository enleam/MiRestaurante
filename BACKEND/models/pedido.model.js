const sql = require("mssql");

//Registrar un pedido con sus productos
async function registrarPedido(atencionId, mozoId, productos) {
    //Validar los datos recibidos
    if (!Number.isInteger(atencionId) || atencionId <= 0) {
        throw new Error("La atencion es invalida");
    }

    if (!Number.isInteger(mozoId) || mozoId <= 0) {
        throw new Error("El mozo es invalido");
    }

    if (!Array.isArray(productos) || productos.length === 0) {
        throw new Error("El pedido debe tener al menos un producto");
    }

    const productosRepetidos = new Set();

    for (const producto of productos) {
        if (!producto ||
            !Number.isInteger(producto.productoId) ||
            producto.productoId <= 0 ||
            !Number.isInteger(producto.cantidad) ||
            producto.cantidad <= 0) {
            throw new Error("Producto o cantidad invalida");
        }

        if (productosRepetidos.has(producto.productoId)) {
            throw new Error("No se puede repetir un producto en el pedido");
        }

        productosRepetidos.add(producto.productoId);
    }

    const transaccion = new sql.Transaction();
    let transaccionActiva = false;

    try {
        await transaccion.begin();
        transaccionActiva = true;

        //Verificar que la atencion este abierta
        //y corresponda al mozo
        const atencion = await new sql.Request(transaccion)
            .input("atencionId", sql.Int, atencionId)
            .input("mozoId", sql.Int, mozoId)
            .query(`
                SELECT a.atencion_id
                FROM salon.AtencionMesa a WITH (UPDLOCK, HOLDLOCK)
                INNER JOIN seguridad.Usuario u
                    ON u.usuario_id = a.mozo_id
                INNER JOIN seguridad.Rol r
                    ON r.rol_id = u.rol_id
                WHERE a.atencion_id = @atencionId
                  AND a.mozo_id = @mozoId
                  AND a.estado = N'ABIERTA'
                  AND u.estado = N'ACTIVO'
                  AND r.nombre = N'MOZO'
            `);

        if (atencion.recordset.length === 0) {
            await transaccion.rollback();
            transaccionActiva = false;
            return null;
        }

        //Crear el pedido
        const resultado = await new sql.Request(transaccion)
            .input("atencionId", sql.Int, atencionId)
            .input("mozoId", sql.Int, mozoId)
            .query(`
                INSERT INTO pedidos.Pedido (
                    atencion_id,
                    registrado_por_id
                )
                OUTPUT
                    INSERTED.pedido_id AS pedidoId,
                    INSERTED.atencion_id AS atencionId,
                    INSERTED.estado,
                    INSERTED.fecha_registro AS fechaRegistro
                VALUES (
                    @atencionId,
                    @mozoId
                )
            `);

        const pedido = resultado.recordset[0];
        const detalles = [];
        let totalCentavos = 0;

        //Registrar los productos del pedido
        for (const item of productos) {

            //Consultar el precio real del producto
            const consulta = await new sql.Request(transaccion)
                .input("productoId", sql.Int, item.productoId)
                .query(`
                    SELECT
                        p.producto_id AS productoId,
                        p.nombre,
                        p.precio
                    FROM catalogo.Producto p WITH (HOLDLOCK)
                    INNER JOIN catalogo.Categoria c
                        ON c.categoria_id = p.categoria_id
                    WHERE p.producto_id = @productoId
                      AND p.disponible = 1
                      AND c.activa = 1
                `);

            if (consulta.recordset.length === 0) {
                throw new Error(
                    "El producto " + item.productoId +
                    " no existe o no esta disponible"
                );
            }

            const producto = consulta.recordset[0];
            const precio = Number(producto.precio);

            //Guardar el detalle del pedido
            const detalle = await new sql.Request(transaccion)
                .input("pedidoId", sql.Int, pedido.pedidoId)
                .input("productoId", sql.Int, item.productoId)
                .input("cantidad", sql.Int, item.cantidad)
                .input("precioUnitario", sql.Decimal(10, 2), precio)
                .query(`
                    INSERT INTO pedidos.DetallePedido (
                        pedido_id,
                        producto_id,
                        cantidad,
                        precio_unitario
                    )
                    OUTPUT
                        INSERTED.detalle_id AS detalleId
                    VALUES (
                        @pedidoId,
                        @productoId,
                        @cantidad,
                        @precioUnitario
                    )
                `);

            //Calcular el subtotal del producto
            const subtotalCentavos =
                Math.round(precio * 100) * item.cantidad;

            totalCentavos += subtotalCentavos;

            detalles.push({
                detalleId: detalle.recordset[0].detalleId,
                productoId: producto.productoId,
                nombre: producto.nombre,
                cantidad: item.cantidad,
                precioUnitario: precio,
                subtotal: subtotalCentavos / 100
            });
        }

        //Confirmar el pedido y sus productos
        await transaccion.commit();
        transaccionActiva = false;

        return {
            pedidoId: pedido.pedidoId,
            atencionId: pedido.atencionId,
            estado: pedido.estado,
            fechaRegistro: pedido.fechaRegistro,
            productos: detalles,
            total: totalCentavos / 100
        };

    } catch (error) {
        if (transaccionActiva) {
            try {
                await transaccion.rollback();
            } catch (rollbackError) {
                console.error("Error al revertir:", rollbackError);
            }
        }

        throw error;
    }
}

module.exports = {
    registrarPedido
};