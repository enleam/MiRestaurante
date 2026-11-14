const mesaService = require("../services/mesa.service");

//Obtener mesas activas
async function listarMesas(req,res) {
    try{
        const mesas = await mesaService.listarMesas();

        return res.status(200).json({
            mensaje: "Mesas obtenidas correctamente",
            mesas
        });
    } catch(error){
        console.error("Error al listar mesas:",error);
        
        return res.status(500).json({
            mensaje: "Error al obtener las mesas"
        });
    }
}

//Obtener una mesa por su ID
async function obtenerMesaPorId(req,res) {
    try{
        const {mesaId} = req.params;
        const mesa = await mesaService.obtenerMesaPorId(mesaId);
        
        return res.status(200).json({
            mensaje: "Mesa obtenida correctamente",
            mesa
        });
    } catch(error){
        console.error("Error la obtener mesa:",error);

        return res.status(error.status || 500).json({
            mensaje: error.status
                ? error.message
                : "Error interno al obtener la mesa"
        });
    }
}

//HU04: iniciar la atencion de una mesa libre
async function iniciarAtencion(req,res) {
    try{
        const {mesaId} = req.params;

        //El mozo que inicia la atencion es el usuario del token
        const mozoId = req.usuario.usuarioId;

        const {atencion, mesa} = await mesaService.iniciarAtencion(mesaId, mozoId);

        return res.status(201).json({
            mensaje: "Atención iniciada correctamente",
            atencion,
            mesa
        });
    } catch(error){
        console.error("Error al iniciar la atencion:",error);

        return res.status(error.status || 500).json({
            mensaje: error.status
                ? error.message
                : "Error interno al iniciar la atención"
        });
    }
}

module.exports={
    listarMesas,
    obtenerMesaPorId,
    iniciarAtencion
};