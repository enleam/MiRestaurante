const mesaModel = require("../models/mesa.model");

//Listar mesas activas
async function listarMesas() {
    return await mesaModel.listarMesas();
}

//Obtener mesa por su ID
async function obtenerMesaPorId(mesaId) {
    const id = Number(mesaId);
    
    //Validar que el ID sea correcto
    if(!Number.isInteger(id) || id <= 0){
        const error = new Error("El ID de la mesa no es válido");
        error.status = 400;
        throw error;
    }

    //Buscar la mesa en la base de datos
    const mesa = await mesaModel.obtenerMesaPorId(id);

    //Verificar si existe
    if(!mesa){
        const error = new Error("Mesa no encontrada");
        error.status = 404;
        throw error;
    }
    return mesa;
}

//HU04: iniciar la atencion de una mesa libre
async function iniciarAtencion(mesaId, mozoId) {
    const id = Number(mesaId);

    //Validar que el ID sea correcto
    if(!Number.isInteger(id) || id <= 0){
        const error = new Error("El ID de la mesa no es válido");
        error.status = 400;
        throw error;
    }

    //El mozo se obtiene del token, no de los datos enviados por la app
    if(!Number.isInteger(mozoId) || mozoId <= 0){
        const error = new Error("Usuario no autenticado");
        error.status = 401;
        throw error;
    }

    let respuesta;

    try {
        respuesta = await mesaModel.iniciarAtencion(id, mozoId);
    } catch (errorBD) {
        //2601 y 2627: SQL Server rechazo un duplicado.
        //El indice UX_Atencion_MesaAbierta impide dos atenciones abiertas en una mesa
        if (errorBD.number === 2601 || errorBD.number === 2627) {
            const error = new Error("La mesa ya tiene una atención abierta");
            error.status = 409;
            throw error;
        }
        throw errorBD;
    }

    if (respuesta.resultado === "NO_ENCONTRADA") {
        const error = new Error("Mesa no encontrada");
        error.status = 404;
        throw error;
    }

    if (respuesta.resultado === "NO_DISPONIBLE") {
        const error = new Error("La mesa no está disponible");
        error.status = 409;
        throw error;
    }

    return {
        atencion: respuesta.atencion,
        mesa: respuesta.mesa
    };
}

module.exports ={
    listarMesas,
    obtenerMesaPorId,
    iniciarAtencion
};