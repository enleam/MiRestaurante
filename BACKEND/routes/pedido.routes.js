
const express = require("express");
const pedidoController = require("../controllers/pedido.controller");
const { verificarToken } = require("../middlewares/auth.middleware");
const { permitirRoles } = require("../middlewares/role.middleware");

const router = express.Router();

//HU10: Consultar pedidos desde cocina
router.get(
    "/cocina",
    verificarToken,
    permitirRoles("COCINA"),
    pedidoController.listarPedidosCocina
);

router.post(
    "/",
    verificarToken,
    permitirRoles("MOZO"),
    pedidoController.registrarPedido
);

router.get(
    "/atencion/:atencionId",
    verificarToken,
    permitirRoles("MOZO"),
    pedidoController.listarPedidosPorAtencion
);

//HU09: Enviar pedido a cocinar
router.patch(
    "/:pedidoId/enviar",
    verificarToken,
    permitirRoles("MOZO"),
    pedidoController.enviarPedidoACocina
);

router.patch(
    "/:pedidoId/estado",
    verificarToken,
    permitirRoles("COCINA"),
    pedidoController.actualizarEstadoPedidoCocina
);

router.put(
    "/:pedidoId",
    verificarToken,
    permitirRoles("MOZO"),
    pedidoController.modificarPedido
);

module.exports = router;
