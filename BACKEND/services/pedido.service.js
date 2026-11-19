const pedidoModel = require("../models/pedido.model");

//Crear un error con codigo HTTP
function crearError(mensaje, estado) {
    const error = new Error(mensaje);
    error.status = estado;
    return error;
}

//Registrar un pedido
async function registrarPedido(atencionId, mozoId, productos) {

    //Validar la atencion
    if (!Number.isInteger(atencionId) || atencionId <= 0) {
        throw crearError("La atencion es invalida", 400);
    }

    //Validar el mozo
    if (!Number.isInteger(mozoId) || mozoId <= 0) {
        throw crearError("El mozo es invalido", 400);
    }

    //Verificar que existan productos
    if (!Array.isArray(productos) || productos.length === 0) {
        throw crearError(
            "El pedido debe contener al menos un producto",
            400
        );
    }

    const productosRegistrados = new Set();

    //Validar cada producto
    for (const producto of productos) {

        if (!producto ||
            !Number.isInteger(producto.productoId) ||
            producto.productoId <= 0) {
            throw crearError("El producto es invalido", 400);
        }

        if (!Number.isInteger(producto.cantidad) ||
            producto.cantidad <= 0) {
            throw crearError(
                "La cantidad debe ser mayor que cero",
                400
            );
        }

        if (productosRegistrados.has(producto.productoId)) {
            throw crearError(
                "No se puede repetir un producto en el pedido",
                400
            );
        }

        productosRegistrados.add(producto.productoId);
    }

    //Registrar el pedido usando el modelo
    let pedido;

    try {
        pedido = await pedidoModel.registrarPedido(
            atencionId,
            mozoId,
            productos
        );
    } catch (error) {
        //Producto inexistente o no disponible
        if (/^El producto \d+ no existe o no esta disponible$/.test(error.message)) {
            throw crearError(error.message, 400);
        }

        throw error;
    }

    //Validar que la atencion permita registrar pedidos
    if (!pedido) {
        throw crearError(
            "La atencion no esta abierta o no corresponde al mozo",
            409
        );
    }
    return pedido;
}

module.exports = {
    registrarPedido
};