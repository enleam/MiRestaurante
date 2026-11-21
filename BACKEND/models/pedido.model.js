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

async function modificarPedido(pedidoId, mozoId, productos) {
    if(!Number.isInteger(pedidoId) || pedidoId <= 0 ||
        !Number.isInteger(mozoId) || mozoId <= 0) {
        const error = new Error("Identificadores invalidos");
        error.status = 400;
        throw error;
        }


if(!Array.isArray(productos) || productos.length ===0){
    const error= new Error(
        "El pedido debe contener al menos un producto"
    );
    error.status = 400;
    throw error;
}

const ids = new Set();

for (const producto of productos){
    if(!producto ||
        !Number.isInteger(producto.productoId) ||
        producto.productoId <= 0 ||
        !Number.isInteger(producto.cantidad) ||
        producto.cantidad <= 0 ||
        ids.has(producto.productoId)) {

            const error = new Error(
                "Productos o cantidades invalidas o repetidas"
            );
            error.status =400;
            throw error;
        }
        ids.add(producto.productoId);
}

const transaccion = new sql.Transaction();
let transaccionActiva = false;

try {
    await transaccion.begin();
    transaccionActiva = true;

    const consultaPedido = await new sql.Request(transaccion)
    .input("pedidoId", sql.Int, pedidoId)
    .input("mozoId", sql.Int, mozoId)
    .query(
        `
                SELECT
                    p.pedido_id AS pedidoId,
                    p.atencion_id AS atencionId,
                    p.estado,
                    a.estado AS estadoAtencion
                FROM pedidos.Pedido p WITH (UPDLOCK, HOLDLOCK)
                INNER JOIN salon.AtencionMesa a
                    ON a.atencion_id = p.atencion_id
                WHERE p.pedido_id = @pedidoId
                  AND p.registrado_por_id = @mozoId
                  AND a.mozo_id = @mozoId
            `
    );

    if (consultaPedido.recordset.length === 0){
        const error = new Error(
            "El pedido no existe o no pertenece al mozo"
        );
        error.status = 404;
        throw error;
    }

    const pedido = consultaPedido.recordset[0];

    if(pedido.estadoAtencion !== "ABIERTA"){
        const error = new Error(
            "La atencion ya no esta abierta"
        );
        error.status = 409;
        throw error;
    }

    if(
        pedido .estado !== "REGISTRADO" &&
        pedido .estado !== "ENVIADO"
    ) {
        const error = new Error(
            "El pedido ya no permite modificaciones"
        );
        error.status = 409;
        throw error;
    }

    const consultaDetalles = await new  sql.Request(transaccion)
    .input("pedidoId", sql.Int, pedidoId)
    .query(
            `
                SELECT
                    detalle_id AS detalleId,
                    producto_id AS productoId,
                    precio_unitario AS precioUnitario
                FROM pedidos.DetallePedido WITH (UPDLOCK, HOLDLOCK)
                WHERE pedido_id = @pedidoId
            `
    );
    const actuales = consultaDetalles.recordset;
    const productosActuales = new Map(
        actuales.map(item => [item.productoId, item])
    );

    const preciosNuevos  = new Map();

    for (const item  of productos){
        if (!productosActuales.has(item.productoId)){
            const consulta = await new sql.Request(transaccion)
            .input("productoId", sql.Int, item.productoId)
            .query(
                `
                        SELECT p.precio
                        FROM catalogo.Producto p
                        INNER JOIN catalogo.Categoria c
                            ON c.categoria_id = p.categoria_id
                        WHERE p.producto_id = @productoId
                          AND p.disponible = 1
                          AND c.activa = 1
                    `);
                    if (consulta.recordset.length === 0) {
                        const error = new Error(
                            "El producto" + item.productoId + " no existe o no esta disponible"
                        );
                        error.status = 400;
                        throw error;
                    }

                    preciosNuevos.set(
                        item.productoId,
                        Number(consulta.recordset[0].precio)
                    );
        }
    }

    for(const actual of actuales) {
        if (!ids.has(actual.productoId)){
            await new sql.Request(transaccion)
            .input("detalleId", sql.Int, actual.detalleId)
            .query(
                `
                        DELETE FROM pedidos.DetallePedido
                        WHERE detalle_id = @detalleId
                    `
            );
        }
    }

    for (const item of productos){
        const existente = productosActuales.get(item.productoId);
        if(existente){
            await new sql.Request(transaccion)
            .input("detalleId", sql.Int, existente.detalleId)
            .input("cantidad", sql.Int, item.cantidad)
            .query(
                `
                        UPDATE pedidos.DetallePedido
                        SET cantidad = @cantidad
                        WHERE detalle_id = @detalleId
                    `
            );
        } else {
            await new sql.Request(transaccion)
            .input("pedidoId", sql.Int, pedidoId)
            .input("productoId", sql.Int, item.productoId)
            .input("cantidad", sql.Int, item.cantidad)
            .input(
                "precioUnitario",
                sql.Decimal(10,2),
                preciosNuevos.get(item.productoId)
            )
            .query(`
                        INSERT INTO pedidos.DetallePedido (
                            pedido_id,
                            producto_id,
                            cantidad,
                            precio_unitario
                        )
                        VALUES (
                            @pedidoId,
                            @productoId,
                            @cantidad,
                            @precioUnitario
                        )
                    `);
        }
    }

    const resultado = await new sql.Request(transaccion)
    .input("pedidoId", sql.Int, pedidoId)
    .query(
        `
                SELECT
                    d.detalle_id AS detalleId,
                    d.producto_id AS productoId,
                    pr.nombre,
                    d.cantidad,
                    d.precio_unitario AS precioUnitario,
                    CAST(
                        d.cantidad * d.precio_unitario
                        AS DECIMAL(18, 2)
                    ) AS subtotal
                FROM pedidos.DetallePedido d
                INNER JOIN catalogo.Producto pr
                    ON pr.producto_id = d.producto_id
                WHERE d.pedido_id = @pedidoId
                ORDER BY d.detalle_id
            `
    );

    const detalles = resultado.recordset.map(item => ({
        detalleId: item.detalleId,
        productoId: item.productoId,
        nombre: item.nombre,
        cantidad: item.cantidad,
        precioUnitario: Number(item.precioUnitario),
        subtotal: Number(item.subtotal)
    }));

    const totalCentavos = detalles.reduce(
        (suma, item)=> 
            suma + Math.round(item.subtotal * 100),
        0
    );

    await transaccion.commit();
    transaccionActiva = false;

    return{
        pedidoId: pedido.pedidoId,
        atencionId: pedido.atencionId,
        estado: pedido.estado,
        productos: detalles,
        total: totalCentavos / 100
    };
} catch (error) {

    if (transaccionActiva){
        try{
            await transaccion.rollback();
        } catch (rollbackError){
            console.error(
                "Error al revertir modificación:",
                rollbackError
            );
        }
    }
    throw error;
}
}


async function listarPedidosPorAtencion(atencionId, mozoId) {

    if (!Number.isInteger(atencionId) || atencionId <= 0 ||
        !Number.isInteger(mozoId) || mozoId <= 0) {

        const error = new Error("Identificadores invalidos");
        error.status = 400;
        throw error;
    }

    const consultaAtencion = await new sql.Request()
        .input("atencionId", sql.Int, atencionId)
        .input("mozoId", sql.Int, mozoId)
        .query(`
            SELECT
                atencion_id,
                estado
            FROM salon.AtencionMesa
            WHERE atencion_id = @atencionId
              AND mozo_id = @mozoId
        `);

    if (consultaAtencion.recordset.length === 0) {
        const error = new Error(
            "La atencion no existe o no pertenece al mozo"
        );
        error.status = 404;
        throw error;
    }

    if (consultaAtencion.recordset[0].estado !== "ABIERTA") {
        const error = new Error(
            "La atencion ya no esta abierta"
        );
        error.status = 409;
        throw error;
    }

    const resultado = await new sql.Request()
        .input("atencionId", sql.Int, atencionId)
        .input("mozoId", sql.Int, mozoId)
        .query(`
            SELECT
                p.pedido_id AS pedidoId,
                p.atencion_id AS atencionId,
                p.estado,
                p.fecha_registro AS fechaRegistro,
                d.detalle_id AS detalleId,
                d.producto_id AS productoId,
                pr.nombre,
                d.cantidad,
                d.precio_unitario AS precioUnitario
            FROM pedidos.Pedido p
            LEFT JOIN pedidos.DetallePedido d
                ON d.pedido_id = p.pedido_id
            LEFT JOIN catalogo.Producto pr
                ON pr.producto_id = d.producto_id
            WHERE p.atencion_id = @atencionId
              AND p.registrado_por_id = @mozoId
            ORDER BY p.pedido_id DESC, d.detalle_id
        `);

    const pedidosMap = new Map();

    for (const fila of resultado.recordset) {

        if (!pedidosMap.has(fila.pedidoId)) {
            pedidosMap.set(fila.pedidoId, {
                pedidoId: fila.pedidoId,
                atencionId: fila.atencionId,
                estado: fila.estado,
                fechaRegistro: fila.fechaRegistro,
                modificable:
                    fila.estado === "REGISTRADO" ||
                    fila.estado === "ENVIADO",
                productos: [],
                total: 0
            });
        }

        const pedido = pedidosMap.get(fila.pedidoId);

        if (fila.detalleId !== null) {

            const precio = Number(fila.precioUnitario);

            const subtotalCentavos =
                Math.round(precio * 100) * fila.cantidad;

            pedido.productos.push({
                detalleId: fila.detalleId,
                productoId: fila.productoId,
                nombre: fila.nombre,
                cantidad: fila.cantidad,
                precioUnitario: precio,
                subtotal: subtotalCentavos / 100
            });

            pedido.total = Math.round(
                pedido.total * 100 + subtotalCentavos
            ) / 100;
        }
    }

    return Array.from(pedidosMap.values());
}
async function enviarPedidoACocina(pedidoId, mozoId) {

    //Validar identificadores
    if(
        !Number.isInteger(pedidoId) || pedidoId <= 0 ||
        !Number.isInteger(mozoId) || mozoId <= 0
    ){
        const error = new Error("Identificadores invalidos");
        error.status = 400
        throw error;
    }
    
    const transaccion = new sql.Transaction();
    let transaccionActiva = false;

    try{
        await transaccion.begin();
        transaccionActiva=true;

        //Verificar que el pedido pertenezca al mozo
        //y que la atencion este abierta

        const consultaPedido = await new sql.Request(transaccion)
            .input("pedidoId", sql.Int, pedidoId)
            .input("mozoId", sql.Int, mozoId)
            .query(`
                SELECT
                    p.pedido_id AS pedidoId,
                    p.atencion_id AS atencionId,
                    p.estado,
                    p.fecha_registro AS fechaRegistro,
                    a.estado AS estadoAtencion
                FROM pedidos.Pedido p WITH (UPDLOCK, HOLDLOCK)
                INNER JOIN salon.AtencionMesa a WITH (UPDLOCK, HOLDLOCK)
                    ON a.atencion_id = p.atencion_id
                INNER JOIN seguridad.Usuario u
                    ON u.usuario_id = p.registrado_por_id
                INNER JOIN seguridad.Rol r
                    ON r.rol_id = u.rol_id
                WHERE p.pedido_id = @pedidoId
                    AND p.registrado_por_id = @mozoId
                    AND a.mozo_id = @mozoId
                    AND u.estado = N'ACTIVO'
                    AND r.nombre = N'MOZO'
                `);
            if(consultaPedido.recordset.length === 0){
                const error = new Error(
                    "El pedido no existe o no pertenece al mozo"
                );
                error.status = 404;
                throw error;
            }

            const pedido=consultaPedido.recordset[0];

            //Verificar que la atencion siga abierta
            if(pedido.estadoAtencion !== "ABIERTA"){
                const error = new Error(
                    "La atencion ya no esta abierta"
                );
                error.status = 409;
                throw error;
            }

            //Solo se pueden enviar pedidos registrados
            if(pedido.estado !== "REGISTRADO"){
                const error = new Error(
                    "El pedido ya fue enviado o no permite envios"
                );
                error.status = 409;
                throw error;
            }

            //Cambiar el estado del pedido a ENVIADO
            const resultado = await new sql.Request(transaccion)
                .input("pedidoId", sql.Int, pedidoId)
                .query(`
                    UPDATE pedidos.Pedido
                    SET estado = N'ENVIADO'
                    OUTPUT
                        INSERTED.pedido_id AS pedidoId,
                        INSERTED.atencion_id AS atencionId,
                        INSERTED.estado,
                        INSERTED.fecha_registro AS fechaRegistro
                    WHERE pedido_id = @pedidoId
                        AND estado = N'REGISTRADO'
                    `);

                if(resultado.recordset.length === 0){
                    const error = new Error(
                       "No se pudo enviar el pedido a cocina"
                    );
                    error.status = 409;
                    throw error;
                }
                //Confirmar la transaccion
                await transaccion.commit();
                transaccionActiva = false;

                return resultado.recordset[0];
    }catch (error){
        if(transaccionActiva){
            try {
                await transaccion.rollback();
            }catch (rollbackError){
                console.error(
                    "Error al revertir envio del pedido:",
                    rollbackError
                );
            }
        }
        throw error;
    }
}
module.exports = {
    registrarPedido,
    modificarPedido,
    listarPedidosPorAtencion,
    enviarPedidoACocina
};