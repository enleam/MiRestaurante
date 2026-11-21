
const express = require("express");
const pedidoController = require("../controllers/pedido.controller");
const { verificarToken } = require("../middlewares/auth.middleware");
const { permitirRoles } = require("../middlewares/role.middleware");

const router = express.Router();

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

router.put(
    "/:pedidoId",
    verificarToken,
    permitirRoles("MOZO"),
    pedidoController.modificarPedido
);

module.exports = router;
