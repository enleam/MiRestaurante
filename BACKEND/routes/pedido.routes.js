const express = require("express");
const pedidoController = require("../controllers/pedido.controller");
const { verificarToken } = require("../middlewares/auth.middleware");
const { permitirRoles } = require("../middlewares/role.middleware");

const router = express.Router();

//HU07: Registrar pedidos (solo MOZO)
router.post(
    "/",
    verificarToken,
    permitirRoles("MOZO"),
    pedidoController.registrarPedido
);

module.exports = router;