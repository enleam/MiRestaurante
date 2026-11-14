const express = require("express");
const router = express.Router();

const mesaController = require("../controllers/mesa.controller");
const {verificarToken} = require("../middlewares/auth.middleware");
const {permitirRoles} = require("../middlewares/role.middleware");

//Listar todas las mesas activas
router.get("/", verificarToken, permitirRoles("MOZO"), mesaController.listarMesas);
//Obtener mesa por su ID
router.get("/:mesaId",verificarToken,permitirRoles("MOZO"),mesaController.obtenerMesaPorId);

module.exports = router;