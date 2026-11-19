const atencionModel = require("../models/atencion.model");

//Iniciar la atencion de una mesa
async function iniciarAtencion(mesaId, mozoId) {

    //Convertir los identificadores a numeros
    const idMesa = Number(mesaId);
    const idMozo = Number(mozoId);

    //Validar el ID de la mesa
    if (!Number.isSafeInteger(idMesa) || idMesa <= 0) {
        const error = new Error("El ID de la mesa no es válido");
        error.status = 400;
        throw error;
    }

    //Validar el ID del mozo autenticado
    if (!Number.isSafeInteger(idMozo) || idMozo <= 0) {
        const error = new Error("El ID del mozo no es válido");
        error.status = 400;
        throw error;
    }

    //Solicitar al modelo que inicie la atencion
    const atencion = await atencionModel.iniciarAtencion(
        idMesa,
        idMozo
    );

    //Comprobar si la mesa estaba disponible
    if (!atencion) {
        const error = new Error(
            "No se puede iniciar la atención. La mesa no está disponible"
        );
        error.status = 409;
        throw error;
    }

    return atencion;
}

//HU07: Consultar la atencion abierta de una mesa
async function obtenerAtencionAbierta(mesaId, mozoId) {

    //Convertir los identificadores a numeros
    const idMesa = Number(mesaId);
    const idMozo = Number(mozoId);

    //Validar el ID de la mesa
    if (!Number.isSafeInteger(idMesa) || idMesa <= 0) {
        const error = new Error("El ID de la mesa no es válido");
        error.status = 400;
        throw error;
    }

    //Validar el ID del mozo autenticado
    if (!Number.isSafeInteger(idMozo) || idMozo <= 0) {
        const error = new Error("El ID del mozo no es válido");
        error.status = 400;
        throw error;
    }

    //Consultar la atencion en la base de datos
    const atencion = await atencionModel.obtenerAtencionAbierta(
        idMesa,
        idMozo
    );

    //Verificar que exista una atencion abierta del mozo
    if (!atencion) {
        const error = new Error(
            "No existe una atención abierta para esta mesa y mozo"
        );
        error.status = 404;
        throw error;
    }
    return atencion;
}

module.exports = {
    iniciarAtencion,
    obtenerAtencionAbierta
};