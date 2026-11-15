const express = require("express");
const router = express.Router();

const catalogoController = require("../controllers/catalogo.controller");

const { verificarToken } = require("../middlewares/auth.middleware");
const { permitirRoles } = require("../middlewares/role.middleware");

// Obtener las categorias activas
router.get(
    "/categorias",
    verificarToken,
    permitirRoles("MOZO"),
    catalogoController.listarCategorias
);

// Obtener los productos de la carta
router.get(
    "/productos",
    verificarToken,
    permitirRoles("MOZO"),
    catalogoController.listarProductos
);

module.exports = router;
