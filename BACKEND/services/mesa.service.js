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

module.exports ={
    listarMesas,
    obtenerMesaPorId
};