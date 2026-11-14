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

module.exports={
    listarMesas,
    obtenerMesaPorId
};