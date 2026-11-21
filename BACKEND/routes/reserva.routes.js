
const express = require("express");
const router = express.Router();

const reservaController = require("../controllers/reserva.controller");

const { verificarToken } = require("../middlewares/auth.middleware");
const { permitirRoles } = require("../middlewares/role.middleware");

// HU06: Consultar todas las reservas registradas
// Acceso exclusivo para el rol MOZO
router.get(
    "/",
    verificarToken,
    permitirRoles("MOZO"),
    reservaController.listarReservas
);

module.exports = router;
