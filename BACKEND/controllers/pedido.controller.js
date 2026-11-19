const pedidoService = require("../services/pedido.service");

//Registrar un pedido
async function registrarPedido(req, res) {
    try {
        //Obtener los datos enviados desde Android
        const { atencionId, productos } = req.body;

        //Obtener el mozo desde el token JWT
        const mozoId = req.usuario?.usuarioId;

        //Verificar que el usuario este autenticado
        if (!mozoId) {
            return res.status(401).json({
                mensaje: "Usuario no autenticado"
            });
        }

        //Registrar el pedido mediante el servicio
        const pedido = await pedidoService.registrarPedido(
            atencionId,
            mozoId,
            productos
        );

        //Responder cuando el pedido se registre correctamente
        return res.status(201).json({
            mensaje: "Pedido registrado correctamente",
            pedido
        });

    } catch (error) {
        //Manejar errores de validacion
        if (error.status) {
            return res.status(error.status).json({
                mensaje: error.message
            });
        }

        //Manejar errores inesperados
        console.error("Error al registrar pedido:", error);

        return res.status(500).json({
            mensaje: "Error interno al registrar el pedido"
        });
    }
}

module.exports = {
    registrarPedido
};