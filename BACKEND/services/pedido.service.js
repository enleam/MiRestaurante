
const pedidoModel = require("../models/pedido.model");


function crearError(mensaje, estado) {
    const error = new Error(mensaje);
    error.status = estado;
    return error;
}


async function registrarPedido(atencionId, mozoId, productos) {


    if (!Number.isInteger(atencionId) || atencionId <= 0) {
        throw crearError("La atencion es invalida", 400);
    }

    if (!Number.isInteger(mozoId) || mozoId <= 0) {
        throw crearError("El mozo es invalido", 400);
    }


    if (!Array.isArray(productos) || productos.length === 0) {
        throw crearError(
            "El pedido debe contener al menos un producto",
            400
        );
    }

    const productosRegistrados = new Set();


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

   
    let pedido;

    try {
        pedido = await pedidoModel.registrarPedido(
            atencionId,
            mozoId,
            productos
        );
    } catch (error) {

        if (/^El producto \d+ no existe o no esta disponible$/.test(error.message)) {
            throw crearError(error.message, 400);
        }

        throw error;
    }


    if (!pedido) {
        throw crearError(
            "La atencion no esta abierta o no corresponde al mozo",
            409
        );
    }

    return pedido;
}


async function modificarPedido(pedidoId, mozoId, productos) {


    const idPedido = Number(pedidoId);
    const idMozo = Number(mozoId);

    if (!Number.isSafeInteger(idPedido) || idPedido <= 0) {
        throw crearError("El ID del pedido es invalido", 400);
    }

    if (!Number.isSafeInteger(idMozo) || idMozo <= 0) {
        throw crearError("El ID del mozo es invalido", 400);
    }

    if (!Array.isArray(productos) || productos.length === 0) {
        throw crearError(
            "El pedido debe contener al menos un producto",
            400
        );
    }

    const productosRegistrados = new Set();

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

    const pedido = await pedidoModel.modificarPedido(
        idPedido,
        idMozo,
        productos
    );

    return pedido;
}

async function listarPedidosPorAtencion(atencionId, mozoId) {
    const idAtencion = Number(atencionId);
    const idMozo = Number(mozoId);

    if(!Number.isSafeInteger(idAtencion)|| idAtencion <= 0){
        throw crearError("El ID de la atencion es invalido", 400);
    }

    if(!Number.isSafeInteger(idMozo)|| idMozo <= 0){
        throw crearError("El ID del mozo es invalido", 400);
    }

    const pedidos = await pedidoModel.listarPedidosPorAtencion(
        idAtencion,
        idMozo
    );

    return pedidos;
    
}
module.exports = {
    registrarPedido,
    modificarPedido,
    listarPedidosPorAtencion
};
