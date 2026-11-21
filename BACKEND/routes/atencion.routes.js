const express = require("express");
const router = express.Router();

const atencionController = require("../controllers/atencion.controller");

const { verificarToken } = require("../middlewares/auth.middleware");
const { permitirRoles } = require("../middlewares/role.middleware");

//HU04: Iniciar la atencion de una mesa
//Solo pueden acceder usuarios con rol MOZO
router.post(
    "/",
    verificarToken,
    permitirRoles("MOZO"),
    atencionController.iniciarAtencion
);

//HU07: Consultar la atencion abierta de una mesa
//Solo pueden acceder usuarios con rol MOZO
router.get(
    "/mesa/:mesaId/abierta",
    verificarToken,
    permitirRoles("MOZO"),
    atencionController.obtenerAtencionAbierta
);

module.exports = router;