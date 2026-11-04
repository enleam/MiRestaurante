const express = require("express");
const { login } = require("../controllers/auth.controller");
const { verificarToken } = require("../middlewares/auth.middleware");
const { permitirRoles } = require("../middlewares/role.middleware");

const router = express.Router();

//Ruta para iniciar sesion
router.post("/login", login);

//Ruta temporal para probar el rol MOZO
router.get(
    "/prueba-mozo",
    verificarToken,
    permitirRoles("MOZO"),
    (req, res) => {
        res.status(200).json({
            mensaje: "Acceso permitido para MOZO"
        });
    }
);

//Ruta temporal para probar el rol COCINA
router.get(
    "/prueba-cocina",
    verificarToken,
    permitirRoles("COCINA"),
    (req, res) => {
        res.status(200).json({
            mensaje: "Acceso permitido para COCINA"
        });
    }
);

module.exports = router;