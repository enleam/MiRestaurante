const atencionService = require("../services/atencion.service");

//Iniciar la atencion de una mesa
async function iniciarAtencion(req, res) {
    try {
        //Obtener el ID de la mesa enviado por Android
        const { mesaId } = req.body;

        //Obtener el ID del mozo autenticado
        const mozoId = req.usuario?.usuarioId;

        //Comprobar que exista un usuario autenticado
        if (!mozoId) {
            return res.status(401).json({
                mensaje: "Usuario no autenticado"
            });
        }

        //Solicitar al servicio que inicie la atencion
        const atencion = await atencionService.iniciarAtencion(
            mesaId,
            mozoId
        );

        //Responder cuando la atencion fue creada
        return res.status(201).json({
            mensaje: "Atención iniciada correctamente",
            atencion
        });

    } catch (error) {
        //Mostrar el error en la terminal del backend
        console.error("Error al iniciar atención:", error);

        //Devolver errores de validacion o conflicto
        if (error.status) {
            return res.status(error.status).json({
                mensaje: error.message
            });
        }

        //Devolver error interno del servidor
        return res.status(500).json({
            mensaje: "Error interno al iniciar la atención"
        });
    }
}

module.exports = {
    iniciarAtencion
};