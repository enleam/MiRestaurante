
const pedidoService = require("../services/pedido.service");

// HU07: Registrar un pedido
async function registrarPedido(req, res) {
    try {
        // Obtener los datos enviados desde Android
        const { atencionId, productos } = req.body;

        // Obtener el mozo desde el token JWT
        const mozoId = req.usuario?.usuarioId;

        // Verificar que el usuario este autenticado
        if (!mozoId) {
            return res.status(401).json({
                mensaje: "Usuario no autenticado"
            });
        }

        // Registrar el pedido mediante el servicio
        const pedido = await pedidoService.registrarPedido(
            atencionId,
            mozoId,
            productos
        );

        // Responder cuando el pedido se registre correctamente
        return res.status(201).json({
            mensaje: "Pedido registrado correctamente",
            pedido
        });

    } catch (error) {
        if (error.status) {
            return res.status(error.status).json({
                mensaje: error.message
            });
        }

        console.error("Error al registrar pedido:", error);

        return res.status(500).json({
            mensaje: "Error interno al registrar el pedido"
        });
    }
}

async function modificarPedido(req, res) {
    try {
        const { pedidoId } = req.params;
        const { productos } = req.body;
        const mozoId = req.usuario?.usuarioId;

        if (!mozoId) {
            return res.status(401).json({
                mensaje: "Usuario no autenticado"
            });
        }

        const pedido = await pedidoService.modificarPedido(
            pedidoId,
            mozoId,
            productos
        );

        return res.status(200).json({
            mensaje: "Pedido modificado correctamente",
            pedido
        });

    } catch (error) {

        if (error.status) {
            return res.status(error.status).json({
                mensaje: error.message
            });
        }


        console.error("Error al modificar pedido:", error);

        return res.status(500).json({
            mensaje: "Error interno al modificar el pedido"
        });
    }
}


/* HU08: Listar los pedidos de una atencion */
async function listarPedidosPorAtencion(req, res) {
    try {
        // Obtener el ID de la atencion desde la URL
        const { atencionId } = req.params;

        // Obtener el mozo autenticado desde el JWT
        const mozoId = req.usuario?.usuarioId;

        // Verificar que el usuario este autenticado
        if (!mozoId) {
            return res.status(401).json({
                mensaje: "Usuario no autenticado"
            });
        }

        // Consultar los pedidos mediante el servicio
        const pedidos = await pedidoService.listarPedidosPorAtencion(
            atencionId,
            mozoId
        );

        // Responder con los pedidos encontrados
        return res.status(200).json({
            mensaje: "Pedidos consultados correctamente",
            pedidos
        });

    } catch (error) {

        // Manejar errores de validacion y acceso
        if (error.status) {
            return res.status(error.status).json({
                mensaje: error.message
            });
        }

        // Manejar errores inesperados
        console.error("Error al consultar pedidos:", error);

        return res.status(500).json({
            mensaje: "Error interno al consultar los pedidos"
        });
    }
}

/*HU09: Enviar pedido a cocina*/
async function enviarPedidoACocina(req, res) {
    try{
        //Obtener el ID del pedido desde la URL 
        const{pedidoId} = req.params;

        //Obtener el mozo autenticado desde el JWT
        const mozoId = req.usuario?.usuarioId;

        //Verificar que el usuario este autenticado
        if(!mozoId){
            return res.status(401).json({
                mensaje: "Usuario no autenticado"
            });
        }
    //Enviar el pedido a cocina mediante el servicio
    const pedido = await pedidoService.enviarPedidoACocina(
        pedidoId,
        mozoId
    );
    //Responder cuando el pedido se envie correctamente
    return res.status(200).json({
        mensaje: "Pedido enviado a cocina correctamente",
        pedido
    });

} catch (error) {
    if (error.status){
        return res.status(error.status).json({
            mensaje: error.message
        });
    }

    console.error("Error al enviar pedido a cocina:", error);

    return res.status(500).json({
        mensaje: "Error interno al enviar el pedido a cocina"
    });
 }
}

/*HU10: Consultar pedidos desde cocina*/
async function listarPedidosCocina(req, res) {
    try{

        //Obtener el usuario de cocina desde el JWT
        const cocinaId = req.usuario?.usuarioId;
        if(!cocinaId){
            return res.status(401).json({
                mensaje: "Usuario no autenticado"
            });
        }
        //Consultar pedidos enviados a cocina
        const pedidos = await pedidoService.listarPedidosCocina(
            cocinaId
        );
        return res.status(200).json({
            mensaje: "Pedidos de cocina consultados correctamente",
            pedidos
        });
    }catch (error){
        if(error.status){
            return res.status(error.status).json({
                mensaje: error.message
            });
        }

        console.error("Error al consultar pedidos de cocina;", error);
        return res.status(500).json({
            mensaje: "Error interno al consultar los pedidos de cocina"
        });
    }
    
}

/*HU09: Actualizar estado del pedido desde cocina*/
async function actualizarEstadoPedidoCocina(req, res) {
    try{
        //Obtener el ID del pedido desde la URL
        const {pedidoId} = req.params;

        //Obtener el nuevo estado enviado por Android
        const {nuevoEstado} = req.body || {};

        //Obtener el usuario de cocina desde el JWT
        const cocinaId = req.usuario?.usuarioId;

        if(!cocinaId){
            return res.status(401).json({
                mensaje: "Usuario no autenticado"
            });
        }
        //Actualizar el estado mediante el servicio

        const pedido = await pedidoService.actualizarEstadoPedidoCocina(
            pedidoId,
            cocinaId,
            nuevoEstado
        );

        return res.status(200).json({
            mensaje: "Estado del pedido actualizado correctamente",
            pedido
        })
    } catch (error){
        if (error.status){
            return res.status(error.status).json({
                mensaje: error.message
            });
        }
        console.error("Error al actualizar pedido de cocina", error);

        return res.status(500).json({
            mensaje: "Error interno al actualizar el pedido"
        });
    }
    
}
module.exports = {
    registrarPedido,
    modificarPedido,
    listarPedidosPorAtencion,
    enviarPedidoACocina,
    listarPedidosCocina,
    actualizarEstadoPedidoCocina
};
